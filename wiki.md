# Sprint 4 Wiki – Project Documentation

## General Information

| | |
|---|---|
| **Sprint** | Sprint 4 – App Implementation and Definition |
| **Daniel Camilo Quimbay Velasquez** | d.quimbay@uniandes.edu.co |
| **Nikol Katherine Rodriguez Ortiz** | nk.rodriguez@uniandes.edu.co |
| **Paola Catherine Jimenez Jaque** | p.jimenezj@uniandes.edu.co |
| **Maria Lucia Benavides Domínguez** | m.benavidesd@uniandes.edu.co |

| Repository | Link |
|---|---|
| Flutter App | [calico-mobile-flutter](https://github.com/Team23-ISIS3510/calico-mobile-flutter) |
| Kotlin App | [calico-mobile-kotlin](https://github.com/Team23-ISIS3510/calico-mobile-kotlin) |
| Backend | [backend](https://github.com/Team23-ISIS3510/backend) |
| Analytics Pipelines | [backend/analytics](https://github.com/Team23-ISIS3510/backend/tree/dev/src/modules/analytics) |

---
## 1. Value Proposition

### 1.2 Problem We Solve

The university peer-tutoring ecosystem relies on informal, decentralized channels like WhatsApp, creating a **"coordination and discovery gap"**. Students face a 48-hour confirmation void, risk wasting money on low-quality tutors, and have no way to find available help quickly. Tutors have no professional tools to manage their availability or track their performance.

---

### 1.3 Our Solution

Calico is a **centralized peer-tutoring marketplace** that replaces informal negotiation with a professionalized, data-driven ecosystem. Students move instantly from "academic crisis" to a confirmed session. Tutors get a professional profile and availability management system.

---

### 1.4 Value Delivered by Feature

| Feature | Value to Student | Evidence from Data |
|---------|-----------------|-------------------|
| **Top Rated & Available Soon** (BQ3) | Find a high-quality tutor available right now, no searching, no waiting | Tutors filtered by rating > 4.5 and availability in the next 4 hours, served in real time |
| **Instant Slot Booking** (BQ5) | Book a confirmed session in one tap, no 48-hour confirmation void | 7.86% of all bookings confirmed instantly; rate grows as more tutors adopt the platform |
| **Your Go-To Tutor** | Personalized recommendation based on your own booking history, no decision fatigue | Built from real completed session data per student per course |
| **Recommended Courses** (BQ10) | See which courses you've needed help with most, platform surfaces what you already know you need | Derived from session frequency per courseId in tutoringSessions collection |
| **Location-Aware UI** | On campus → highlights in-person tutors nearby. Off campus → highlights virtual tutors | GPS coordinates compared against campus boundary in real time |
| **Offline Support** | Book sessions, edit profile, and browse tutors even without internet, nothing is lost | SQLite queue syncs pending bookings automatically on reconnect |
| **Context-Aware Greeting** | App adapts to your time of day and session schedule — feels personal, not generic | 6 distinct messages derived from device clock + session data |

---

### 1.5 Business Questions as Value Drivers

| BQ | Question | Value it creates |
|----|----------|-----------------|
| **BQ3** | Which tutors with rating > 4.5 are available in the next 4 hours? | Reduces student search time from hours to seconds |
| **BQ5** | What % of bookings succeed instantly without manual confirmation? | Measures and improves the core booking engine reliability |
| **BQ10** | What % of sessions originate from the carousel vs search? | Tells us whether smart recommendations are driving conversions |
| BQ16 | What is the weekly percentage of tutors who use the history view feature? | Helps measure adoption and engagement of the history analytics feature among tutors |

---

### 1.6 Revenue Model

| Stream | Description |
|--------|-------------|
|  |  |
| |  |
|  |  |

---

### 1.7 Data as a Competitive Advantage

The data collected by the platform compounds over time:

- **More sessions** → better Go-To Tutor recommendations → higher repeat booking rate
- **More bookings** → more accurate BQ5 success rate → better booking engine optimization
- **More students** → richer course demand data → better tutor supply matching

This creates a **network effect**: the more students use Calico, the more valuable it becomes for both students and tutors.

---

### 1.8 Why Calico Wins Over Alternatives

| Alternative | Problem | How Calico solves it |
|-------------|---------|---------------------|
| WhatsApp groups | No quality signal, 48h confirmation void | Verified ratings + instant booking |
| Generic tutoring sites | Not university-specific, no peer context | Built for Uniandes — courses, schedules, and campus awareness |
| No platform | Students fail or overpay for poor help | Affordable peer tutors with verified academic credentials |

---

# 2. Micro-Optimization Strategies

# 2.1 Flutter

## 2.1.1 Optimization Overview

| Optimization ID | Feature | Objective | Status | Owner
|---|---|---|---|---|
|  |  |  |  |  |
|  |  |  |  |  |

---

## 2.1.2 Optimization Details

## Micro-optimization Strategies

> Each optimization was evaluated using Flutter DevTools Performance Profiler before and after implementation. The profiler measured frame rendering time, widget rebuild count, and CPU usage during typical user interactions.

---

## 2.1 Optimization Overview

| Optimization ID | Feature | Objective | Status | Owner |
|---|---|---|---|---|
| 1 | Home — course and session listing | Render only visible items (lazy lists) instead of building the entire list when opening the screen | Planned | Maria Lucia Benavides Domínguez |
| 2 | Home — "Recommended for you" section | Avoid recalculating and reassigning the recommended courses list on every widget rebuild | Planned | Maria Lucia Benavides Domínguez |
| 3 | Home — state and search | Limit rebuilds to the subtree that actually changes when the `HomeController` notifies updates (e.g. when filtering courses) | Planned | Maria Lucia Benavides Domínguez |

**Planned validation tools:** Flutter DevTools (Performance, CPU Profiler, rebuild tracking in debug mode) and Android Studio Profiler (memory and CPU in `--profile` mode), using repeatable scenarios: opening Home, prolonged scrolling, and fast typing in the course search bar.

**Guía paso a paso (escenarios, capturas, métricas):** [`wiki_flutter_profiling_guide.md`](wiki_flutter_profiling_guide.md)

---

## 2.2 Optimization Details

### Optimization 1 — Lazy Lists in Home

#### Context

The main screen (`HomeScreen`) contains courses, pending sessions, confirmed sessions, and a horizontal recommended carousel. It is the entry point after login and the most frequently scrolled view. In the current design, the scrollable body is built as a `ListView` whose children are expanded using the spread operator over collections (`..._controller.courses.map(...)`, and similarly for sessions). That pattern is acceptable for very small lists, but it scales poorly: Flutter must instantiate one widget per item even if it is not inside the viewport.

#### Performance Problem

When loading Home, the framework immediately builds all `CourseCard` and `SessionCard` widgets from the complete lists. This implies more work on the UI thread during the first frame after loading, higher memory pressure (more elements in the widget tree), and a greater chance of jank while scrolling, since the initial cost already consumed part of the frame budget. The problem becomes worse if the backend or demo seed returns many courses or sessions. In terms of micro-optimization concepts, this is equivalent to **creating unnecessary objects in a frequently executed path** (similar to allocating objects inside a loop or a drawing method executed on every frame).

#### Optimization Strategy

**Proposed design:** replace eager materialization with on-demand building:

- Use `ListView.builder`, `ListView.separated`, or a `CustomScrollView` with `SliverList` / `SliverChildBuilderDelegate` for the course, pending session, and confirmed session sections.
- Keep section headers (`SectionHeader`, offline banners, etc.) as slivers or fixed widgets outside builders whenever they do not depend on the list index.
- Preserve the horizontal recommended carousel with `ListView.separated` (which is already lazy per item); only standardize the pattern across the rest of the vertical scroll.

**Expected behavior:** when opening Home, only visible items plus a small scroll buffer are built; while scrolling, widgets are created and disposed in a bounded way. Business logic (`HomeController`, repositories, navigation to `CourseDetailScreen`) remains unchanged; only the list presentation layer is modified.

**Success criteria (to be measured through profiling):** lower memory peak when entering Home, reduced `build` time on the first frame with large lists, and a lower percentage of frames exceeding the 16 ms budget during fast scrolling.

#### Profiling Before Optimization

| Metric | Value |
|---|---|
| CPU Usage |  |
| Memory Usage |  |
| Render Time |  |
| Other |  |

##### Evidence
- Screenshot:
- Video:
- Profiling Tool Used:

---

#### Code Snippet Before

```dart
// CODE HERE
```

---

#### Code Snippet After

```dart
// CODE HERE
```

---

#### Profiling After Optimization

| Metric       | Before | After | Improvement |
| ------------ | ------ | ----- | ----------- |
| CPU Usage    |        |       |             |
| Memory Usage |        |       |             |
| Render Time  |        |       |             |
| Other        |        |       |             |



### Optimization 2 — Caché de cursos recomendados

#### Context

La sección **"Recommended for you"** deriva del historial de sesiones del estudiante: se cuentan sesiones por `courseId`, se ordenan y se toman los tres cursos más frecuentes. El controlador ya usa `ArrayMap` para los conteos por curso (adecuado para colecciones pequeñas y lecturas frecuentes, alineado con la micro-optimización de **estructuras de datos ligeras**). Sin embargo, el getter que expone `recommendedCourses` sigue ejecutando en cada acceso operaciones adicionales: construcción de un `Map` auxiliar, copia de entradas a lista, ordenamiento y mapeo a entidades.

En `HomeScreen`, ese getter se invoca desde `build` cada vez que la pantalla se reconstruye.

#### Performance Problem

Cualquier evento que llame a `notifyListeners()` en `HomeController` —carga de datos, fin de búsqueda con isolate, sincronización offline, cambio de pestaña, etc.— provoca un nuevo `build` de Home y, con él, **una recomputación completa de recomendados aunque las sesiones y los cursos no hayan cambiado**. Se crean listas y mapas temporarios en una ruta caliente (el método `build` del árbol de widgets), lo que incrementa trabajo de CPU y presión sobre el recolector de basura sin beneficio para el usuario.

#### Optimization Strategy

**Diseño propuesto:** separar el **cálculo** de la **exposición** de recomendados:

- Introducir un campo privado en `HomeController`, por ejemplo `_recommendedCourses`, materializado como `List<CourseEntity>` (idealmente inmutable o copia defensiva al exponer).
- Recalcular esa lista solo en puntos de invalidación explícitos: tras `loadSessions`, `loadCourses`, o cuando cambie la composición de `_sessions` / `_allCourses` de forma que afecte el ranking. Al invalidar `_sessionCountCache`, recalcular también `_recommendedCourses` en el mismo flujo.
- El getter público `recommendedCourses` devolverá la lista cacheada sin ordenar ni mapear en cada lectura.
- Mantener `ArrayMap` para los conteos; el sort y el `take(3)` ocurren una vez por invalidación, no una vez por frame.

**Comportamiento esperado:** la UI sigue mostrando los mismos tres cursos recomendados, pero el costo de ordenamiento y asignaciones desaparece de la ruta de rebuild frecuente (p. ej. al escribir en el buscador).

**Criterio de éxito (a medir en profiling):** menos muestras de CPU en `recommendedCourses`, `List.sort` y creación de `Map` durante búsqueda o rebuilds repetidos; frames más estables al teclear en el campo de búsqueda.

#### Profiling Before Optimization

| Metric | Value |
|---|---|
| CPU Usage |  |
| Memory Usage |  |
| Render Time |  |
| Other |  |

##### Evidence
- Screenshot:
- Video:
- Profiling Tool Used:

---

#### Code Snippet Before

```dart
// CODE HERE
```

---

#### Code Snippet After

```dart
// CODE HERE
```

---

#### Profiling After Optimization

| Metric       | Before | After | Improvement |
| ------------ | ------ | ----- | ----------- |
| CPU Usage    |        |       |             |
| Memory Usage |        |       |             |
| Render Time  |        |       |             |
| Other        |        |       |             |


### Micro-optimization  3 — Rebuilds acotados en Home

#### Context

`HomeScreen` es un `StatefulWidget` que registra un listener global sobre `HomeController`. Ante cada `notifyListeners()`, el callback `_onUpdate` ejecuta `setState(() {})` sobre **toda** la pantalla. El header (logo, badge de campus), la barra de búsqueda, el banner contextual y el cuerpo scrollable comparten el mismo ciclo de reconstrucción.

La búsqueda de cursos ya delega el filtrado pesado a un isolate (`filterCoursesInIsolate`), pero al resolver el `Future` el controlador actualiza `_filteredCourses` y notifica de nuevo, disparando un rebuild completo.

#### Performance Problem

Partes de la UI que no dependen del resultado de la búsqueda —cabecera, `TextField`, banner de contexto, indicadores de offline que no cambiaron— se reconstruyen igual que la lista de cursos. En Flutter, cada `setState` en el estado raíz de la pantalla invalida un subárbol grande. Esto es análogo a **repintar o rebindar toda la jerarquía** cuando solo cambió un subconjunto de datos (principio de no repetir trabajo caro en toda la UI, comparable a evitar `findViewById` / redibujado completo en Android).

El impacto es más visible durante la **escritura rápida en el buscador**: muchas notificaciones seguidas multiplican trabajo de layout y `build` en widgets estáticos.

#### Optimization Strategy

**Diseño propuesto:** acotar el alcance del `Listenable` sin cambiar la arquitectura `ChangeNotifier` + repositorios:

- Extraer el bloque scrollable (recomendados + listas de cursos/sesiones) a un widget hijo que escuche al `HomeController` mediante `ListenableBuilder` (o `AnimatedBuilder`) **solo alrededor de esa sección**.
- Dejar en el `State` de `HomeScreen` únicamente el estado local que no vive en el controlador: pestaña seleccionada, flags de offline/sync, resultado de campus, etc. Esos campos seguirán usando `setState` local sin forzar rebuild de la lista.
- Opcionalmente, dividir en sub-widgets con `const` donde los parámetros no cambien (p. ej. `_HomeHeader`, `_SearchBar`) para que Flutter pueda saltar rebuilds si el padre se reconstruye pero las referencias son estables — combinado con 3, el padre del header no debería reconstruirse en cada tecla de búsqueda.

**Comportamiento esperado:** al filtrar cursos, solo se reconstruye el subtree que consume `courses`, `sessions` y `recommendedCourses`; header y buscador permanecen estables salvo que cambien sus props explícitas.

**Criterio de éxito (a medir en profiling):** con "Track widget rebuilds" activo en debug, drástica reducción de rebuilds de `_HomeHeader`, `_SearchBar` y `_ContextAwareBanner` durante la búsqueda; en Performance, menor tiempo total de `build` por frame al escribir.

**Nota de implementación:** 3 es complementaria a 1 y 2; conviene perfilar cada optimización por separado (una rama o experimento por ID) para atribuir mejoras en la wiki.

#### Profiling Before Optimization

| Metric | Value |
|---|---|
| CPU Usage |  |
| Memory Usage |  |
| Render Time |  |
| Other |  |

##### Evidence
- Screenshot:
- Video:
- Profiling Tool Used:

---

#### Code Snippet Before

```dart
// CODE HERE
```

---

#### Code Snippet After

```dart
// CODE HERE
```

---

#### Profiling After Optimization

| Metric       | Before | After | Improvement |
| ------------ | ------ | ----- | ----------- |
| CPU Usage    |        |       |             |
| Memory Usage |        |       |             |
| Render Time  |        |       |             |
| Other        |        |       |             |

---

### Micro-optimization 4 — const Constructors on Static Widgets
> Implemented by Paola Catherine Jimenez Jaque

#### Problem Identified
Flutter's profiler showed that static widgets with no dynamic data — such as `_HomeHeader`, section titles, empty state views, and icon widgets — were being **reinstantiated on every rebuild** even though their content never changes. Without `const`, Flutter cannot short-circuit the widget instantiation and must allocate new objects on every frame.

**Before profiling result:**
- Static widgets rebuilt on every `notifyListeners()` call
- Object allocation visible in DevTools Memory tab on each rebuild
- Widgets like `AppLogo`, `SectionHeader`, `EmptyStateView` created fresh each frame

#### Optimization Implemented
Added `const` keyword to all widget constructors and their call sites where the widget has no dynamic parameters.

```dart
// Before — new object created on every rebuild
_HomeHeader()
SectionHeader('Courses')
EmptyStateView('No courses found')
Icon(Icons.search, color: AppColors.brown, size: 24)

// After — Flutter reuses the same object, zero allocation
const _HomeHeader()
const SectionHeader('Courses')
const EmptyStateView('No courses found')
const Icon(Icons.search, color: AppColors.brown, size: 24)
```

#### Justification
When a widget is marked `const`, Dart canonicalizes it at compile time — the same instance is reused across rebuilds. Flutter's element reconciliation detects that the widget reference is identical and skips the build entirely. This is the lowest-effort, highest-impact optimization available in Flutter — zero runtime cost, zero logic change.


---

### Micro-optimization 5 — Debounced Search Input
> Implemented by Paola Catherine Jimenez Jaque

#### Problem Identified
The tutor search screen triggered a new API call and cache lookup on **every keystroke** in the search text field. Flutter's profiler showed that typing "Cálculo" (7 characters) fired 7 consecutive search calls — 6 of which were immediately discarded when the next character arrived. This caused redundant network requests, unnecessary cache writes, and visible loading flicker on each keystroke.

**Before profiling result:**
- 1 API call fired per keystroke
- Typing a 7-character query = 7 network requests
- Loading spinner flickered on every character typed
- Cache filled with partial query results that were never used again

#### Optimization Implemented
Added a `Timer`-based debounce in `TutorSearchController` that delays the search execution by 400ms after the last keystroke. If a new character arrives before the timer fires, the previous timer is cancelled and reset.

```dart
// Before — fires search on every keystroke
TextField(
  onChanged: (query) => _controller.search(query),
)

// After — debounced, fires only after 400ms of inactivity
Timer? _debounceTimer;

void _onSearchChanged(String query) {
  _debounceTimer?.cancel();
  _debounceTimer = Timer(const Duration(milliseconds: 400), () {
    _controller.search(query);
  });
}

@override
void dispose() {
  _debounceTimer?.cancel(); // prevent timer fire after widget disposal
  super.dispose();
}
```

#### Justification
A 400ms debounce window matches typical human typing speed — most users pause naturally between words or after completing a name. This converts O(n) API calls per query (where n = characters typed) into O(1) — exactly one call per completed search intent. The timer is cancelled in `dispose()` to prevent a call firing after the widget is removed from the tree.


---

### Micro-optimization 6 — RepaintBoundary on Heavy Animated Widgets
> Implemented by Paola Catherine Jimenez Jaque

#### Problem Identified
The `_ContextAwareBanner` uses `AnimatedContainer` to transition colors based on time of day. Flutter's profiler showed that every time the banner's animation ticked, it **triggered a repaint of the entire home screen** — including the course list and session cards below it — because they shared the same render layer.

**Before profiling result:**
- Banner animation caused full-screen repaint on every tick
- Repaint regions visible in DevTools (Highlight Repaints enabled)
- Course list and session cards repainted unnecessarily during banner transition

#### Optimization Implemented
Wrapped `_ContextAwareBanner` in a `RepaintBoundary` widget, isolating it into its own render layer so its animation repaints only its own subtree.

```dart
// Before — banner animation repaints entire screen
_ContextAwareBanner(
  title: ContextAwareHelper.getTitle(),
  message: ContextAwareHelper.getMessage(...),
  icon: ContextAwareHelper.getIcon(),
  backgroundColor: ContextAwareHelper.getBackgroundColor(),
  accentColor: ContextAwareHelper.getAccentColor(),
),

// After — banner repaint isolated to its own layer
RepaintBoundary(
  child: _ContextAwareBanner(
    title: ContextAwareHelper.getTitle(),
    message: ContextAwareHelper.getMessage(...),
    icon: ContextAwareHelper.getIcon(),
    backgroundColor: ContextAwareHelper.getBackgroundColor(),
    accentColor: ContextAwareHelper.getAccentColor(),
  ),
),
```

#### Justification
`RepaintBoundary` promotes its child to a separate compositing layer in Skia/Impeller. When the banner animates, only that layer is repainted and re-composited — the course list and session cards below it are untouched. This is especially impactful for animations that run continuously or frequently, since every saved repaint directly reduces GPU work.

---

# 2.2 Kotlin Version

## 2.2.1 Optimization Overview

| Optimization ID | Feature | Objective | Status |
| --------------- | ------- | --------- | ------ |
|                 |         |           |        |
|                 |         |           |        |

---

## 2.2.2 Optimization Details

# Optimization [ID]

## Context

[WRITE HERE]

## Performance Problem

[WRITE HERE]

## Profiling Before Optimization

| Metric       | Value |
| ------------ | ----- |
| CPU Usage    |       |
| Memory Usage |       |
| Render Time  |       |
| Other        |       |

---

## Optimization Strategy

[WRITE HERE]

---

## Code Snippet Before

```kotlin
// CODE HERE
```

---

## Code Snippet After

```kotlin
// CODE HERE
```

---

## Profiling After Optimization

| Metric       | Before | After | Improvement |
| ------------ | ------ | ----- | ----------- |
| CPU Usage    |        |       |             |
| Memory Usage |        |       |             |
| Render Time  |        |       |             |
| Other        |        |       |             |

---

## Final Justification

[WRITE HERE]

---

# 3. New Features

# 3.1 Flutter Version

## 3.1.1 Features Summary

| Feature ID | Feature Name | Team Member | Sprint | Category | Owner
| ---------- | ------------ | ----------- | ------ | -------- |--------|
|            |              |             |        |          |        |
|            |              |             |        |          |        |

---
## New Feature:
### Feature Description

---

### 3.1.2 Multi-threading

---

### 3.1.3 Local Storage

---

### Business Value

---

## New Feature: Tutor Search with Filters
> Implemented by Paola Catherine Jimenez Jaque

### Feature Description

A new dedicated search screen that allows students to search for tutors by course, filter by rating, location (virtual/in-person), and price. The screen is accessible from the home screen and provides a richer discovery experience than the carousel.

---

### 3.1.4 Caching Strategy

#### What it does
When a student searches for tutors for a given course, the result is stored in an LRU Cache keyed by the search parameters. If the same search is performed again within 5 minutes, the cached result is returned instantly without a network call. This avoids redundant API calls for repeated or back-navigated searches.

#### Implementation

| Field | Detail |
|-------|--------|
| **Cache structure** | `LRUCache<String, List<AvailableTutorModel>>` — reuses the existing LRU implementation from `lib/core/cache/lru_cache.dart` |
| **Cache key** | Composite key: `"search_${courseId}_${minRating}_${locationType}"` — unique per search parameter combination |
| **maxSize** | 10 entries — covers the typical number of distinct searches in a session |
| **TTL** | 5 minutes — tutor availability changes frequently; longer TTL would show stale slots |
| **Cache hit** | Result returned instantly with no network call. A subtle `'Showing cached results'` label is hidden from the user unless they tap a refresh button |
| **Cache miss** | API called, result cached, returned to UI |
| **Invalidation** | Cache cleared after a successful booking so the next search reflects the updated availability |

#### Why LRU over alternatives
A simple `HashMap` would grow unbounded. A TTL-only cache without LRU would keep rarely used entries while evicting nothing. LRU ensures the most recently used searches stay in memory while old ones are evicted automatically when `maxSize` is reached.

```dart
// Cache key construction
String _cacheKey(String courseId, double minRating, String locationType) =>
  'search_${courseId}_${minRating}_$locationType';

// In TutorSearchRepositoryImpl
Future<List<AvailableTutorModel>> searchTutors({
  required String courseId,
  double minRating = 4.0,
  String locationType = 'all',
}) async {
  final key = _cacheKey(courseId, minRating, locationType);

  // Check cache first
  final cached = _cache.get(key);
  if (cached != null) return cached;

  // Cache miss — fetch from API
  final results = await _apiClient.get(
    '/analytics/bookable-tutors',
    query: {
      'course': courseId,
      'minRating': minRating.toString(),
    },
  );

  final tutors = (results['tutors'] as List)
    .map((t) => AvailableTutorModel.fromJson(t))
    .where((t) => locationType == 'all' ||
      (locationType == 'virtual' && t.location.toLowerCase() == 'virtual') ||
      (locationType == 'inperson' && t.location.toLowerCase() != 'virtual'))
    .toList();

  _cache.put(key, tutors);
  return tutors;
}
```

---

### 3.1.5 Eventual Connectivity Strategy

#### What it does
When a student opens the tutor search screen without internet, the app serves the last cached search results instead of an error screen. A connectivity banner notifies the user they are offline. When internet returns, the screen automatically refreshes with fresh data.

#### Implementation

| Field | Detail |
|-------|--------|
| **Connectivity detection** | `StreamSubscription<List<ConnectivityResult>>` on `Connectivity().onConnectivityChanged` |
| **Offline behavior** | Serves last cached search result from LRU Cache. Shows orange `'Offline — showing saved results'` banner. Search filters are disabled while offline. |
| **Online behavior** | Normal API call, result cached, banner hidden |
| **On reconnect** | `StreamSubscription` detects reconnect → auto-triggers `searchTutors()` with last used parameters → refreshes results silently |
| **No cache available** | If the student opens the screen offline with no prior search cached, shows `'No internet connection — search unavailable offline'` with a retry button |
| **Anti-patterns avoided** | No polling · No blocking UI · StreamSubscription cancelled in `dispose()` to prevent memory leaks |

```dart
// TutorSearchScreen
late StreamSubscription _connectivitySub;
bool _isOffline = false;
Map<String, dynamic>? _lastSearchParams;

@override
void initState() {
  super.initState();
  _connectivitySub = Connectivity()
    .onConnectivityChanged
    .listen((results) {
      final offline = results.every(
        (r) => r == ConnectivityResult.none);
      setState(() => _isOffline = offline);

      // Auto-refresh on reconnect with last used params
      if (!offline && _lastSearchParams != null) {
        _search(_lastSearchParams!);
      }
    });
}

@override
void dispose() {
  _connectivitySub.cancel(); // prevent setState-after-dispose
  super.dispose();
}

Future<void> _search(Map<String, dynamic> params) async {
  _lastSearchParams = params;
  setState(() => _isLoading = true);

  try {
    final results = await _repository.searchTutors(
      courseId: params['courseId'],
      minRating: params['minRating'],
      locationType: params['locationType'],
    );
    setState(() {
      _results = results;
      _isLoading = false;
    });
  } catch (_) {
    // Offline — LRU cache fallback handled in repository
    setState(() => _isLoading = false);
  }
}
```

#### Behavior by network state

| Online | Offline | On Reconnect |
|--------|---------|-------------|
| Normal API call. Result cached in LRU. Results shown. | Last LRU cache entry served. Orange offline banner shown. Filters disabled. | StreamSubscription fires → auto re-search with last params → fresh results loaded silently. |

---

### Business Value

- Students can discover tutors beyond the 4-hour carousel window
- Repeated searches are served instantly from cache — better UX on slow campus WiFi
- Offline support ensures students on weak connections can still browse previously loaded results
- Filter by location enables the location-aware context feature to extend into explicit search

---

# 3.2 Kotlin Version

## 3.2.1 Features Summary

| Feature ID | Feature Name | Team Member | Sprint | Category |
|:---|:---|:---|:---|:---|
| F001 | Secure Multi-Provider Auth (Firebase Email/Password + Google Login) | Daniel Camilo Quimbay Velazquez | 1 | Authentication |
| F002 | Profile Management (Triple-Layer Cache: LRU, Disk, Network) | Daniel Camilo Quimbay Velazquez | 1 | User Profile |
| F003 | Course Catalog & Application (Offline support with SQLite) | Daniel Camilo Quimbay Velazquez | 3 | Courses |
| F004 | Availability Management (CRUD for time slots) | Daniel Camilo Quimbay Velazquez | 3 | Availability |
| F005 | Shake-to-Report Bug (Accelerometer + email intent) | Daniel Camilo Quimbay Velazquez | 4 | Sensor Feature |
| F006 | Session Overview (Incoming and upcoming sessions) | Nikol Katherin Rodriguez Ortiz | 2 | Home Page |
| F007 | Occupancy Analysis (Workload vs. available slots analysis) | Nikol Katherin Rodriguez Ortiz | 2 | Home Page |
| F008 | Tutor Recommendations (Collaborative filtering based on booking history) | Daniel Camilo Quimbay Velazquez | 3 | Home Page |
| F009 | Stability Dashboard (Telemetry: crash-free sessions, API latency) | Daniel Camilo Quimbay Velazquez | 4 | Analytics |
| F010 | Session History (Last 2 and next 2 tutoring sessions) | Daniel Camilo Quimbay Velazquez | 4 | External Service |
| F011 | Google Authentication (Sign-up and login with Google) | Nikol Katherin Rodriguez Ortiz | 2 | Authentication |
| F012 | Past Tutoring History (Complete history of past tutoring sessions) | Nikol Katherin Rodriguez Ortiz | 4 | Tutor History |

---

## 3.2.2 New feature tutoring History View with Offline Cache

**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **Event description** | User opens the History View screen, which must display their complete list of past tutoring sessions with tutor name, profile picture, course, date, price, and payment status. Optional filters by date range and course can be applied. The event occurs regardless of network availability. |
| **System response** | Offline: L1 memory cache is checked first; if cold, L2 SQLite (cache_history) is queried and stale data is displayed immediately — the session list is never empty. Online: Fresh data is fetched from GET /tutoring-sessions/student/{studentId}/history via Dispatchers.IO. The backend queries Firestore for sessions and enriches each one with tutor info (name, profile picture) from the users collection, then returns { sessions, count, stats, uniqueCourses }. The full response is stored in both L1 (in-memory) and L2 (SQLite with TTL), then rendered on Dispatchers.Main. API failure despite connection: Stale cached data is served as a silent fallback with an orange "Showing saved data" badge, without blocking the UI. On app relaunch: L1 is cold but L2 persists across restarts, guaranteeing history is always visible. |
| **Possible anti-patterns** | Calling the API on the main thread (UI freeze) · Skipping the TTL check and serving indefinitely stale sessions · Not persisting the enriched response (sessions + stats) to L2 after a successful API call · Relying solely on in-memory cache and losing all history on restart · Not switching to Dispatchers.Main before updating UI state · Applying date/course filters on the API call instead of in-memory (unnecessary re-fetches) · Not invalidating the cache when a new session is created or cancelled. |
| **Caching strategy** | Two-level cache. L1: In-memory LinkedHashMap with LRU eviction (max 20 entries, keyed by studentId) — nanosecond access, ideal for filter changes and UI recompositions. L2: SQLite cache_history table with a configurable 5-minute TTL. Read policy: L1 → L2 → API. If TTL expired: refresh from API when online, serve stale when offline. Cache is invalidated on every successful session creation or cancellation. |
| **Storage type** | SQLite for the L2 persistent cache — lightweight, supports TTL-based validation, and survives app restarts. SQLite was preferred over Room here due to its simplicity for a single cached JSON payload per user session. |
| **Stored data type** | json_data TEXT (full enriched API response serialized as a JSON string — includes sessions[], stats { totalSessions, totalSpent, uniqueCourses, paidSessions, pendingSessions }, and uniqueCourses[]) + timestamp INTEGER stored in the cache_history table. Deserialized to HistoryResponse data class in the repository layer inside Dispatchers.IO. |


---

## 3.2.3 Local Storage Strategies

### Local Storage Strategy

**Owner:** Nikol Katherin Rodriguez Ortiz

The History View uses three complementary local-storage strategies. Each was chosen based on data shape, query needs, and offline behavior.

| Technology | Feature / data stored | Why chosen | Access pattern | Persistence / invalidation |
|------------|-----------------------|------------|----------------|-----------------------------|
| MemoryCache (L1) | Full enriched history response keyed by studentId: session list with tutor name and photo, stats, and uniqueCourses | Sub-millisecond reads for filter changes and UI recompositions without hitting disk or network. LRU eviction keeps memory bounded (max 20 entries). | Read on every screen open; populated after every successful L2 or API read. LinkedHashMap keyed by studentId. | Cleared on app restart. Always superseded by L2 — no standalone TTL. |
| SQLite (sqflite) | Offline fallback cache for History View: complete enriched API payload (sessions with tutor name + profile picture, stats, uniqueCourses) | Structured single-row cache keyed by student_id with TTL validation. Survives app restarts and guarantees the session list is always available offline. | upsert() on every successful API response; queryOne(student_id) on L1 miss or cold start. Table: cache_history. | Persists until replaced by a fresh API response (TTL 5 min) or cleared on logout via clearAll(). Invalidated on session creation or cancellation. |
| SharedPreferences | Active filter state: history_filter_course, history_filter_start_date, history_filter_end_date | Small, scalar filter values. Persists the user's last selection across restarts so the screen reopens in the same filtered state without re-prompting. | load() on screen init; save() on filter change; clear() on logout. | Persists across restarts. Cleared on logout for privacy. |

### History cache strategy (clear flow)

- **Remote-first:** `getStudentHistory(studentId)` requests the full list from `GET /tutoring-sessions/student/{studentId}/history` via `Dispatchers.IO`.
- **Enrichment at backend:** the API resolves `tutorName` and `profilePicture` from the Firestore `users` collection per session before returning — the client stores the already-enriched payload, avoiding per-session lookups on the mobile side.
- **SQLite write shape:** stores one row in `cache_history` (`AppDatabaseService.tableHistoryCache`) with `student_id`, `json_data` (full serialized response: `sessions[]`, `stats { totalSessions, totalSpent, uniqueCourses, paidSessions, pendingSessions }`, `uniqueCourses[]`), and `timestamp` (epoch ms for TTL check).
- **L1 populate:** on every successful read from either API or L2, the full response is written to the in-memory `LinkedHashMap` under key `studentId`.
- **Filter application:** date range and course filters are always applied **in-memory** over the cached session list — no re-fetch is triggered by a filter change, keeping the experience instant even offline.
- **Offline fallback:** if the remote call fails, reads `cache_history` by `student_id`. If a row exists, decodes `json_data` and returns `CachedResult(..., isFromCache: true)`, showing an orange "Showing saved data" badge. If no row exists (first offline run), it rethrows and shows an error state.
- **Invalidation:** the `cache_history` row is deleted and the L1 entry is evicted when the student creates or cancels a session, forcing a fresh fetch on the next screen open.

**Code location:** `lib/features/history/data/repositories/history_repository_impl.dart` → `getStudentHistory()`, `_safeUpsert()`, `_readHistoryCache()`.

### Privacy decision

Because session history contains sensitive data (tutor names, course details, prices, payment status), logout cleanup always:

- Deletes `cache_history` rows for the current user from SQLite
- Removes filter state keys from SharedPreferences
- Evicts the user's L1 `LinkedHashMap` entry

This prevents previous-user session data from leaking into a new session.


---

## 3.2.4 Multithreading Strategies

### Strategy 1 — `viewModelScope.launch` with `withContext(Dispatchers.IO)`

**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | When the History screen initializes, `viewModelScope.launch` starts a coroutine on the main thread and immediately emits Loading state. Inside, `withContext(Dispatchers.IO)` suspends execution and resumes it on the IO dispatcher thread pool to perform all I/O work in sequence: L1 memory cache check → L2 SQLite query (`cache_history`) → API call if needed. After data is retrieved, date range and course filters are applied **in-memory** on the same IO thread before `withContext` completes. Control then automatically returns to `Dispatchers.Main`, where `_uiState.value = Success(data)` is assigned safely. The full L1 → L2 → API cascade runs sequentially inside a single coroutine — each layer is only queried if the previous one misses or is expired. |
| **Why this over alternatives** | `Thread`/`AsyncTask` (deprecated) require manual `runOnUiThread` marshaling. `RxJava` adds heavy dependency overhead. `Handler`/`Looper` is verbose boilerplate. Kotlin Coroutines with `Dispatchers.IO` is the idiomatic Android solution: structured concurrency, automatic ViewModel-scoped cancellation, and sequential readable code with no callback nesting. Unlike Flutter's `Future`, `withContext` switches the actual execution context — the coroutine is truly moved to the IO thread pool, not just queued on an event loop. Applying filters inside `withContext` avoids a redundant dispatcher hop that would occur if filtering were done after returning to Main. |
| **Threading model** | Coroutine starts on **`Dispatchers.Main`** (main thread via `viewModelScope`), suspends into **`Dispatchers.IO`** (shared thread pool, up to 64 threads) for all cache reads, network I/O, and in-memory filter application, then automatically returns to **`Dispatchers.Main`** for the UI state update. No thread is ever blocked — suspension releases the thread to do other work while waiting. |
| **Error handling** | `try/catch` wraps the entire `withContext` block. Any exception from cache reads or the API call is caught, and the ViewModel falls back to serving stale L2 SQLite data. If no cached row exists either, `_uiState.value = Error(message)` is emitted with a retry action. The coroutine is bound to `viewModelScope` and cancelled automatically if the user navigates away before the load completes. |
| **Code location** | `features/history/presentation/HistoryViewModel.kt` → `loadHistory(studentId)` · `features/history/data/HistoryRepositoryImpl.kt` → `getStudentHistory()`, `_safeUpsert()`, `_readHistoryCache()` · `features/history/data/local/HistoryCacheDao.kt` → `getCachedHistory()`, `saveHistory()` |

---

### Strategy 2 — `MutableStateFlow` as a thread-safe UI state bridge

**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | The ViewModel declares `private val _uiState = MutableStateFlow<HistoryUiState>(Loading)` and exposes `val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()`. After any coroutine completes work on `Dispatchers.IO` and returns to `Dispatchers.Main`, it assigns `_uiState.value = Success(sessions, stats, uniqueCourses)` or `_uiState.value = Error(message)`. The Compose UI collects with `val state by viewModel.uiState.collectAsState()`, which triggers recomposition only on the parts of the tree that depend on the changed state. Filter changes do not emit a new `Success` — they reuse the already-cached list and trigger recomposition through a separate `filterState: StateFlow<HistoryFilter>`. |
| **Why this over alternatives** | `LiveData` requires lifecycle-aware observers and does not integrate as cleanly with Compose `collectAsState()`. `mutableStateOf` is UI-layer only and cannot be safely written from a background thread without an explicit dispatcher hop. `StateFlow` is coroutine-native, thread-safe by design, and the correct choice for ViewModel → Compose UI communication in a coroutine-first architecture. It also supports multiple collectors and replays the last value to new subscribers — essential for screen rotation without re-fetching. |
| **Threading model** | `_uiState.value` is always assigned on **`Dispatchers.Main`** after `withContext(Dispatchers.IO)` returns. `StateFlow` is internally thread-safe, but assigning on the main thread ensures Compose recomposition is triggered without needing `Dispatchers.Main.immediate` workarounds. Collectors on the UI side always receive emissions on the main thread via `collectAsState()`. |
| **Error handling** | `HistoryUiState` is a sealed class with `Loading`, `Success(sessions, stats, uniqueCourses, isFromCache)`, and `Error(message, retryAction)` subclasses. The ViewModel always emits `Loading` first, then transitions to `Success` or `Error`. `isFromCache = true` triggers the orange "Showing saved data" badge in the UI. This provides a clear state machine that the composable switches on exhaustively, preventing any partially-rendered state. |
| **Code location** | `features/history/presentation/HistoryViewModel.kt` → `_uiState`, `uiState`, `filterState` · `features/history/presentation/HistoryScreen.kt` → `val state by viewModel.uiState.collectAsState()` |

---
## 3.2.5 Caching Strategies

**Owner:** Nikol Katherin Rodriguez Ortiz

### Cache 1 — LRU In-Memory Cache (InMemoryCache.kt)

| Field | Content |
|-------|---------|
| **Data structure** | `InMemoryCache` wrapping a `LinkedHashMap<String, CacheEntry>` with `accessOrder = true`. Each `CacheEntry` stores the raw value and an insertion timestamp in milliseconds. |
| **Eviction policy** | Least Recently Used — `LinkedHashMap` with `accessOrder = true` automatically moves any accessed entry to the map's tail (MRU position) on every `get()` or `put()`. When `size > maxSize`, `removeEldestEntry()` fires and removes the head entry (LRU position) in O(1). |
| **Complexity** | `get`: O(1) · `put`: O(1) · `evict`: O(1) — access-ordered `LinkedHashMap` maintains LRU discipline natively with no extra bookkeeping. |
| **TTL** | No automatic per-entry expiry for session lists — staleness is resolved by the stale-while-revalidate background refresh. For user profiles resolved inside `mapCompletedSessions`, the insertion timestamp is compared against `userPrefs.cacheExpiryMs` before use; an expired entry falls through to SQLite, and a stale-but-existing profile is kept as offline fallback if the network call fails. |
| **Parameters** | `maxSize = 20` — fits simultaneous session and user-profile entries across navigated tutors without unbounded heap growth. |
| **Where used** | `HistoryCacheLoader` — keyed by `history_tutor_{tutorId}` / `history_student_{...}` for raw session lists, and `history_user_{userId}` for resolved user profiles. L1 is checked first on every screen entry; an L1 hit returns data with zero disk I/O. On an L2 (SQLite) hit, the entry is immediately promoted into L1 so subsequent accesses stay in RAM. |
| **Thread-safety** | Every public method is `@Synchronized`. `LinkedHashMap` is not thread-safe by default; the annotation serializes concurrent access from `Dispatchers.IO` and `Dispatchers.Main` coroutines without data races. |
| **Why LRU over alternatives** | FIFO would evict recently navigated tutors. LFU adds per-entry frequency counters with no benefit at n ≤ 20. LRU evicts the tutor whose sessions were accessed least recently — the most likely to be irrelevant — with zero extra bookkeeping. A plain `HashMap` cannot express access order and would require a separate eviction pass. |

---

### Cache 2 — SQLite Persistent Store (CacheDatabase.kt)

| Field | Content |
|-------|---------|
| **Data structure** | SQLite table `cache_home` with schema `(id TEXT PRIMARY KEY, json_data TEXT NOT NULL, timestamp INTEGER NOT NULL)`. Session lists and user profiles are serialized to JSON via Gson before storage and deserialized on read. |
| **Write policy** | `INSERT OR REPLACE` (upsert via `CONFLICT_REPLACE`) — each successful API response atomically overwrites the previous row for the same key. Only the latest snapshot is kept; no versioning or append logic. |
| **Read policy** | `SELECT` by primary key `id`; returns a `Pair<String?, Long>` of `(jsonData, timestamp)`. A null `json` signals a cache miss and triggers a network call. |
| **TTL** | Implicit via the `timestamp` column. Session lists have no hard expiry — the stale-while-revalidate pattern shows the existing row immediately and silently replaces it once the background fetch completes. User profiles check `(now − timestamp) < userPrefs.cacheExpiryMs`; a stale profile is kept as a graceful fallback when the network is unavailable. |
| **Concurrency** | All `saveCache` / `getCache` calls are suspend functions dispatched on `Dispatchers.IO`. SQLite operations never block the Main Thread; `withContext(Dispatchers.IO)` enforces this at the call site. |
| **Where used** | `HistoryCacheLoader.fetchAndCacheTutorHistory` / `fetchAndCacheStudentHistory` — write session JSON after each successful API call. `readCachedTutorHistory` / `readCachedStudentHistory` — read on L1 miss. `resolveUserProfile` — reads and writes per-user profile JSON with expiry validation. `HistoryViewModel` also writes `history_last_access_{tutorId}` fire-and-forget as an audit timestamp on every screen visit. |
| **Why SQLite over SharedPreferences** | `SharedPreferences` serializes all keys into a single XML file read entirely into memory. The history cache can hold arbitrarily large JSON arrays; SQLite provides row-level access, primary-key O(log n) lookup, and no full-file deserialization cost. |
| **Why two tiers** | L1 (`InMemoryCache`) absorbs repeated accesses within the same session with O(1) latency and no disk I/O; L2 (SQLite) survives navigation events, configuration changes, and app restarts, guaranteeing a readable offline state. The combination produces the stale-while-revalidate UX: instant response from local data + silent background update from the network. |
---


# 4. New Views

# 4.1 Flutter Version

## 4.1.1 Views Summary

| View ID | View Name | Team Member | Related Feature | Owner         |
| ------- | --------- | ----------- | --------------- |---------------|
|         |           |             |                 |               |
|         |           |             |                 |               |

---
### 4.1.2 Tutor Search Screen

#### View Description

A dedicated tutor discovery screen accessible from the bottom navigation bar (Search tab). Students can search tutors by name, filter by Price, Location, Course, and Rating, and browse a ranked list of available tutors. An Auto-Assign option is available for students who want the best match selected automatically.

---

#### UI Components

| Component | Description |
|-----------|-------------|
| **Search bar** | Text field to search tutors by name in real time |
| **Filter chips** | Four tappable pills: Price, Location, Course, Rating — each opens a filter selector |
| **Auto-Assign row** | Quick action that automatically selects the best available tutor based on active filters |
| **Tutor list** | Vertical scrollable list showing tutor avatar, name, and rating |
| **Bottom navigation** | Search tab (index 1) highlighted when on this screen |

---

#### Screen Behavior

| State | Behavior |
|-------|---------|
| **Default** | Shows all available tutors sorted by rating DESC |
| **Searching** | Filters list in real time by tutor name using `compute()` (Isolate) |
| **Filter applied** | List updates to show only tutors matching the selected filter combination |
| **Auto-Assign tapped** | Selects the top-rated tutor with the earliest available slot|
| **Offline** | Orange banner shown. Last cached results served. Filters and search disabled. |
| **No results** | Empty state with message and suggestion to clear filters |
| **Tutor tapped** | Opens `BookingBottomSheet` with tutor, studentId, and courseId |

---

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/902ef37a-e29f-457e-95ee-c08e0a00eb25" />

---

# 4.2 Kotlin Version

## 4.2.1 Views Summary

| View ID | View Name | Team Member | Related Feature |
|:---|:---|:---|:---|
| V001 | Home Page | Nikol Katherin Rodriguez Ortiz | F006, F007 |
| V002 | History | Nikol Katherin Rodriguez Ortiz | F012 |
| V003 | Availability | Nikol Katherin Rodriguez Ortiz | F004 |
| V004 | Login Screen | Daniel Camilo Quimbay Velazquez | F001, F011 |
| V005 | Register Screen | Daniel Camilo Quimbay Velazquez | F001, F011 |

---

## 4.2.2 History Screen

**View Description**

The History screen allows tutors to quickly review their past tutoring sessions without navigating through multiple screens. The user experience focuses on fast reading of important information, using clean visual cards, clear typographic hierarchy, and simple, intuitive navigation.

The screen maintains a minimalist structure with plenty of white space, reducing cognitive load and allowing the user to easily identify each registered tutoring session. The top title "History" together with the back button provides orientation within the app and makes it easy to return to the previous view without confusion.

Each tutoring session is represented by an independent card with rounded corners and soft shadows. This visual decision helps clearly separate each session and improves information scannability. The user can quickly identify where each record starts and ends, especially when many tutoring sessions are stored.

Inside each card, the information is organized hierarchically. At the top, the student's name and the type of tutoring are presented, allowing immediate recognition of the session. On the right, the tutoring price is highlighted with a differentiated color badge, improving the visibility of the economic value generated by each session.

The interface uses contextual icons to facilitate visual recognition of data without needing to read every text completely. Icons for user, calendar, clock, and money allow quick association of each section with its corresponding meaning. This improves reading speed and makes the experience more intuitive.

Secondary information such as date, time, and price uses a visual hierarchy based on typographic size and color. Descriptive labels have a softer tone while important values use greater contrast. This strategy naturally guides the user's attention to the most relevant data.

The bottom navigation maintains consistency with the rest of the application through a persistent bottom navigation bar. The active "History" section is visually highlighted using a differentiated background and prominent icon color, allowing the user to easily identify which module they are currently in.

The experience is also oriented toward efficiency. The user can review multiple sessions vertically through continuous scrolling (using a `RecyclerView`), avoiding unnecessary steps or additional interactions. This pattern improves accessibility and optimizes the history query flow.

Visually, the screen uses a warm and neutral palette based on soft beige and orange tones. This choice conveys warmth and maintains coherence with the overall application design. Moreover, the moderate use of colors avoids distractions and helps keep focus on the main information.

The interface was designed with real mobile devices in mind, prioritizing readable text sizes, large touch elements, and adaptable layout to different screen sizes (`ConstraintLayout`, `Material Design` components). This guarantees a consistent and usable experience on Android.

**UI Components (Android/Kotlin)**

| Component | Description |
|:---|:---|
| `ImageButton` / `Toolbar` | Back button at top-left to return to previous screen |
| `TextView` | Title "History" at the top, large and bold (`textAppearance="headlineSmall"`) |
| `CardView` | Container for each session with rounded corners, elevation, and ripple effect |
| `ImageView` / `CircleImageView` | Student avatar (icon or placeholder image) |
| `TextView` | Student name primary text (e.g., "María García") |
| `TextView` | Tutoring type secondary text (e.g., "Math Tutoring") |
| `TextView` / `Chip` | Price badge with colored background showing session price |
| `LinearLayout` (horizontal) | Date row: icon + formatted date (e.g., "Apr 15, 2026") |
| `LinearLayout` (horizontal) | Time row: icon + time range (e.g., "3:00 PM - 4:30 PM") |
| `BottomNavigationView` | Persistent bar with navigation options (Home, History, etc.), "History" item highlighted |
| `RecyclerView` | Vertical scrollable list of history cards with `LinearLayoutManager` |
| `SwipeRefreshLayout` | Optional pull-to-refresh to reload history |

**Screen Behavior (Kotlin + ViewModel)**

| State | Behavior |
|:---|:---|
| Default | Shows all past tutoring sessions sorted by most recent date descending (`LiveData` / `StateFlow` from `ViewModel`). |
| Loading | Displays a progress bar or shimmer effect while fetching history data from local database (`Room`) or API (`Retrofit`). |
| Empty | Shows friendly message "No tutoring sessions yet" with an illustration (`ImageView` + `TextView`). |
| Error | Shows error snackbar or banner with retry button. |
| Offline | Serves cached history data from `Room` if available, otherwise shows empty state. |
| Card tapped | (Optional) Opens a detail view (`Activity` or `Fragment`) with full session information, student feedback, and notes. |
| Scroll | Infinite scroll pagination – loads more sessions when reaching the bottom (using `PagingSource` or scroll listener). |

**Screen Mockup**

*(Place your screenshot here)*

![History Screen](path/to/history_screen.png)


---

# 5. Business Questions (BQ)

## 5.1 BQ Summary

| ID | Business Question | Type | Owner | Sprint |
|:---|:---|:---|:---|:---|
| **BQ1** | How stable and performant is the application during peak academic periods (crash rate, API latency, and failed booking/payment events)? | Type 1  | Daniel Camilo Quimbay Velásquez | 4 |
| **BQ3** | For a given student, which tutors with rating >4.5 and availability within the next 4 hours should be recommended? | Type 2 | Maria Lucia Benavides Domínguez | 4 |
| **BQ4** | What is the student search volume per hour for a tutor's specific subjects compared to their current availability? | Type 2 | Nikol Katherin Rodriguez Ortiz | 2 |
| **BQ5** | What percentage of booking attempts for the earliest available slot succeed instantly without manual confirmation? | Type 2 | Paola Catherine Jimenez Jaque | 4 |
| **BQ8** | Which student-side features correlate most strongly with higher booking frequency and repeat session rates over time? | Type 3 | Maria Lucia Benavides Domínguez | 4 |
| **BQ9** | How often do tutors use profile optimization tools, and which elements are most frequently updated? | Type 3 | Paola Catherine Jimenez Jaque | 4 |
| **BQ10** | What percentage of booked sessions originate from the "Combined availability" view versus the standard search results with specific availability? | Type 3 | Paola Catherine Jimenez Jaque | 4 |
| **BQ14** | What is the cancellation rate of confirmed tutoring sessions within 12 hours of start time under the current deposit-based confirmation policy during exam weeks? | Type 5 | Daniel Camilo Quimbay Velásquez | 4 |
| **BQ15** | Does the average homepage load time exceed the performance threshold of 2 seconds, and in what percentage of sessions is this target not met? | Type 1 | Nikol Katherin Rodriguez Ortiz | 3 |
| **BQ16** | What is the weekly percentage of tutors who use the history view feature? | Type 3 | Nikol Katherin Rodriguez Ortiz | 4 |


---

### 5.2 BQ9 – Tutor Profile Optimization Usage
**Question:** How often do tutors use profile optimization tools, and which elements are most frequently updated?
> Implemented by Paola Catherine Jimenez Jaque

#### Type of BQ
This is a **Type 3 Business Question (Monitoring & Retention)** because it:
- Tracks tutor engagement with the platform over time
- Identifies which profile fields tutors consider most important
- Helps the platform understand which optimization tools drive tutor retention

#### How the Question is Answered

**Core Logic**
- Every time a tutor updates their profile via `PATCH /users/:id`, the updated fields are logged to a `profile_updates` collection in Firestore
- Each document stores which fields were changed, the tutorId, and a timestamp
- The analytics endpoint aggregates these logs to count update frequency per field

**Processing Steps**

| Step | Layer | Description |
|------|-------|-------------|
| 1. Event capture | Bronze | Every `PATCH /users/:id` request logs the updated field names to `profile_updates` collection |
| 2. Aggregation | Silver | `GET /analytics/profile-update-stats` queries `profile_updates`, groups by field name, counts occurrences |
| 3. Ranking | Silver | Fields sorted by update frequency DESC |
| 4. Visualization | Gold | Dashboard bar chart showing update count per field |

#### Derived Metrics

| Metric | Formula |
|--------|---------|
| **Updates per field** | COUNT(profile_updates where field = X) |
| **Most updated field** | field with MAX(count) |
| **Total profile updates** | COUNT(all profile_updates documents) |
| **Active tutors** | COUNT(DISTINCT tutorId in profile_updates) |

#### Pipeline Architecture

| Layer | Contents |
|-------|----------|
| **Bronze** | `profile_updates` collection — raw log documents with `tutorId`, `fields[]`, `timestamp` |
| **Silver** | Group by field name → COUNT per field → sort DESC |
| **Gold** | AGGREGATE → SHAPE → WRAP → SERVE `GET /analytics/profile-update-stats` → Dashboard bar chart |

#### Architectural Location

```
analytics → profileUpdateStats (BQ9)
users → user.service → updateUser() → logs to profile_updates
```

#### Data Logged per Update

```typescript
// Logged on every PATCH /users/:id
await db.collection('profile_updates').add({
  tutorId: userId,
  fields: Object.keys(updateDto), // e.g. ['bio', 'hourlyRate']
  timestamp: new Date(),
});
```

#### Visualization Justification
- Included in the existing dashboard alongside other system metrics
- Bar chart clearly shows which fields tutors update most — actionable for product decisions
- Connects directly to the backend via `GET /analytics/profile-update-stats` — realtime, no files

#### Business Value
- Identifies which profile features tutors value most
- Informs which optimization tools to improve or promote
- Tracks tutor engagement with the platform over time
- Higher profile update frequency correlates with more active tutors

#### Rationale
The pipeline follows a **Bronze → Silver → Gold architecture**:

- **Bronze Layer**
    - Raw collection: `profile_updates`
    - Each document logged at the moment of a PATCH request
    - No hardcoded data — every log is a real tutor action

- **Silver Layer**
    - Firestore query groups documents by field name
    - In-memory aggregation counts occurrences per field
    - Sorts by frequency DESC

- **Gold Layer**
    - **AGGREGATE**: COUNT per field + DISTINCT tutor count
    - **SHAPE**: ranked array of `{ field, count }` objects
    - **WRAP**: API response with metadata
    - **SERVE**: `GET /analytics/profile-update-stats` → Dashboard

#### Analytics Pipeline Diagram

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/93d03cc4-62f5-43ae-9069-af52a48d4083" />


### 5.2 BQ16 - Use of history by tutor

**Owner:** Nikol Katherin Rodriguez Ortiz

> **Question:** What is the weekly percentage of tutors who use the history view feature?
---
### 5.3 Type of BQ
This is a **Type 3 Business Question (Features Analysis)** because it:
- Measures the adoption rate of an existing feature (the history view)
- Uses internal telemetry data collected from tutor interactions
- Helps the business decide whether to keep, improve, or remove the history view feature based on real usage
- Provides evidence about user acceptance to support UX/UI decisions
---
### 5.4 How the Question Is Answered
For each tutor interaction with the history view within a defined weekly window, an analytics event is built containing:
- The tutor identifier (`tutorId`)
- The type of event triggered (`eventType = "history_view_opened"`)
- The exact moment the event occurred (`timestamp`)
- Optional contextual data (`metadata`)

Then, across all events of the selected week, the following are computed:
- **Unique tutors using the feature** — distinct count of `tutorId` values across all events
- **Total active tutors** — total number of registered tutors retrieved from the users collection
- **Weekly usage percentage** — percentage of active tutors who opened the history view at least once

The BQ is answered with one main outcome:
1. What percentage of tutors used the history view this week? → **X%**

Complementary outputs include the daily event distribution and the top tutors by event count.

---
### 5.5 Processing Steps (High-Level Summary)
1. Read raw events from the `history_analytics` Firestore collection, filtering by `eventType = 'history_view_opened'` and the selected weekly window (`weekStart` to `weekEnd`)
2. Join with the `users` collection to retrieve all registered tutors (`isTutor = true`) and enrich event data with tutor names
3. Filter valid events — exclude:
    - Events with missing or null `tutorId`
    - Events outside the selected weekly range
4. Per event, evaluate:
    - Whether the tutor is already counted in the unique tutors set
5. Aggregate:
    - `totalEvents` — total valid history view events in the week
    - `uniqueTutorsUsing` — distinct tutors who triggered at least one event
    - `totalActiveTutors` — total tutors retrieved from the users collection
6. Calculate final metrics:
    - `weeklyPercentage = (uniqueTutorsUsing / totalActiveTutors) × 100`
    - `eventsByDay` — grouped event count per day of the week
    - `topTutors` — ranked list of tutors by event count, including tutor name
7. Return BQ16 answer via `GET /analytics/history/bq16` or `GET /analytics/history/bq16/week?start=...&end=...`
---
#### Metrics Derived from This Processing
| Metric | Description |
|---|---|
| **Active Tutors (Cohort)** | Total number of registered tutors retrieved from the users collection |
| **Unique Tutors Using Feature** | Distinct tutors who opened the history view at least once in the week |
| **Weekly Usage Percentage (%)** | Percentage of active tutors who used the history view during the selected week |
| **Total Events** | Raw count of all history view events recorded in the week |
| **Events by Day** | Distribution of events across each day of the selected week |
| **Top Tutors** | Ranked list of the most active tutors by event count, including their names |
| **BQ16 Answer** | Numeric: the weekly percentage of tutors using the history view |
| **Temporal Trend** | Whether the weekly usage percentage grows, remains stable, or decreases across multiple weeks |
---
### 5.6  Pipeline Diagram


---

