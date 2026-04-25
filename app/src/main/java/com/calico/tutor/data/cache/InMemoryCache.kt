package com.calico.tutor.data.cache

/**
 * Nivel 1 del sistema de caché de 2 niveles: Caché LRU en memoria.
 *
 * Implementación basada en LinkedHashMap con las siguientes decisiones de diseño:
 *
 * ESTRUCTURA:
 * LinkedHashMap mantiene el orden de inserción (o acceso) de sus entradas.
 * Con accessOrder=true, cada get() mueve el elemento al final de la lista,
 * dejando el menos recientemente accedido (LRU) al inicio para ser eliminado.
 *
 * PARÁMETROS DEL LINKEDHASHMAP:
 * - initialCapacity = maxSize + 1:
 *     Reserva capacidad para exactamente maxSize + 1 entradas.
 *     Evita un rehashing inmediato cuando se inserta el elemento maxSize-ésimo.
 * - loadFactor = 0.75f:
 *     Valor estándar de Java (umbral del 75% de ocupación antes de rehash).
 *     Balance óptimo entre uso de memoria y rendimiento de hashing.
 * - accessOrder = true:
 *     Activa el modo LRU. false = insertion-order (FIFO), true = access-order (LRU).
 *     Con true, get() y put() actualizan la posición del elemento en la lista.
 *
 * THREAD-SAFETY:
 * LinkedHashMap no es thread-safe por defecto.
 * Todos los métodos están @Synchronized para garantizar acceso seguro desde
 * múltiples corrutinas (Dispatchers.IO y Dispatchers.Main) sin condiciones de carrera.
 */
class InMemoryCache(private val maxSize: Int = 20) {

    /** Entrada del caché con el valor almacenado y su timestamp de inserción. */
    data class CacheEntry(
        val value: Any,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val map = object : LinkedHashMap<String, CacheEntry>(
        maxSize + 1,  // initialCapacity: +1 para evitar rehashing al llegar al límite
        0.75f,        // loadFactor: balance óptimo memoria/rendimiento
        true          // accessOrder: true = LRU (acceso más reciente al final)
    ) {
        /**
         * Se invoca automáticamente por LinkedHashMap después de cada put().
         * Devuelve true cuando el mapa supera maxSize, lo que elimina
         * la entrada más antigua (menos recientemente accedida, al inicio del mapa).
         */
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > maxSize
    }

    /** Inserta o actualiza una entrada. Mueve el elemento al final (MRU). */
    @Synchronized
    fun put(key: String, value: Any) {
        map[key] = CacheEntry(value)
    }

    /**
     * Obtiene una entrada por clave, o null si no existe.
     * Con accessOrder=true, esta llamada mueve el elemento al final (actualiza LRU order).
     */
    @Synchronized
    fun get(key: String): CacheEntry? = map[key]

    /** Elimina todas las entradas del caché. */
    @Synchronized
    fun clear() = map.clear()

    /** Número actual de entradas en el caché. */
    @Synchronized
    fun size(): Int = map.size
}
