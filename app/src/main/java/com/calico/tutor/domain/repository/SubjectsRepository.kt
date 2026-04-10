package com.calico.tutor.domain.repository

import com.calico.tutor.data.dto.response.SubjectsHistoryResponse
import com.calico.tutor.domain.utils.Result

/**
 * Repository Pattern para Recommended Subjects Feature
 * 
 * Abstracción que define el contrato para obtener materias recomendadas
 * SIN conocer detalles de cómo se obtienen, filtran u ordenan
 */
interface SubjectsRepository {
    
    /**
     * Obtiene historial de materias (para calcular recomendadas)
     * 
     * @return Resultado con historial de materias del tutor
     * 
     * Nota: La implementación puede:
     * - Obtener del backend
     * - Filtrar por frecuencia
     * - Ordenar cronológicamente
     * - Cachear resultados
     * Todo esto está OCULTO para el ViewModel
     */
    suspend fun getSubjectsHistory(): Result<SubjectsHistoryResponse>
}
