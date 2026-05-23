# Sprint 3 – App Implementation and Definition

## General Information

| | |
|---|---|
| **Sprint** | Sprint 3 – App Implementation and Definition |
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

## 1. Problem & Solution

### Problem

The university peer-tutoring ecosystem is currently paralyzed by structural disorganization, relying on informal, decentralized channels like WhatsApp that create a significant "coordination and discovery gap". This fragmentation forces students into a 48-hour confirmation void while preventing qualified tutors from efficiently broadcasting their availability to those in need. Because there is no centralized platform for academic transparency, students are forced into a "quality gamble", risking time and money on superficial assistance rather than the deep foundational understanding required for long-term success. Ultimately, this lack of a formal marketplace transforms what should be a simple academic exchange into a frustrating, inefficient, and unreliable process for both parties.

### Solution

Our solution is a Centralized Peer-Tutoring Marketplace that replaces informal negotiation with a professionalized, dual-sided ecosystem built on a synchronous booking engine and verified academic transparency. By centralizing the tutor-student connection, the platform allows students to move instantly from "academic crisis" to a confirmed session via real-time scheduling and verified tutor profiles that emphasize teaching depth. Simultaneously, the platform empowers tutors by providing a professional management dashboard to track performance, earnings, and availability, effectively transforming fragmented peer-help into a reliable, scalable, and data-driven academic economy.

---

## 2. Business Questions (BQs)

| ID | Business Question | Type | Owner |
|----|-------------------|------|-------|
| **BQ14** | What is the cancellation rate of confirmed tutoring sessions within 12 hours of start time under the current deposit-based confirmation policy during exam weeks? | Type 5 | Daniel Camilo Quimbay Velásquez |
| **BQ8** | Which student-side features correlate most strongly with higher booking frequency and repeat session rates over time? | Type 3 | Maria Lucia Benavides Domínguez |
| **BQ15** | Does the average homepage load time exceed the performance threshold of 2 seconds, and in what percentage of sessions is this target not met? | Type 1 | Nikol Katherin Rodriguez Ortiz |
| **BQ10** | What percentage of booked sessions originate from the "Combined availability" view versus the standard search results with specific availability? | Type 3 | Paola Catherine Jimenez Jaque |

### BQ15

**Owner:** Nikol Katherin Rodriguez Ortiz

> **Question:** Does the average homepage load time exceed the performance threshold of 2 seconds, and in what percentage of sessions is this target not met?

---

#### Type of BQ

This is a **Type 1 Business Question (App Telemetry & Performance Monitoring)** because it:
- Measures application performance through homepage load time
- Uses internal telemetry data collected from user sessions
- Evaluates whether the system meets a defined performance threshold (2 seconds)
- Helps identify stability and performance issues that impact user perception

---

#### How the Question Is Answered

For each app session within a defined time window, a performance record is built containing:

- The measured homepage load time in milliseconds (`load_time_ms`)
- A binary flag indicating whether the session exceeded the 2-second threshold (`exceeded_threshold`)
- Session metadata: `user_id`, `session_id`, `timestamp`, `device_type`

Then, across all sessions, the following are computed:

- **Average load time** — mean of all `load_time_ms` values
- **Threshold exceedance rate** — percentage of sessions where `load_time_ms > 2000 ms`

The BQ is answered with two outcomes:
1. Does the average load time exceed 2 seconds? → **Yes / No**
2. What percentage of individual sessions fail to meet the target? → **X%**

---

#### Processing Steps (High-Level Summary)

1. Read raw `app_events` from the data source, filtering by `event_type = 'homepage_load'` and the selected analysis time window
2. Join with `users` to enrich records with user context (optional: segment by user role, device, OS)
3. Filter valid sessions — exclude:
    - Incomplete loads (app crashed before homepage rendered)
    - Sessions with missing or null `load_time_ms`
4. Per session, evaluate:
    - `exceeded_threshold = load_time_ms > 2000`
5. Aggregate:
    - `total_sessions` — total valid homepage load events
    - `exceeded_sessions` — count where `exceeded_threshold = true`
6. Calculate final metrics:
    - `avg_load_time_ms = mean(load_time_ms)`
    - `pct_exceeding = (exceeded_sessions / total_sessions) × 100`
7. Return BQ15 answer via `GET /analytics/homepage-performance`

---

#### Metrics Derived from This Processing

| Metric | Description |
|---|---|
| **Sessions (Cohort)** | Total valid homepage load events within the selected time window |
| **Average Load Time (ms)** | Mean homepage load duration across all sessions in the cohort |
| **Threshold Exceedance Rate (%)** | Percentage of sessions where load time exceeded 2000 ms |
| **Exceeded Session Count** | Raw count of sessions above the 2-second threshold |
| **Within-Threshold Session Count** | Raw count of sessions that met the ≤ 2s target |
| **BQ15 Answer** | Boolean: does `avg_load_time_ms > 2000`? + the exceedance percentage |
| **Temporal Trend** | Whether exceedance rate remains stable or worsens across time buckets within the window |

---

#### Pipeline Diagram


<img width="45%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/bq15_pipeline_bronze_silver_gold.png" />

---
### BQ8

**Owner:** Maria Lucia Benavides Domínguez

> **Question:** Which student features are most strongly associated with higher booking frequency and higher repeat session rate, and do these relationships remain stable over time?

---

#### Type of BQ

This is a **Type 3 Business Question (Behavioral Drivers & Retention Analysis)** because it:
- Identifies which student behaviors or product features are associated with stronger engagement
- Uses internal platform usage and tutoring session data
- Evaluates how specific binary features influence booking frequency and repeat session rate
- Helps prioritize product improvements that increase retention and repeated usage

---

#### How the Question Is Answered

For each student within a defined time window, a profile is built containing:

- Whether the student has each feature or not (binary features)
- Two outcome metrics:
    - Booking frequency
    - Repeat session rate

Then, for each feature, the association between that yes/no condition and each outcome is measured using:

- Point-biserial correlation
- Uplift analysis between:
    - students with the feature
    - students without the feature

Features are then ranked by the strength of that association, and a time-based analysis is added to evaluate whether the pattern remains stable across different periods.

The BQ is answered with two outcomes:
1. Which features show the strongest positive association with booking frequency and repeat rate? → **Ranked feature list**
2. Are these relationships stable across time periods? → **Yes / No + temporal trend interpretation**

---

#### Processing Steps (High-Level Summary)

1. Read tutoring sessions and auxiliary datasets from the data source, including:
    - booking context
    - carousel events when applicable
    - tutor ratings
2. Filter by:
    - selected analysis time window
    - valid sessions only, excluding:
        - cancelled sessions
        - declined sessions
        - rejected sessions
3. Aggregate by student:
    - binary feature flags
    - booking frequency
    - repeat session rate
4. Calculate:
    - feature × outcome correlations using point-biserial correlation
    - uplift values between students with and without each feature
5. Flag low-support features where one group has insufficient sample size
6. Rank features by absolute correlation strength
7. Add temporal trend analysis using time buckets
8. Return BQ8 answer via `GET /analytics/student-feature-impact`

---

#### Metrics Derived from This Processing

| Metric | Description |
|---|---|
| **Students / Sessions (Cohort)** | Total students and valid tutoring sessions included within the selected time window |
| **Average Frequency (Cohort)** | Mean number of tutoring sessions per week across students |
| **Average Repeat Rate (Cohort)** | Mean fraction of sessions that repeat the same tutor + course pair |
| **Feature Adoption** | Percentage and counts of students with vs without a given feature |
| **Frequency Correlation** | Strength of association between a binary feature and booking frequency |
| **Repeat Rate Correlation** | Strength of association between a binary feature and repeat session rate |
| **Uplift** | Difference in average outcomes between students who use a feature and those who do not |
| **Feature Ranking** | Ordered list of features based on strongest absolute association with frequency or repeat rate |
| **Low Support Flag** | Indicates insufficient sample size for reliable comparison |
| **Temporal Trend** | Whether feature impact remains stable or changes across time buckets within the analysis window |
| **BQ8 Answer** | Ranked list of strongest drivers + interpretation of temporal stability |

---

###@ Pipeline Diagram


<img width="35%" alt="image" src="https://github.com/user-attachments/assets/a63c7b49-1206-4da4-b039-e1ff8c08a318" />

-----

### BQ10 – Booking Source Distribution
**Owner:** Paola Catherine Jimenez Jaque

> **Question:** What percentage of booked sessions originate from the "Combined availability" carousel view versus standard search results?

#### Type of BQ

This is a **Type 3 Business Question (Monitoring & Funnel Analysis)** because it:
- Tracks user behavior across different booking entry points
- Measures the effectiveness of the carousel feature vs standard search
- Helps evaluate which UI flow drives more conversions over time

#### How the Question is Answered

**Core Logic**
- Every booking attempt records a `bookingSource` field in `tutoringSessions`
- `bookingSource: 'carousel'` → originated from the "Top Rated & Available Soon" carousel
- `bookingSource: 'search'` → originated from standard search results
- Distribution = COUNT per source / COUNT total × 100

#### Processing Steps

| Step | Layer | Description |
|------|-------|-------------|
| 1. Source Tagging | Bronze | Flutter app sends `bookingSource` field in `POST /tutoring-sessions` at booking time |
| 2. Session Storage | Bronze | `tutoringSessions` collection stores every booking with its `bookingSource` |
| 3. Aggregation | Silver | `GET /analytics/booking-source-stats` queries Firestore and groups sessions by `bookingSource` |
| 4. Metric Calculation | Silver | Computes `carouselBookings`, `otherBookings`, and `carouselPercentage` in-memory |
| 5. Visualization | Gold | Dashboard displays a card with total bookings, carousel %, and a bar chart over time |

#### Derived Metrics

| Metric | Formula |
|--------|---------|
| **Carousel Bookings** | COUNT(tutoringSessions where `bookingSource = 'carousel'`) |
| **Other Bookings** | COUNT(tutoringSessions where `bookingSource ≠ 'carousel'`) |
| **Carousel Percentage** | carouselBookings / totalSessions × 100 |

#### Pipeline Architecture

| Layer | Contents |
|-------|----------|
| **Bronze** | `tutoringSessions` — raw session documents with `bookingSource` field |
| **Silver** | Source grouping → COUNT per source → percentage calculation |
| **Gold** | AGGREGATE → SHAPE → WRAP → SERVE `GET /analytics/booking-source-stats` → Dashboard card |

#### Pipeline Diagram

<img width="65%" alt="Pipeline2" src="https://github.com/user-attachments/assets/2a1aa1b0-9072-4d5a-94ef-12fd7dfb0f70" />

### BQ14 – Cancellation Elasticity & Policy Impact

**Owner:** Daniel Camilo Quimbay Velásquez

> **Question:** What is the cancellation rate of confirmed tutoring sessions within 12 hours of the start time under the current deposit-based confirmation policy during exam weeks?

---

#### Type of BQ

This is a **Type \*** Business Question because it encompasses a mix nature of several categories:
- **Type 2 (UX Improvement):** By analyzing cancellation behavior during high-stress exam weeks, it helps refine notifications or reminders to support the user's academic context.
- **Type 4 (Benefits from Data):** It evaluates the effectiveness of the **deposit-based policy** (financial friction) in protecting revenue and tutor availability, allowing the business to optimize profit by reducing lost opportunity costs.

---

#### How the Question Is Answered

For each tutoring session scheduled within the defined "Exam Week" cohorts, a performance record is processed containing:

- **Policy Compliance:** A binary flag in the payment entity related to the session indicating if a deposit was successfully captured (`paid`).
- **Cancellation Latency:** The time delta in hours between `cancellation_timestamp` and `scheduled_start_time`.

The BQ is answered with the core metric:
1. **Late Attrition Rate** — The percentage of confirmed sessions where cancellation occurred within the < 12h window.

---

#### Processing Steps (High-Level Summary)

1. **Ingestion & Filtering:** Read raw `tutoring_sessions` from the data source, filtering for sessions with `status = 'cancelled'`.
2. **Data Cleansing:** Exclude system-generated cancellations (e.g., payment failures) to focus strictly on **User-Initiated Attrition**.
3. **Latency Calculation:** Compute the cancellation window per session:
   $$Latency = \frac{StartTime - CancellationTime}{3600000}$$
4. **Categorization:** Flag sessions where $Latency \leq 12$.
5. **Aggregation:**
    - `total_confirmed_sessions`: Count of all sessions with a deposit in the window.
    - `critical_cancellations`: Count of sessions cancelled with $\leq 12$ hours notice.
6. **Final Computation:**
    - `late_cancellation_rate = (critical_cancellations / total_confirmed_sessions) * 100`
7. **Output:** Return BQ14 result in the dasboard via `GET /analytics/dashboard`.

---

#### Metrics Derived from This Processing

| Metric | Description |
|---|---|
| **Peak Demand Cohort** | Total valid tutoring sessions processed within the Exam Week window. |
| **Late Cancellation Rate (%)** | Percentage of confirmed sessions cancelled within 12 hours of start. |
| **Opportunity Cost (Hours)** | Total tutor hours lost due to late-window cancellations. |
| **Deposit Yield** | Total revenue/points forfeited by students due to policy enforcement. |
| **Policy Sensitivity** | Variance in cancellation rates during high-stress weeks vs. standard weeks. |
| **BQ14 Answer** | The final percentage of attrition and its trend relative to previous exam cycles. |

---

#### Pipeline Architecture

| Layer | Contents |
|-------|----------|
| **Bronze** | Raw `tutoring_sessions` and `payments` collections with status and timestamps. |
| **Silver** | Latency calculation ($StartTime - CancelTime$) + Academic Calendar join. |
| **Gold** | AGGREGATE → SHAPE → SERVE `GET /analytics/dashboard` → Metrics displayed on the dashboard. |

#### Pipeline Diagram

<img width="100%" alt="KmiloBQ" src="https://github.com/user-attachments/assets/ba77fefd-88cd-4609-960c-e047cba7b7f7" />

---

## 3. All the Features in Each App

### Flutter

#### Features Implemented:

| Category | Feature | Description |
|:---|:---|:---|
| **Authentication** | **Email/Password + Google Login** | Complete auth flow with Firebase and backend integration: register, login, Google sign-in, password reset, logout, and session restoration on app start. |
| **Authentication** | **Connectivity-Aware Login** | Login screen detects offline state, informs the user, and applies eventual connectivity behavior (pending login intent + retry on reconnect) for smoother sign-in UX. |
| **Home** | **Session Overview (Upcoming + Pending Sync)** | Home dashboard consolidates upcoming sessions from backend and offline-pending bookings queued locally, including contextual states for loading, retry, and sync progress. |
| **Home** | **Recommended Courses** | Recommendation view highlights top courses using session history frequency, helping students quickly return to frequently used tutoring topics. |
| **Home** | **Course Search & Filtering** | Real-time course filtering from the home search bar, including isolate-based filtering to keep UI responsive while typing. |
| **Course Detail** | **Tutor Availability (Next 4 Hours)** | Course detail loads top-rated tutors available soon, supports booking from carousel cards, and tracks interaction analytics events. |
| **Course Detail** | **Your Go-To Tutor** | Personalized per-course tutor card based on past student behavior, with booking entry point and offline fallback from local cache when network/API fails. |
| **Booking** | **Offline Booking Queue + Reconnect Sync** | When offline, bookings are stored locally (SQLite/Drift queue) and automatically synchronized to backend once connectivity returns, preserving user intent. |
| **Profile** | **Profile Management + Offline Edit Sync** | Profile data is cached for offline reads, description edits are saved as pending updates, and sync is executed automatically on reconnection. |
| **Sensor Feature** | **Motion Alert Configuration** | Users can configure alert destination, identity, and location; settings persist locally and are restored across app restarts. |
| **Sensor Feature** | **Motion Alert Trigger + Local Audit Log** | Motion events can trigger emergency alert attempts; success/failure is recorded in local logs and exposed in alert history for traceability. |
| **Offline & Cache** | **Multi-Storage Connectivity Strategy** | Combines SharedPreferences, Hive, SQLite/sqflite, and Drift to provide remote-first behavior with robust offline fallback and eventual consistency by feature. |

Design choices

| Category | Diseño | Detalle |
|---|---|---|
| Design System | Identidad visual consistente | Uso de paleta cálida institucional (primario/natural), tipografía uniforme y estilos centralizados para botones, textos y estados. |
| Design System | Componentes reutilizables | Construcción basada en widgets compartidos (SectionHeader, AppBottomNav, OfflineCacheNotice, tarjetas) para mantener coherencia visual entre pantallas. |
| Navigation UX | Flujo principal simplificado | Navegación lineal clara: autenticación → inicio → detalle de curso/sesión → perfil, minimizando fricción para tareas frecuentes. |
| Navigation UX | Navegación contextual por tareas | Desde Home se priorizan acciones de alto valor (buscar curso, revisar sesiones, reservar tutor) con accesos directos y CTA visibles. |
| Home UX | Jerarquía de información | Home organiza contenido por prioridad: estado/contexto, recomendados, cursos y sesiones; facilita escaneo rápido para estudiantes. |
| Home UX | Diseño orientado a estado | Vistas diferenciadas para loading, success, offline, error y retry, evitando pantallas ambiguas o vacías sin explicación. |
| Course Detail UX | Card-centric decision flow | Tarjetas de tutor diseñadas para comparación rápida (rating, disponibilidad, ubicación), con acción de reserva integrada. |
| Course Detail UX | Feedback de conectividad en contexto | En detalle de curso se muestran avisos de caché/offline y opciones de reintento donde ocurre el problema, no en un mensaje global. |
| Profile UX | Perfil editable con confianza | Edición con confirmación visual y estados de “pendiente de sincronización”, permitiendo continuidad incluso sin red. |
| Sensor Feature UX | Configuración guiada de Motion Alert | Formulario orientado a propósito (correo, nombre, ubicación, activación) con foco en claridad y bajo esfuerzo cognitivo. |
| Offline UX | Transparencia de datos locales | La UI comunica cuándo los datos provienen de caché y cuándo hay sincronización pendiente, mejorando confianza del usuario. |
| Resilience UX | Recuperación explícita | Botones de “Reintentar”, refresco al reconectar y mensajes accionables; evita depender de un único mensaje genérico para toda la app. |
| Accessibility & Readability | Legibilidad y contraste | Tamaños de texto, espaciado y contraste diseñados para lectura móvil rápida; iconografía de soporte para reforzar semántica de estado. |
| Performance Perception | Respuesta percibida fluida | Cargas parciales, caché local y actualizaciones progresivas reducen espera percibida y mantienen sensación de app “siempre disponible”. |


### Kotlin

| Category | Feature | Description |
|:---|:---|:---|
| **Authentication** | **Secure Multi-Provider Auth** | Complete flow supporting Firebase Email/Password and **Google Login**. Implements **EncryptedSharedPreferences** to secure auth tokens and persist sessions. |
| **User Profile** | **Profile Management** | Dedicated view for tutor identity. Implements a **Triple-Layer Cache Strategy** (Manual LRU Memory Cache + Disk Cache + Network) to optimize image loading and data consumption. |
| **Courses** | **Course Catalog & Application** | Dynamic interface to browse and apply for tutoring subjects. Implements **Eventual Connectivity** using **SQLite Relational DB** to allow browsing previously loaded courses while offline. |
| **Availability** | **Availability Management (CRUD)** | Comprehensive management system to **Create, Read, Update, and Delete** tutoring time slots, ensuring real-time synchronization with the backend availability engine. |
| **Sensor Feature** | **Shake-to-Report Bug** | Hardware integration using the **Accelerometer** to detect shake gestures, triggering an automated bug report via Email Intent with embedded device telemetry. |
| **Home Page** | **Session Overview** | Centralized dashboard displaying synchronized "Incoming" and "Upcoming" sessions via the NestJS API. |
| **Home Page** | **Occupancy Analysis** | Algorithmic visualization of tutor workload by calculating sessions per hour vs. available slots to identify peak demand periods. |
| **Home Page** | **Tutor Recommendations** | Intelligent suggestion engine utilizing collaborative filtering based on historical booking patterns and search intent. |
| **Analytics** | **Stability Dashboard** | Telemetry monitoring module that tracks application health, crash-free session percentages, and API latency. |
| **External Service** | **Session History** | Chronological aggregation of the two most recent and two upcoming tutoring engagements. |

---

## 4. Eventual Connectivity Strategies

### Flutter

### Feature 1 — Profile Edit with Offline Queue
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **Event description** | User taps edit on their profile description and saves. |
| **System response** | **Offline:** description saved to `SharedPreferences` under `pending_profile_update_<userId>`. Profile updated optimistically in memory, UI reflects change immediately with a *"Pending sync"* badge. **Online:** `PATCH /users/<userId>` called normally, response cached. **On reconnect:** `StreamSubscription` detects connectivity, syncs pending update, removes key on success. |
| **Possible anti-patterns** | Blocking UI waiting for PATCH response · Not clearing the pending key after sync · Overwriting a newer server value with stale local data · Storing raw objects instead of JSON strings. |
| **Caching strategy** | Full profile JSON cached after every successful API call. No TTL, invalidated explicitly when a successful PATCH returns. Cache hit shows an orange *"Showing saved data"* badge. |
| **Storage type** | **SharedPreferences**, ideal for a single small JSON object per user. Hive or SQLite would be overkill here. |
| **Stored data type** | `String` (JSON). Serialized via `jsonEncode(profile.toJson())`, deserialized via `UserProfile.fromJson(jsonDecode(stored))`. |

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/977384f0-2cc3-405d-b7ca-f405fbf2dbcf" />
<img width="25%" alt="image" src="https://github.com/user-attachments/assets/92c4d777-e84d-4429-b954-480e25da5c39" />



---

### Feature 2 — Tutor Availability Cache
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **Event description** | User opens a course detail screen, triggering `getAvailableTutors(courseId)`. |
| **System response** | **Cache hit (< 30 min):** returns cached JSON immediately, no network call. **Cache miss / expired:** fetches from `/analytics/bookable-tutors`, caches result with timestamp. **Offline + expired:** serves stale cache instead of empty screen. |
| **Possible anti-patterns** | No TTL (stale slots shown as available) · In-memory only cache (lost on restart) · Never invalidating after a booking. |
| **Caching strategy** | TTL-based (30 min). Each entry stores a `cachedAt` timestamp. Explicitly invalidated per `courseId` after a successful booking. |
| **Storage type** | **Hive**, stores multiple entries (one per courseId) efficiently. Simpler than SQLite for a key-value structure with no relational needs. |
| **Stored data type** | `String` (JSON with shape `{"tutors": [...], "cachedAt": "ISO8601"}`). Deserialized via `AvailableTutorModel.fromJson()`. |

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/d0f62e77-375c-4672-b104-e149ec2d6b15" />
<img width="25%" alt="image" src="https://github.com/user-attachments/assets/0e63cc08-81e5-4c27-be5b-1b0afa74a4f3" />

---

### Feature 3 — Offline Session Booking Queue
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **Event description** | User taps "Book Now" while offline. Sync triggered on reconnect via `StreamSubscription`. |
| **System response** | **Offline:** row inserted into `pending_sessions` table with `synced = false`. Home screen shows a *"Pending Sync"* section with badges. **On reconnect:** `SyncService` reads all unsynced rows, POSTs each to `/tutoring-sessions`, marks `synced = true` on success. Booking moves to *"Upcoming Sessions"* automatically. |
| **Possible anti-patterns** | Blocking UI on DB insert · No per-row failure isolation (one bad row blocks all) · Polling instead of streaming connectivity · Deleting rows instead of marking synced. |
| **Caching strategy** | No TTL, rows kept until synced. `synced = true` acts as the invalidation flag. Audit trail preserved after sync. |
| **Storage type** | **SQLite via Drift**, structured relational data with sync state that needs querying (`WHERE synced = false`). SharedPreferences can't query by field; Hive lacks type-safe queries. |
| **Stored data type** | Typed Drift columns, `TextColumn`, `DateTimeColumn`, `IntColumn`, `BoolColumn`. No manual serialization needed; Drift handles all type mapping automatically. |

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/edc86894-536e-473a-a923-9b4f25f66ec0" />
<img width="25%" alt="image" src="https://github.com/user-attachments/assets/0547c6dc-3038-4e91-8ead-49ecbec3f59b" />
<img width="25%" alt="image" src="https://github.com/user-attachments/assets/3cd02769-3b9b-41f4-ae6c-b28bbdc6a571" />

---


### Feature 4 — Upcoming Sessions (Remote-First + SQLite Offline Fallback)
**Owner:** Maria Lucia Benavides Domínguez

| Field                      | Content                                                                                                                                                                                                                                                                                                                                                                            |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Event description**      | Student opens Home and the app loads **Upcoming Sessions** (`getUpcomingSessions(studentId)`).                                                                                                                                                                                                                                                                                    |
| **System response**        | **Online:** app fetches sessions from backend (`_sessions.getStudentSessions(studentId)`), filters future sessions (`startDateTime > now`), sorts by start time, then upserts cache in SQLite (`cache_upcoming_sessions`). **Offline / API failure:** app reads cached payload by `student_id` and returns it as `CachedResult(..., isFromCache: true)`.                                           |
| **Possible anti-patterns** | Querying backend on every rebuild · Not filtering/sorting before persisting · Dropping cached data when one request fails · Storing each session in ad-hoc keys without user scoping.                                                                                                                                                                                            |
| **Caching strategy**       | Remote-first with deterministic fallback. Cache entry is replaced on successful remote read. No fixed TTL; freshness is communicated through `last_updated`.                                                                                                                                                                                                                      |
| **Storage type**           | **SQLite (`sqflite`)** via `AppDatabaseService`, because this feature needs stable keyed lookups by `student_id`, structured payload persistence, and timestamp metadata.                                                                                                                                                                                                         |
| **Stored data type**       | `payload` JSON (`List<Map<String, dynamic>>`) + `last_updated` epoch milliseconds. Serialized with `jsonEncode`, restored with `jsonDecode`, then mapped back to `SessionEntity`.                                                                                                                                                                                                 |

Sequence Diagram of eventual connectivity:

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/b93be6f2-d34f-4224-b59b-0cc3fc402fd6" />

App:


Connect it with internet:

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/f69408a7-4200-47fb-8a6b-41b150b6608d" />


Without internet connection

<img width="30%"  alt="image" src="https://github.com/user-attachments/assets/1d99d246-653b-41cb-bd23-507a1e535184" />


---

### Feature 5 — Your Go-To Tutor (Per-Course Personalized Offline Cache)
**Owner:** Maria Lucia Benavides Domínguez

| Field                      | Content                                                                                                                                                                                                                                                                                                                                                                 |
| -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Event description**      | Student opens a specific course detail and the app loads **Your Go-To Tutor** (`getGoToTutor(studentId, courseId)`).                                                                                                                                                                                                                                                  |
| **System response**        | **Online:** app requests personalized tutor from analytics (`getReturningTutor`), then upserts SQLite row (`cached_go_to_tutor`) keyed by `student_id + course_id`. **Offline / API failure:** app reads local cached row and serves it with `isFromCache = true` and `lastUpdated`. **UI:** section shows cached badge/time when data is not fresh from network. |
| **Possible anti-patterns** | Using global (non-scoped) cache keys that mix users/courses · Hiding whole section when request fails (no user feedback) · Not distinguishing cached vs fresh data in UI · Overwriting cache with null/error payloads.                                                                                                                                             |
| **Caching strategy**       | Context-keyed cache (`student_id`, `course_id`) with replace-on-success semantics. Cache serves as personalized offline fallback and is refreshed whenever remote call succeeds.                                                                                                                                                                                       |
| **Storage type**           | **SQLite (`sqflite`)** because composite-key lookups and deterministic overwrite behavior are required for per-user, per-course personalization.                                                                                                                                                                                                                       |
| **Stored data type**       | JSON object under `payload` with tutor fields (`id`, `name`, `rating`, `location`, `nextSlotStart`, `nextSlotEnd`) plus `last_updated` timestamp.                                                                                                                                                                                                                     |

Sequence Diagram of eventual connectivity:

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/1606f52e-1ecc-4ecb-8ea0-8e12617be10d" />

App:


Connect it with internet:

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/85ab7f5a-6dfa-45f2-b2be-4b7fdc96a7cc" />

Without internet connection

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/70c4867f-f19c-46b7-ae28-802635187aea" />

Reconnect to internet:

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/5565ea14-88cb-4032-ba8c-52f29d4638f7" />


---

### Feature 6 — Motion Alert (Offline-Safe Settings + Deferred Delivery)
**Owner:** Maria Lucia Benavides Domínguez

| Field                      | Content                                                                                                                                                                                                                                                                                                                                                                                                        |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Event description**      | Student enables movement monitoring and configures alert destination (`alertEmail`, `studentName`, `location`). Motion events may trigger emergency alert attempts while connectivity changes.                                                                                                                                                                                                                 |
| **System response**        | **Configuration path:** settings are persisted locally in `SharedPreferences` so monitoring state survives app restarts and can be loaded offline. **Trigger path:** motion coordinator attempts delivery through repository when possible; each attempt is logged to local file (success/failure). **Offline / network issue:** app keeps monitoring and records failed attempts locally, allowing traceability and retry logic on future valid triggers. |
| **Possible anti-patterns** | Keeping critical settings only in memory · Losing alert-attempt evidence when network fails · Blocking UI on sensor callbacks · Failing to clear sensitive local motion data on logout.                                                                                                                                                                                                                       |
| **Caching strategy**       | Persistent local configuration cache + append-only local event log. This guarantees offline continuity for monitoring setup and observability of delivery failures.                                                                                                                                                                                                                                            |
| **Storage type**           | **SharedPreferences** for current settings (small key-value state) + **private local file** for audit/event history.                                                                                                                                                                                                                                                                                          |
| **Stored data type**       | Scalars in `SharedPreferences` (`String`, `bool`) and newline/appended text records in local file (`timestamp`, `reason`, destination, location, result/error).                                                                                                                                                                                                                                               |

Sequence Diagram of eventual connectivity:

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/1a5a7c7e-9bd5-40b5-b54e-3a5f6a8d36d5" />

App:

Motion alert configuration form (values saved).

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/1edb1473-ed0b-4a02-9553-2fab69bb51e7" />

App behavior/notification when monitoring is active.

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/865bad78-94eb-4d6b-adcd-540b5b30d37a" />

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/d3f64e22-36fd-42d3-86e3-85f0748a9eda" />


Local event log entries for success and failure.

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/ffa4396f-a3b3-41e8-b77f-4d064161ec38" />


Logout flow clearing motion preferences/log for privacy.

<img width="30%" alt="image" src="https://github.com/user-attachments/assets/ff0894c5-ba87-4e54-8a74-cff868318938" />


### Kotlin

#### Feature 1 — Home Page with Offline Cache
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **Event description** | User opens the Home Page, which must display sessions, occupancy, and recommended subjects. The event occurs regardless of network availability. |
| **System response** | **Offline:** L1 memory cache is checked first; if cold, L2 SQLite (`cache_home`) is queried and stale data is displayed immediately — UI is never empty. **Online:** Fresh data is fetched from the API via `Dispatchers.IO`, stored in both L1 (in-memory `LinkedHashMap`) and L2 (SQLite with 5-minute TTL), then rendered on `Dispatchers.Main`. **API failure despite connection:** Stale cached data is served as a silent fallback without blocking the UI. **On app relaunch:** L1 is cold but L2 persists across restarts, guaranteeing data is always available. |
| **Possible anti-patterns** | Calling the API on the main thread (UI freeze) · Skipping the TTL check and serving indefinitely stale data · Not populating L2 after a successful API response · Relying solely on in-memory cache and losing all data on restart · Not switching to `Dispatchers.Main` before updating UI state. |
| **Caching strategy** | Two-level cache. **L1:** In-memory `LinkedHashMap` with LRU eviction (max 20 entries) — nanosecond access, ideal for UI recompositions. **L2:** SQLite `cache_home` table with a configurable 5-minute TTL. Read policy: L1 → L2 → API. If TTL expired: refresh from API when online, serve stale when offline. Cache is invalidated on every successful mutation. |
| **Storage type** | **SQLite** for the L2 persistent cache — lightweight, supports TTL-based validation, and survives app restarts. **DataStore** for user preferences. SQLite was preferred over Room here due to its simplicity for a single cached payload per section. |
| **Stored data type** | `json_data TEXT` (full API response serialized as a JSON string) + `timestamp INTEGER` stored in the `cache_home` table. Deserialized to domain data classes in the repository layer inside `Dispatchers.IO`. |

<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/home%20page%20eventual%20connectivity.png" />


---

#### Feature 2 — Availabilities with Offline Queue
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **Event description** | Tutor creates, edits, or deletes an availability slot. The action may occur with or without an active internet connection, and the result must eventually reach the backend. |
| **System response** | **Offline (Create):** Availability is stored in the local SQLite queue (`pending_availabilities`) with action type `CREATE` and a temporary `local-{timestamp}` ID. The new slot appears in the UI immediately (optimistic update) and an *"No internet connection. Changes will be saved later."* banner is shown. **Offline (Edit / Delete):** Same flow — queued as `UPDATE` or `DELETE`, UI reflects the change instantly. **Online:** API call is made directly; the queue is bypassed entirely. **On reconnect:** `ConnectivityManager.NetworkCallback` triggers `syncPendingActions()`, which processes the queue in FIFO order, replaces temporary IDs with real server IDs, removes synced records, and displays a *"Changes synchronized successfully."* banner. |
| **Possible anti-patterns** | Blocking the UI while waiting for the API response · Not persisting the queue to SQLite (operations lost on restart) · Processing the queue concurrently without a `Job` lock (race conditions and duplicate requests) · Not replacing `local-{timestamp}` IDs with real server IDs after sync · Overwriting newer server state with stale offline mutations · Triggering a new sync while a previous one is still running. |
| **Caching strategy** | Read cache for the availability list, invalidated on every successful mutation. Optimistic write cache: mutations are applied locally before server confirmation, keeping UI response under 100ms regardless of network latency. Temporary IDs (`local-{timestamp}`) are replaced with real server IDs after synchronization completes. A `Job` is used as a mutex to prevent concurrent sync executions. |
| **Storage type** | **SQLite** for the offline mutations queue (`pending_availabilities` table) — persistent across app restarts, FIFO-processable, and reliable for deferred synchronization. Chosen over in-memory structures because pending operations must survive process death. |
| **Stored data type** | `availability_json TEXT` (full availability object serialized as JSON), `action_type TEXT` (CREATE / UPDATE / DELETE), `availability_id TEXT`, `created_at INTEGER` (Unix timestamp). |


<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/availability%20eventual%20connectivity.png" />

##### offline message

<img width="25%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/message%20offline%20availability.png" />

##### offline message when we create an availability

<img width="25%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/create%20availability.png" />

##### offline message when we edit an availability

<img width="25%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/edit%20availability.png" />

##### offline message when we delete an availability

<img width="25%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/delite%20availability.png" />

---

#### Feature 3 — Course Management & Application Queue
**Owner:** Daniel Camilo Quimbay Velásquez

| Field | Content |
|-------|---------|
| **Event description** | User browses the course catalog and taps "Apply" to become a tutor for a specific subject. |
| **System response** | **Online:** The app performs a `POST` request to the backend. On success, the UI updates and the local SQLite cache is refreshed. **Offline:** The application is intercepted and stored in the `pending_applications` SQLite table with a `sync_status = 'pending'` flag. The UI performs an **Optimistic Update**, showing the application as "Pending Sync" to the user. **On Reconnect:** The `ConnectivityManager.NetworkCallback` triggers a background synchronization worker that iterates through the SQLite queue, sends the pending POST requests in FIFO order, and updates the local record to `synced = true` upon backend confirmation. |
| **Possible anti-patterns** | Losing pending applications if the app is closed (solved by using SQLite persistence instead of RAM) · Creating duplicate applications if the sync button is pressed rapidly · Not notifying the user that their application is "waiting for internet" · Blocking the UI while the background sync is running. |
| **Caching strategy** | **Write-Behind Caching:** Applications are "written" to the local database first. This ensures 0ms latency for the user. The background synchronization logic ensures the local state and server state eventually reach consistency (Eventual Consistency). |
| **Storage type** | **SQLite (Relational)**. This is required because each pending application needs metadata (Course ID, Timestamp, Sync Status) that must survive a device reboot. |
| **Stored data type** | **Relational Table:** `course_id (TEXT)`, `application_data (TEXT/JSON)`, `sync_status (BOOLEAN)`, `created_at (INTEGER)`. |

<img width="35%" alt="Feature3EvCKmilo" src="https://github.com/user-attachments/assets/ffe4bc1c-972c-47cc-bd10-38ac35bcc38f" />

Courses feature when disable wifi connection:

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/76d602df-7e08-48f6-a002-8e77f16fbce2" />

Courses feature after applying to a course without internet connection:

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/365044d6-e11f-4799-9b5a-8921b47e7253" />

Courses feature after syncing when internet connection is re-stablished:

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/91bb742f-701f-4f0a-9a1a-d0299c9c7709" />


---

#### Feature 4 — Profile View with Triple-Layer Cache
**Owner:** Daniel Camilo Quimbay Velásquez

| Field | Content |
|-------|---------|
| **Event description** | User navigates to the Profile screen to view their tutor identity and profile picture. |
| **System response** | **L1 (Memory):** The app first checks a manual **LruCache**; if a hit occurs, the bitmap is rendered instantly. **L2 (Disk/Cache):** If L1 is cold, it checks `context.cacheDir` for `profile_temp.jpg`. If found, it's decoded, added back to L1, and displayed. **L3 (Network):** If both caches are cold and `isNetworkAvailable()` is true, the image is downloaded, saved to the Disk Cache, and stored in the LruCache. **Offline:** If the image is not in L1 or L2, a default placeholder/initials avatar is shown to prevent a broken UI. |
| **Possible anti-patterns** | Using a fixed cache size that ignores device RAM limits (causing OOM errors) · Downloading the profile picture on every view appearance · Not using `Dispatchers.IO` for file system or network streaming · Storing the high-res bitmap in SharedPreferences (it should only store the URL). |
| **Caching strategy** | **LRU + Disk Persistence:** Uses a manual `LruCache` set to 1/8th of the available app memory for the "Active" session. The Disk cache (`cacheDir`) ensures the image survives app restarts. This "Triple-Check" flow (Memory → Disk → Network) minimizes data usage and maximizes UI snappiness. |
| **Storage type** | **LruCache (Memory)** for high-speed access + **Internal App-Specific Storage (Disk)** for persistence. This satisfies the requirement for "Manual Cache Structures" and "App-Specific Files." |
| **Stored data type** | `Bitmap` (in Memory) and `ByteArray/File` (on Disk). Binary data is handled via `InputStream.copyTo(FileOutputStream)` to ensure memory efficiency during the write process. |

<img width="35%" alt="Feature4EvCKmilo" src="https://github.com/user-attachments/assets/20baf022-ee11-4494-b5a8-ba2769b48393" />


Profile feature without connection:

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/1116993e-641f-434c-a227-d09abc870bbf" />

As shown, the profile image is displayed even without connection if its saved on the device in some of the 3 options of storage.

---

## 5. Local Storage Strategies

### Flutter

#### Local Storage Strategy
**Owner:** Maria Lucia Benavides Domínguez

The app uses three complementary local-storage strategies in the features of your go to tutor, alert motion and Upcomming sessions. Each one was chosen based on data shape, query needs, and offline behavior.

| Technology                    | Feature / data stored                                                                                                                      | Why chosen                                                                                                                                                                                     | Access pattern                                                                                                               | Persistence / invalidation                                                                 |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| **SharedPreferences**        | Motion alert configuration: `alertEmail`, `studentName`, `location`, `isEnabled`                                                          | Small, low-cardinality user settings. Fast key-value reads/writes with minimal overhead; no relational queries required.                                                                     | `load()` / `save()` / `clear()` in `motion_alert_preferences.dart` using scalar keys.                                      | Persists across app restarts. Cleared on logout for privacy.                              |
| **SQLite (`sqflite`)**       | Offline fallback cache for **Upcoming Sessions** and **Your Go-To Tutor**                                                                  | Structured cache entries keyed by user context (`student_id`, `course_id`) with deterministic lookup. Supports remote-first + local-fallback behavior when network/API calls fail.          | `upsert()` on successful remote response, `queryOne()` on failure. Tables: `cache_upcoming_sessions`, `cached_go_to_tutor`. | Persists until replaced by newer remote data or cleared on logout (`clearAll()`).         |
| **Private local files**      | Motion alert event history log (timestamp, reason, destination email, location, success/failure, error)                                   | Append-only audit trail is simpler and cheaper as a file than as relational rows. No complex filtering or joins required.                                                                    | Append event on each alert attempt; clear file on logout.                                                                    | Persists locally for history/audit until explicit cleanup.                                |

#### Upcoming Sessions strategy (clear flow)

- **Remote-first:** `getUpcomingSessions(studentId)` requests the full list from backend via `_sessions.getStudentSessions(studentId)`.
- **Derivation before persistence:** repository filters `startDateTime > now` and sorts by start date.
- **SQLite write shape:** stores one row in `cache_upcoming_sessions` (`AppDatabaseService.tableUpcomingSessions`) with `student_id`, `payload` (JSON), and `last_updated` (epoch ms).
- **Offline fallback:** if remote fails, reads by `student_id`, decodes `payload`, and returns `CachedResult(..., isFromCache: true)`. If row is missing (first offline run), it rethrows.
- **Code location:** `lib/features/home/data/repositories/student_tutoring_repository_impl.dart` → `getUpcomingSessions()`, `_safeUpsert()`, `_readUpcomingSessionsCache()`.

#### Privacy decision

Because motion-alert data is sensitive (email, location, timestamps), logout cleanup always:

- Stops active monitoring
- Clears motion settings from SharedPreferences
- Deletes local alert-history file

This prevents previous-user sensitive data from leaking into a new session.

---
#### Local Storage Strategy
**Owner:** Paola Catherine Jimenez Jaque

| Technology | Feature | Why chosen | Access pattern | Persistence |
|------------|---------|------------|----------------|-------------|
| **Drift (SQLite)** | Pending session booking queue | Structured relational data with multiple typed fields. Needs querying by `synced = false` — impossible with key-value stores without loading everything into memory. | `SELECT WHERE synced=false` · `UPDATE synced=true` | Until explicitly confirmed by server |
| **Hive** | Tutor availability cache | Document-style data (list of tutors per course) with no relationships. Only needs lookup by `courseId` and full replacement on refresh. No schema or migrations needed. | `box.get(courseId)` · `box.put(courseId, json)` | 30-minute TTL, invalidated after booking |
| **SharedPreferences** | Profile cache + offline edit queue | Single small JSON values keyed by `userId`. Profile is at most a few hundred bytes. Using a DB here would be over-engineering — native key-value is O(1) with zero setup. | `getString(key)` · `setString(key, json)` | Until sync confirmed or overwritten |

<img width="100%" alt="image" src="https://github.com/user-attachments/assets/562c3ecf-87d6-4bf6-82e0-8bdb5b4c0d1d" />

### Kotlin

#### Feature: Home Page
**Owner:** Nikol Katherin Rodriguez Ortiz

##### In-Memory Cache — `InMemoryCache.kt`
| Field | Content |
|:---|:---|
| **Purpose** | L1 cache layer — fastest data access for UI recompositions, checked before any I/O operation. |
| **Implementation** | `LinkedHashMap(maxSize, 0.75f, accessOrder = true)` with LRU eviction. Entries wrapped in `CacheEntry(value: Any, timestamp: Long)`. |
| **Max Size** | 20 entries (default). Oldest accessed entry evicted automatically when limit is reached. |
| **Thread Safety** | All public methods (`put`, `get`, `clear`) annotated with `@Synchronized`. |
| **Cache Keys** | `${CacheDatabase.KEY_SESSIONS}_$tutorId` · `${CacheDatabase.KEY_OCCUPANCY}_$tutorId` · `${CacheDatabase.KEY_SUBJECTS}` |
| **Key Methods** | `put(key: String, value: Any)` (Line 52) · `get(key: String): CacheEntry?` (Line 61) · `clear()` (Line 65) |
| **Location** | `app/src/main/java/com/calico/tutor/data/cache/InMemoryCache.kt` (Lines 52–70) |


##### Local Relational DB — `CacheDatabase.kt`
| Field | Content |
|:---|:---|
| **Database** | `calico_cache.db` (Version 2) |
| **Purpose** | L2 persistent cache for Home Page sections. Survives app restarts and serves as fallback when offline or API fails. |
| **Key Table** | `cache_home` — columns: `id TEXT PRIMARY KEY`, `json_data TEXT`, `timestamp INTEGER`. |
| **TTL Policy** | `isFresh = now - timestamp < expiryMs` — default 5 minutes, configurable via `UserPreferencesDataStore`. |
| **Fallback behavior** | If API fails or device is offline, stale `json_data` is served without invalidating the row. |
| **Key Methods** | `saveCache(key, json)` (Line 71) · `getCache(key): Pair<String?, Long>` (Line 83) |
| **Location** | `app/src/main/java/com/calico/tutor/data/local/CacheDatabase.kt` (Lines 46–92) |

##### User Preferences — `UserPreferencesDataStore.kt`
| Field | Content |
|:---|:---|
| **Purpose** | Reactive storage for cache configuration. TTL expiry used by both Home Page and Availabilities cache layers. |
| **Mechanism** | Jetpack DataStore (Flow-based) — reads are non-blocking and observable by the ViewModel. |
| **Stored Keys** | `cache_expiry_ms` (default: 300 000 ms) · `last_sync_time` · `notifications_enabled` |
| **Key Methods** | `setCacheExpiryMs(expiryMs: Long)` (Line 58) · `updateLastSyncTime()` (Line 62) |
| **Location** | `app/src/main/java/com/calico/tutor/data/local/UserPreferencesDataStore.kt` (Lines 32–68) |


<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/Home%20Page%20Local%20Storage.png" />

---

#### Feature: Availabilities
**Owner:** Nikol Katherin Rodriguez Ortiz

##### In-Memory Cache — `InMemoryCache.kt`
| Field | Content |
|:---|:---|
| **Purpose** | L1 cache for the availability list. Holds the current loaded state and optimistic writes. Checked before any SQLite or API call. |
| **Cache Key** | `${CacheDatabase.KEY_AVAILABILITIES}_$tutorId` |
| **Optimistic writes** | Local mutations (`applyLocalCreate`, `applyLocalUpdate`, `applyLocalDelete`) write directly to the in-memory list with `local-{timestamp}` IDs before any network call. |
| **Invalidation** | Cleared via `invalidateAndReload()` (Line 612) after every successful mutation or completed sync. |
| **Key Methods** | `put(key, value)` (Line 52) · `get(key): CacheEntry?` (Line 61) · `clear()` (Line 65) |
| **Location** | `app/src/main/java/com/calico/tutor/data/cache/InMemoryCache.kt` (Lines 52–70) · `AvailabilityViewModel.kt` → `applyLocalCreate()` (Line 498), `applyLocalUpdate()` (Line 516), `applyLocalDelete()` (Line 532) |

##### Local Relational DB — `CacheDatabase.kt`
| Field | Content |
|:---|:---|
| **Database** | `calico_cache.db` (Version 2) |
| **Purpose** | Persistent offline mutations queue. Stores every CREATE / UPDATE / DELETE that could not reach the backend. Survives app restarts and process death. |
| **Key Table** | `pending_availabilities` — columns: `id` (AUTOINCREMENT), `availability_json`, `action_type`, `availability_id`, `created_at`. |
| **Processing order** | FIFO — rows read `ORDER BY created_at ASC` to preserve operation sequence. |
| **Temporary IDs** | Offline entries use `local-{timestamp}` as `availability_id`, replaced with real server ID after successful sync. |
| **Key Methods** | `savePending(json, actionType, availabilityId)` (Line 95) · `getAllPending(): List<PendingItem>` (Line 110) · `deletePending(id: Long)` (Line 132) · `getPendingCount(): Int` (Line 143) |
| **Location** | `app/src/main/java/com/calico/tutor/data/local/CacheDatabase.kt` (Lines 52–145) |

##### Background Sync Worker — `PendingAvailabilitiesWorker.kt`
| Field | Content |
|:---|:---|
| **Purpose** | Fallback sync mechanism. Processes the entire `pending_availabilities` queue even if the app is in the background or was restarted before sync completed. |
| **Mechanism** | `CoroutineWorker` scheduled via WorkManager. Reads all pending rows, calls the appropriate API method per `action_type`, deletes each row on success, and logs results via `FileManager`. |
| **Schedule** | Every 15 minutes · Network requirement: `NetworkType.CONNECTED` · Backoff: LINEAR, 5-minute initial delay. |
| **Work name** | `"sync_pending_availabilities"` — prevents duplicate workers from running concurrently. |
| **Key Methods** | `doWork(): Result` (Line 35) · `schedulePeriodicWork(context)` (Line 113) |
| **Location** | `app/src/main/java/com/calico/tutor/data/worker/PendingAvailabilitiesWorker.kt` (Lines 35–130) |


<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/vailabilities%20Local%20Storage.png" />

---
#### Feature: Course & Application Management
**Owner:** Daniel Camilo Quimbay Velásquez

##### Local Relational DB — `DatabaseHelper.kt`
| Field | Content |
|:---|:---|
| **Database** | `app_database.db` (Version 5) |
| **Purpose** | Primary storage for courses, applications, and tutor profiles. |
| **Key Tables** | `courses`, `approved_courses`, `applications`, `pending_applications` (Offline Sync Queue). |
| **Thread Safety** | Implements `SQLiteDatabase` transactions (`beginTransaction()`, `setTransactionSuccessful()`) to ensure atomicity. |
| **Key Methods** | `saveCourses()`, `saveApplications()`, `savePendingApplication()`. |
| **Location** | `app/src/main/java/com/calico/tutor/ui/screen/DatabaseHelper.kt` (Lines 10-397) |

##### SharedPreferences — `TokenManager.kt`
| Field | Content |
|:---|:---|
| **Purpose** | Secure storage for authentication tokens and user email. |
| **Mechanism** | **EncryptedSharedPreferences** using AES-256 GCM (Master Key) and AES-256 SIV (Keys). |
| **Stored Data** | `KEY_ID_TOKEN`, `KEY_REFRESH_TOKEN`, `KEY_EMAIL`. |
| **Key Logic** | `isTokenValid()` and `isTokenExpiringSoon()` to manage session longevity. |
| **Location** | `app/src/main/java/com/calico/tutor/data/datasource/local/TokenManager.kt` (Lines 8-130) |

<img width="50%" alt="LocalStorageCourses" src="https://github.com/user-attachments/assets/f5c73c1f-20ab-4907-8ad3-f7b0a5144198" />

---

#### Feature: Profile Identity
**Owner:** Daniel Camilo Quimbay Velásquez

##### Local Relational DB — `DatabaseHelper.kt`
| Field | Content |
|:---|:---|
| **Database** | `app_database.db` (Version 5) |
| **Purpose** | Persistent caching of the tutor's identity to avoid redundant API calls on startup. |
| **Key Table** | `tutor_profile` (Stores: `name`, `email`, `subject`, `profile_image_url`). |
| **Mechanism** | Query results are mapped to the `TutorProfile` data class for immediate UI injection. |
| **Key Methods** | `saveTutorProfile(tutor)`, `getTutorProfiles()`. |
| **Location** | `app/src/main/java/com/calico/tutor/ui/screen/DatabaseHelper.kt` (Lines 220-259) |

##### App-Specific Files — `FileManager.kt`
| Field | Content |
|:---|:---|
| **Purpose** | Persistence of internal audit logs for profile updates and backup of session data. |
| **Mechanism** | Internal File API (`context.filesDir`). |
| **Cleanup** | `cleanOldBackups()` ensures only the 5 most recent JSON backups are stored. |
| **Format** | `logs.txt` uses `appendText` to record errors and profile sync success timestamped with `SimpleDateFormat`. |
| **Location** | `app/src/main/java/com/calico/tutor/data/local/FileManager.kt` (Lines 22-68) |

##### User Preferences — `UserPreferencesDataStore.kt`
| Field | Content |
|:---|:---|
| **Purpose** | Reactive storage for profile-related settings like notifications and sync intervals. |
| **Stored Keys** | `cache_expiry_ms`, `last_sync_time`, `notifications_enabled`. |
| **Mechanism** | Jetpack DataStore (Flow-based observability for reactive UI updates). |
| **Key Methods** | `updateLastSyncTime()`, `setNotificationsEnabled()`. |
| **Location** | `app/src/main/java/com/calico/tutor/data/local/UserPreferencesDataStore.kt` (Lines 32-69) |

<img width="50%" alt="LocalStorageProfile" src="https://github.com/user-attachments/assets/c0fbdcc7-57e0-4c2d-bd13-1fc4ded28645" />


---

## 6. Multithreading Strategies

### Flutter

#### Strategy 1 — Future Strategy (Request-Response Asynchrony) - Your Go-To Tutor and Available in the next 4 hours
**Owner:** Maria Lucia Benavides Domínguez

| Field                          | Content                                                                                                                                                                                                                                                                                                                                                   |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Mechanism**                  | `Future<T>` via `async/await` for one-shot, request-response operations                                                                                                                                                                                                                                                                                    |
| **Problem it solves**          | Avoids blocking the UI while waiting for **network + storage I/O** (API calls, SQLite/Hive reads/writes)                                                                                                                                                                                                                                                   |
| **Where it was used**          | `StudentTutoringRepositoryImpl.getAvailableTutorsNext4Hours(...)` · `StudentTutoringRepositoryImpl.getGoToTutor(...)` · `StudentTutoringRepositoryImpl.getUpcomingSessions(...)`                                                                                                                                                                         |
| **How it works**               | Each method `await`s the remote call, then performs a **best-effort cache write**. If the remote fails, it attempts to **read from cache** and returns that instead. Example patterns: `try { await remote; await cacheWrite; } catch { await cacheRead; rethrow if no cache }`.                                                                    |
| **Why this over alternatives** | A `Stream` is unnecessary because these are **single responses**, not continuous events. An `Isolate/compute()` is not appropriate for the **I/O-bound** portion—Dart async I/O already yields control back to the event loop while waiting, so spawning a worker isolate would add overhead with little benefit.                                         |
| **Threading model**            | Runs on the **main isolate**, but remains responsive because the `await` points are non-blocking. The UI thread is not “busy”; it is simply awaiting responses while still rendering frames and processing gestures.                                                                                                                                         |
| **Error handling**             | Remote failure → fallback to cache when available (`_tutorsCache.read(...)`, `_readGoToTutorCache(...)`, `_readUpcomingSessionsCache(...)`). Cache writes are wrapped in `_safeUpsert(...)` and intentionally **swallowed** on failure to keep the remote-success path from breaking.                                                                   |
| **Code location**              | `lib/features/home/data/repositories/student_tutoring_repository_impl.dart` → `getAvailableTutorsNext4Hours()`, `getGoToTutor()`, `getUpcomingSessions()`, `_safeUpsert()`, `_readUpcomingSessionsCache()`                                                                                                                                              |

#### Strategy 2 — Stream Strategy (Continuous Reactive Processing) - Movement detection
**Owner:** Maria Lucia Benavides Domínguez

| Field                          | Content                                                                                                                                                                                                                                                                                                                                                                        |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Mechanism**                  | `StreamSubscription<AccelerometerEvent>` via `accelerometerEventStream().listen(...)`                                                                                                                                                                                                                                                                                           |
| **Problem it solves**          | Motion sensors generate a **continuous event source**; the app needs to react to each event, apply rules over time windows, and trigger an action when conditions are met (instead of polling).                                                                                                                                                                                |
| **How it works**               | For each accelerometer event, compute magnitude \( \sqrt{x^2+y^2+z^2} \). If magnitude ≥ `threshold`, apply a **debounce** (ignore hits within 700ms), keep a rolling list of hit timestamps inside `window` (default 30s), and trigger when hits ≥ `minHitsInWindow` (default 5) while enforcing a `cooldown` (default 1 min). |
| **Why this over alternatives** | A `Future` is one-shot and cannot represent a continuous sensor feed. An `Isolate/compute()` is unnecessary because each event’s math is lightweight; the key is **reactivity**, not heavy CPU work.                                                                                                                                                                            |
| **Threading model**            | Runs on the **main isolate** (stream callbacks execute there). The handler is small, so it is safe to update UI-facing `ValueNotifier`s and call the trigger callback without noticeable jank.                                                                                                                                                                                 |
| **Error handling**             | Lifecycle safety via `stop()`/`dispose()` cancelling the subscription and clearing state. Cooldown + debounce reduce accidental repeated triggers. Trigger callback is awaited so the alert pipeline is serialized per-hit sequence.                                                                                                                                              |
| **Code location**              | `lib/core/services/motion_alert_service.dart` → `start()`, `stop()`, `dispose()` · `lib/core/services/motion_alert_coordinator.dart` → `_applySettings()`, `_onTriggered()`                                                                                                                                                                                                     |

#### Strategy 3 — Isolate Strategy (Explicit Multithreading in Flutter) - Upcoming Sessions
**Owner:** Maria Lucia Benavides Domínguez

| Field                          | Content                                                                                                                                                                                                                                                                                                                                                                   |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Mechanism**                  | **Not used in this implementation** for these features (no `Isolate.spawn(...)` / `compute()` in the upcoming-sessions pipeline).                                                                                                                                                                                                                                         |
| **Why it wasn’t necessary**    | In `getUpcomingSessions(...)`, the filtering and sorting are small list operations done after awaiting I/O. For the expected dataset size, this is fast enough on the **main isolate** and keeps the implementation simpler.                                                                                                                                               |
| **When an Isolate would help** | If the app must process **large lists** (e.g., thousands of sessions) or run expensive computations per item (parsing, heavy transformations) frequently (e.g., every keystroke), then moving that CPU work to a **worker isolate** via `compute()` is appropriate to prevent frame drops.                                                                                 |
| **Constraints (important)**    | Worker isolates have **no shared memory**; data must be message-passed (serializable), and the isolate entry function must be top-level/static (no captured widget state).                                                                                                                                                                                                |
| **Suggested next step (if needed)** | If upcoming sessions ever becomes a performance bottleneck, extract “filter + sort upcoming sessions” into a pure function and call it with `compute(...)` (same pattern as the course-search isolate strategy later in this section).                                                                                                                                 |
| **Code location**              | Current upcoming-sessions logic: `lib/features/home/data/repositories/student_tutoring_repository_impl.dart` → `getUpcomingSessions()`                                                                                                                                                                                   

### Strategy 4 — `Future.wait` with `.then()` / `.catchError()` - Pending sessions
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **How it works** | `initState` calls `_controller.markLoading()` then fires `Future.wait([loadCourses, loadSessions, loadPendingSessions], eagerError: false)`. All three I/O waits overlap on the event loop simultaneously. `eagerError: false` ensures all futures run to completion even if one fails, partial data (e.g. courses without sessions) is still rendered. `.then((_) { _controller.markSuccess(); setState(() {}); })` transitions to success state. `.catchError((e) { _controller.markFailure(e.toString()); setState(() {}); })` transitions to failure state and shows a retry button. |
| **Why this over alternatives** | A `Stream` would be wrong here, there is no continuous event source, just three one-shot HTTP calls. An `Isolate/compute()` would be wrong too — these are I/O-bound operations, not CPU-bound; spawning a worker isolate for network calls adds overhead with no benefit since I/O already releases the thread. `Future.wait` is the correct tool for concurrent I/O. |
| **Threading model** | Runs entirely on the **main isolate**. Dart's async I/O is non-blocking — while waiting for HTTP responses, the event loop is free to process frames and gestures. No UI jank occurs because the thread is never blocked, only waiting. |
| **Error handling** | `eagerError: false` prevents one failing future from cancelling the others. `.catchError()` catches any unhandled exception and calls `_controller.markFailure(message)` which surfaces a retry button. Each individual future (`loadCourses`, `loadSessions`) has its own `try/catch` so partial failures degrade gracefully. |
| **Code location** | `lib/features/home/presentation/screens/home_screen.dart` → `initState` · `lib/features/home/presentation/controllers/home_controller.dart` → `loadData()`, `markLoading()`, `markSuccess()`, `markFailure()` |

---

### Strategy 5 —`StreamSubscription` on Connectivity Changes - Home page
**Owner:** Paola Catherine Jimenez Jaque


| Field | Content |
|-------|---------|
| **Mechanism** | `StreamSubscription<List<ConnectivityResult>>`, reactive event stream on the main isolate |
| **Problem it solves** | Without a Stream, the only way to detect connectivity changes would be polling (`Timer.periodic`), wasting battery and CPU. A `Future` cannot model a continuous event source. A `Stream` emits exactly when the network state changes and nothing more. |
| **How it works** | `Connectivity().onConnectivityChanged` emits a `List<ConnectivityResult>` on every network change. The subscription is stored as `_connectivitySubscription` field. On each emission: if all results are `ConnectivityResult.none` → `_isOffline = true` → `setState` renders the red offline banner. On reconnect: triggers `SyncService.syncPendingSessions` → `loadPendingSessions` → `loadSessions` → `setState` so synced bookings move from *"Pending Sync"* to *"Upcoming Sessions"*. Also triggers `ProfileRepositoryImpl.syncPendingUpdate` to flush pending profile edits. Same pattern used in `profile_screen.dart`, `_connectivitySub` calls `_controller.syncAndReload()` on reconnect. |
| **Why this over alternatives** | A `Future` is one-shot, it cannot model repeated connectivity events. `compute()` is for CPU-bound work, not event listening. A `Stream` is the only correct abstraction for a continuous, push-based event source like network status. |
| **Threading model** | Runs on the **main isolate**. The platform sends connectivity events via a platform channel; the stream callback executes on the main thread so `setState` is safe to call directly. |
| **Error handling** | `_connectivitySubscription` is always cancelled in `dispose()`, forgetting to cancel keeps the callback alive after the widget is destroyed and causes `setState-after-dispose` crashes. Sync failures inside `syncPendingSessions` are swallowed per-row so one bad booking does not block the rest. |
| **Code location** | `lib/features/home/presentation/screens/home_screen.dart` → `_connectivitySubscription`, `_onConnectivityChanged()` · `lib/features/profile/presentation/screens/profile_screen.dart` → `_connectivitySub` |

---

### Strategy 6 — Isolate via `compute()` for Course Filtering - Filtering courses
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **Mechanism** | `compute()`, spawns a real Dart **worker isolate** off the main thread |
| **Problem it solves** | Without this, every keystroke in the search bar triggers string matching across the full course list on the main isolate — dropping frames and making the UI feel janky. This is a CPU-bound operation that genuinely benefits from a separate thread. |
| **How it works** | `filterCoursesInIsolate(courses, query)` calls `compute(_filterCourses, FilterParams(courses, query))`. `compute()` spawns a worker isolate, copies `FilterParams` by value via message passing (no shared memory), runs `_filterCourses` off the main thread, and returns the result as a `Future`. A `_lastSearchQuery` guard in `search(query)` discards stale results — if the user typed faster than the isolate returned, only the result matching the most recent query updates the UI, preventing out-of-order renders. |
| **Why this over alternatives** | A `Future` runs on the main isolate, it would still block the UI thread during heavy computation. A `Stream` is for continuous events, not one-shot computation. `compute()` is the correct tool for CPU-bound work that must not block the UI thread. |
| **Threading model** | Runs on a **worker isolate**, a completely separate Dart thread with no shared memory. `FilterParams` must be a plain serializable class (no functions, no Streams, no `BuildContext`). `_filterCourses` must be a top-level function because isolates cannot capture instance state references. |
| **Error handling** | `compute()` wraps the isolate result in a `Future`, any exception thrown inside `_filterCourses` is propagated as a failed Future and caught in `search()` with a `try/catch`. The `_lastSearchQuery` guard also prevents stale results from overwriting a newer query result. Tests require `TestWidgetsFlutterBinding.ensureInitialized()` because `compute()` needs the Flutter engine's isolate infrastructure even in test environments. |
| **Code location** | `lib/core/utils/course_filter_isolate.dart` → `filterCoursesInIsolate()`, `_filterCourses()`, `FilterParams` · `lib/features/home/presentation/controllers/home_controller.dart` → `search()` |

---

### Kotlin

### Home Page

#### Strategy 1 — `viewModelScope.launch` with `withContext(Dispatchers.IO)`
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | When the Home screen initializes, `viewModelScope.launch` starts a coroutine on the main thread. Inside, `withContext(Dispatchers.IO)` suspends execution and resumes it on the IO dispatcher thread pool to perform all I/O work in sequence: L1 memory cache check → L2 SQLite query (`cache_home`) → API call if needed. After `withContext` completes, control automatically returns to `Dispatchers.Main`, where `_uiState.value = data` is assigned safely. The full L1 → L2 → API cascade runs sequentially inside a single coroutine — each layer is only queried if the previous one misses or is expired. |
| **Why this over alternatives** | `Thread`/`AsyncTask` (deprecated) require manual `runOnUiThread` marshaling. `RxJava` adds heavy dependency overhead. `Handler`/`Looper` is verbose boilerplate. Kotlin Coroutines with `Dispatchers.IO` is the idiomatic Android solution: structured concurrency, automatic ViewModel-scoped cancellation, and sequential readable code with no callback nesting. Unlike Flutter's `Future`, `withContext` switches the actual execution context — the coroutine is truly moved to the IO thread pool, not just queued on an event loop. |
| **Threading model** | Coroutine starts on **`Dispatchers.Main`** (main thread via `viewModelScope`), suspends into **`Dispatchers.IO`** (shared thread pool, up to 64 threads) for all cache and network I/O, then automatically returns to **`Dispatchers.Main`** for the UI state update. No thread is ever blocked — suspension releases the thread to do other work while waiting. |
| **Error handling** | `try/catch` wraps the entire `withContext` block. Any exception from cache reads or the API call is caught, and the ViewModel falls back to serving stale L2 data. Error state is exposed via `StateFlow<HomeUiState>` so the UI reacts declaratively. The coroutine is bound to `viewModelScope` and cancelled automatically if the user navigates away before the load completes. |
| **Code location** | `features/home/presentation/HomeViewModel.kt` → `loadHomeData()` · `features/home/data/HomeRepositoryImpl.kt` → `getData()` · `features/home/data/local/HomeCacheDao.kt` → `getCachedData()`, `saveData()` |

---

#### Strategy 2 —  `MutableStateFlow` as a thread-safe UI state bridge
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | The ViewModel declares `private val _uiState = MutableStateFlow<HomeUiState>(Loading)` and exposes `val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()`. After any coroutine completes work on `Dispatchers.IO` and returns to `Dispatchers.Main`, it assigns `_uiState.value = Success(data)` or `_uiState.value = Error(message)`. The Compose UI collects with `val state by viewModel.uiState.collectAsState()`, which triggers recomposition only on the parts of the tree that depend on the changed state. This entirely replaces the need for `LiveData`, `Handler`, or manual `runOnUiThread` calls. |
| **Why this over alternatives** | `LiveData` requires lifecycle-aware observers and does not integrate as cleanly with Compose `collectAsState()`. `mutableStateOf` is UI-layer only and cannot be safely written from a background thread without an explicit dispatcher hop. `StateFlow` is coroutine-native, thread-safe by design, and the correct choice for ViewModel → Compose UI communication in a coroutine-first architecture. It also supports multiple collectors and replays the last value to new subscribers. |
| **Threading model** | `_uiState.value` is always assigned on **`Dispatchers.Main`** after `withContext(Dispatchers.IO)` returns. `StateFlow` is internally thread-safe, but assigning on the main thread ensures Compose recomposition is triggered without needing `Dispatchers.Main.immediate` workarounds. Collectors on the UI side always receive emissions on the main thread via `collectAsState()`. |
| **Error handling** | `HomeUiState` is a sealed class with `Loading`, `Success(data)`, and `Error(message)` subclasses. The ViewModel always emits `Loading` first, then transitions to `Success` or `Error`. This prevents the UI from ever rendering incomplete data and provides a clear state machine that the composable switches on exhaustively. |
| **Code location** | `features/home/presentation/HomeViewModel.kt` → `_uiState`, `uiState` · `features/home/presentation/HomeScreen.kt` → `val state by viewModel.uiState.collectAsState()` |


<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/Multithreading%20Strategies%20%20home%20page.png" />

---

### Availabilities

#### Strategy 1 — Fire-and-forget coroutine for optimistic mutations
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | When the user creates, edits, or deletes a slot, `viewModelScope.launch` fires immediately. The first operation inside the coroutine is a synchronous UI state update (optimistic write) on `Dispatchers.Main` — the change appears in the list instantly. Then `withContext(Dispatchers.IO)` runs the actual persistence: if offline, the operation is enqueued to `pending_availabilities` (SQLite) with a `local-{timestamp}` ID; if online, the API call is made directly. The caller (the UI) does not `await` the result — it returns as soon as the optimistic update is applied. Total perceived latency is under 100ms regardless of connectivity. |
| **Why this over alternatives** | A blocking call on the main thread causes ANR for slow networks. `AsyncTask` is deprecated since API 30. A plain `Thread` requires manual UI marshaling via `runOnUiThread`. The fire-and-forget coroutine pattern — optimistic update first, I/O second — is the correct model for mutation-heavy features where perceived responsiveness is more important than strict consistency. Unlike Flutter's `Future`, a Kotlin coroutine is scoped to the ViewModel and cancelled automatically on lifecycle end. |
| **Threading model** | The optimistic UI update runs synchronously on **`Dispatchers.Main`** before any suspension point. The SQLite write / API call runs on **`Dispatchers.IO`** inside `withContext`. The caller returns immediately after the optimistic update — the `launch` block is detached from the call site. |
| **Error handling** | If the DB write or API call fails inside `withContext(Dispatchers.IO)`, the coroutine catches the exception, reverts the optimistic UI update (removes the slot from the list), and shows an error snackbar. If the SQLite queue write itself fails, the user is notified that the action could not be saved and must retry manually. |
| **Code location** | `features/availability/presentation/AvailabilityViewModel.kt` → `createAvailability()`, `editAvailability()`, `deleteAvailability()` · `features/availability/data/AvailabilityRepositoryImpl.kt` → `saveLocally()`, `callApi()` |

---

#### Strategy 2 — `Job` reference as a coroutine mutex for sync control
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | The ViewModel holds `private var syncJob: Job? = null`. When `syncPendingActions()` is called, the first line checks `if (syncJob?.isActive == true) return` — if a sync coroutine is already running, the new call is silently discarded. Otherwise, `syncJob = viewModelScope.launch { ... }` starts a new sync coroutine and stores its reference. The coroutine reads all rows from `pending_availabilities` in FIFO order via `Dispatchers.IO`, sends each to the API, replaces `local-{timestamp}` IDs with real server IDs, and deletes the synced rows. When finished, `syncJob` becomes inactive and the next trigger can start a new sync. |
| **Why this over alternatives** | A `kotlinx.coroutines.sync.Mutex` would also prevent concurrent execution but adds `withLock` boilerplate and can suspend the caller. A `synchronized` block blocks the calling thread. The `Job.isActive` check is the simplest idiomatic pattern for preventing duplicate one-off background operations — it is a non-blocking check, not a lock, so it never suspends or creates contention. |
| **Threading model** | The `isActive` guard check runs on **`Dispatchers.Main`** (non-suspending, instant). The queue processing loop runs on **`Dispatchers.IO`** inside `withContext`. Multiple concurrent `syncPendingActions()` calls (e.g., rapid connectivity events) are safely serialized by the guard with zero blocking. |
| **Error handling** | If an individual API call fails inside the loop, that row is left in `pending_availabilities` and retried on the next `syncPendingActions()` invocation. If the entire sync coroutine is cancelled (user leaves the screen), `syncJob` becomes inactive and the next `NetworkCallback` event starts a fresh sync. No data is ever lost because the queue persists in SQLite across process death. |
| **Code location** | `features/availability/presentation/AvailabilityViewModel.kt` → `syncJob`, `syncPendingActions()` · `features/availability/data/local/PendingAvailabilityDao.kt` → `getPending()`, `delete()` |



---

#### Strategy 3 — `ConnectivityManager.NetworkCallback` + coroutine for reactive sync
**Owner:** Nikol Katherin Rodriguez Ortiz

| Field | Content |
|-------|---------|
| **How it works** | The ViewModel registers a `ConnectivityManager.NetworkCallback`. `onAvailable(network)` is called by the Android OS on a system binder thread whenever connectivity is restored. From inside `onAvailable`, `viewModelScope.launch` is called to hop back to `Dispatchers.Main` and then trigger `syncPendingActions()`. `onLost(network)` sets `_isOffline = true` and emits an updated UI state to show the offline banner. The callback is unregistered in `onCleared()` to prevent memory leaks and stale callbacks after ViewModel destruction. |
| **Why this over alternatives** | Polling with `Timer` or `WorkManager` periodic tasks introduces latency between reconnection and sync, and wastes battery during offline periods. `BroadcastReceiver` with `CONNECTIVITY_ACTION` is deprecated since API 28. `NetworkCallback` is the modern battery-efficient Android API for connectivity events — it fires exactly when the network state changes. Pairing it with `viewModelScope.launch` is mandatory because `onAvailable` runs on an OS binder thread, not the main thread, so UI state cannot be updated directly from inside the callback. |
| **Threading model** | `onAvailable` / `onLost` are called on an **OS binder thread** (not Main, not IO). `viewModelScope.launch` inside the callback posts work to **`Dispatchers.Main`**, where UI state is updated and `syncPendingActions()` is called, which then dispatches queue processing to **`Dispatchers.IO`**. Three threads are involved, each with a clear responsibility. |
| **Error handling** | If `viewModelScope` has already been cancelled when `onAvailable` fires (screen destroyed), the `launch` is a no-op. The callback is unregistered in `onCleared()`, making this scenario rare. Duplicate sync launches triggered by rapid connectivity events are prevented by the `Job.isActive` guard in Strategy 2. Individual sync failures leave rows in the queue for the next reconnection event. |
| **Code location** | `features/availability/presentation/AvailabilityViewModel.kt` → `registerNetworkCallback()`, `unregisterNetworkCallback()`, `onCleared()` · `features/availability/presentation/AvailabilityScreen.kt` → offline banner composable |


<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/Multithreading%20Strategies%20%20availability.png" />

---


### Courses Management

#### Strategy 1 — `withContext(Dispatchers.IO)` for Relational Data Persistence
**Owner:** Daniel Camilo Quimbay Velásquez

| Field | Content |
|-------|---------|
| **How it works** | When loading the catalog or applying for a course, the ViewModel initiates a coroutine. Inside, `withContext(Dispatchers.IO)` shifts execution to the IO thread pool to interact with the SQLite database and the NestJS API. This ensures that complex relational queries (joining available vs. approved courses) and network latency do not block the Main thread. Once the background work is finished, the coroutine automatically resumes on the Main thread to update the UI state. |
| **Why this over alternatives** | Traditional `Thread` or `AsyncTask` implementations would require manual handlers to return data to the UI, increasing the risk of memory leaks. `withContext` is the idiomatic Kotlin solution for context-shifting, providing a sequential and readable flow while ensuring that expensive I/O operations are offloaded from the frame-rendering thread. |
| **Threading model** | Starts on **`Dispatchers.Main`**, suspends into **`Dispatchers.IO`** for SQLite/API transactions, and resumes on **`Dispatchers.Main`** for state notification. |
| **Error handling** | Wrapped in `try/catch` blocks. If the API call fails or the database is locked, the exception is caught and the UI is updated with an `Error` state or falls back to the `isOffline` cached data from the local DB. |
| **Code location** | `features/courses/data/CoursesRepository.kt` → `fetchCourses()` · `features/courses/presentation/CoursesViewModel.kt` → `loadData()` |

---

#### Strategy 2 — `MutableStateFlow` as a reactive UI bridge
**Owner:** Daniel Camilo Quimbay Velásquez

| Field | Content |
|-------|---------|
| **How it works** | The ViewModel maintains a private `MutableStateFlow<CoursesState>` that defaults to `Loading`. After background tasks complete, the new state (Success or Error) is assigned on the Main thread. The Compose UI observes this via `collectAsState()`. This ensures that any data emitted from the repository layer is safely bridged to the UI, triggering recomposition only when the state actually changes. |
| **Why this over alternatives** | Unlike `LiveData`, `StateFlow` is native to coroutines and doesn't require a lifecycle owner to be passed into the repository. It provides a more robust and type-safe way to handle UI states in a modern Compose-based architecture compared to manual `runOnUiThread` calls. |
| **Threading model** | State assignments occur on **`Dispatchers.Main`**. The `StateFlow` mechanism is internally thread-safe, but UI consumption is strictly bound to the Main thread to avoid illegal state exceptions during recomposition. |
| **Error handling** | Uses a Sealed Class `CoursesState` to exhaustively handle `Loading`, `Success`, and `Error` cases. This prevents the UI from entering inconsistent states and ensures the "Offline" banner is displayed only when the `isOffline` flag is verified. |
| **Code location** | `features/courses/presentation/CoursesViewModel.kt` → `_coursesState`, `coursesState` · `features/courses/presentation/CoursesScreen.kt` |


<img width="100%" alt="CourseMultiThreading" src="https://github.com/user-attachments/assets/91d7beda-634d-408d-85ea-806e7f13e4ff" />


---

### Profile Identity

#### Strategy 1 — Parallel Execution via `async/await` for Multi-Source Loading
**Owner:** Daniel Camilo Quimbay Velásquez

| Field | Content |
|-------|---------|
| **How it works** | To populate the Profile view, the app must fetch tutor metadata and a profile image simultaneously. Instead of sequential calls, `viewModelScope.launch` uses `async(Dispatchers.IO)` for each task. Both requests run in parallel. The UI then `await()`s both results. This reduces total load time to the duration of the slowest individual request rather than the sum of all requests. |
| **Why this over alternatives** | Sequential loading with `withContext` would double the perceived latency. Spawning raw Threads would be resource-heavy and lack structured cancellation. `async` provides the perfect balance of parallel execution and easy synchronization within the same coroutine scope. |
| **Threading model** | Multiple concurrent tasks are dispatched to the **`Dispatchers.IO`** thread pool. The results are gathered and returned to **`Dispatchers.Main`** using structured suspension. |
| **Error handling** | Each `deferred.await()` is monitored. If the image fails to load, the metadata can still be displayed using a placeholder avatar, ensuring the entire screen doesn't fail due to a single non-critical resource. |
| **Code location** | `features/profile/presentation/ProfileViewModel.kt` → `loadProfileData()` |

---

#### Strategy 2 — Defensive Synchronization for Manual LRU Cache
**Owner:** Daniel Camilo Quimbay Velásquez

| Field | Content |
|-------|---------|
| **How it works** | The profile uses a manual `LruCache` to store Bitmaps. Because the cache can be accessed by the Main thread (for rendering) and background threads (for decoding newly downloaded images), all access methods are marked with `@Synchronized`. This creates a monitor lock that ensures only one thread can modify the cache's internal `LinkedHashMap` at a time. |
| **Why this over alternatives** | Without `@Synchronized`, concurrent access would trigger a `ConcurrentModificationException`. While a `Mutex` could be used, `@Synchronized` is more performant for the millisecond-fast operations required by a memory cache, as it avoids the overhead of coroutine suspension for simple memory writes. |
| **Threading model** | Uses JVM-level synchronization to protect shared memory across **Main** and **IO** threads. |
| **Error handling** | Prevents race conditions during cache eviction. If two threads try to insert a bitmap simultaneously, the lock ensures they are serialized, preventing memory corruption or null-pointer hits. |
| **Code location** | `core/cache/InMemoryCache.kt` → `@Synchronized put()`, `get()` |

<img width="100%" alt="ProfileMultiThreading" src="https://github.com/user-attachments/assets/9b25bfad-1029-49aa-8aa6-58e28afa6f26" />



---
## 7. Caching Strategies

### Flutter

### Cache 1 — LRU Cache (`lru_cache.dart`)
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **Data structure** | Generic `LRUCache<K, V>` backed by `LinkedHashMap` from `dart:collection` |
| **Eviction policy** | Least Recently Used — on cache hit, entry is removed and re-inserted at tail (MRU position); when `maxSize` is exceeded, the first key (LRU position) is evicted |
| **Complexity** | `get`: O(1) · `put`: O(1) · `evict`: O(1) — `LinkedHashMap` maintains insertion order, enabling constant-time head removal |
| **TTL** | Per-entry expiry via `_CacheEntry` wrapper storing `expiresAt`. Expired entries are treated as cache misses and evicted on access — no background timer needed |
| **Parameters** | `maxSize: 10` (≈ number of courses in the app) · `ttl: 5 minutes` (tutor availability changes frequently — longer TTL would show stale booked slots) |
| **Where used** | `AnalyticsRepositoryImpl` — `static LRUCache<String, List<TutorEntity>>` keyed by `courseId`. Tutor lists are reused across widget rebuilds and screen navigation without extra network calls |
| **Why LRU over alternatives** | FIFO would evict recently used courses. LFU adds frequency counters with no benefit at n=10. LRU evicts the least recently accessed course — the most likely to be irrelevant — with zero extra bookkeeping |
| **Why `LinkedHashMap`** | Maintains insertion order natively in Dart. Removing and re-inserting a key moves it to the tail in O(1). A plain `HashMap` cannot express access order; a `List` would require O(n) search |

---

### Cache 2 — ArrayMap (`array_map.dart`)
**Owner:** Paola Catherine Jimenez Jaque

| Field | Content |
|-------|---------|
| **Data structure** | Generic `ArrayMap<K extends Comparable<K>, V>` backed by two parallel sorted `List`s (keys + values) |
| **Lookup** | Binary search → O(log n). For n ≤ 10: ≤ 4 comparisons. Comparable to HashMap O(1) but with lower constant due to contiguous memory — no pointer chasing |
| **Insert** | O(n) due to sorted-array shift. Acceptable because writes are rare relative to reads in both use cases |
| **Memory advantage** | Zero fixed overhead vs HashMap's minimum 128-byte bucket array. ~160 B for 10 entries vs ~480 B for an equivalent HashMap |
| **Where used — 1** | `HomeController._sessionCountCache: ArrayMap<String, int>?` — caches session counts per `courseId` to power the *"Recommended for you"* section. Built once per `loadSessions` call, nulled on reload, reused on every `recommendedCourses` getter call |
| **Where used — 2** | `ProfileRepositoryImpl._inMemoryPatch: ArrayMap<String, String>` — L1 layer for pending offline profile description edits keyed by `userId`. Written alongside SharedPreferences (L2) when offline; checked first during sync to avoid deserialization overhead |
| **Why ArrayMap over HashMap** | At n ≤ 10, binary search (≤ 4 ops on contiguous memory) outperforms hashing (hash computation + bucket lookup + pointer dereference). Memory footprint is 3× smaller. The sorted invariant also makes the structure self-documenting — iteration order is predictable |
| **Why ArrayMap over LRUCache** | No eviction needed — `_sessionCountCache` is rebuilt from scratch on every data reload and nulled explicitly; `_inMemoryPatch` holds at most one entry per user. LRU overhead (reordering on access) would add complexity with no benefit |

### Summary for the two previous strategies:
<img width="348" height="600" alt="image" src="https://github.com/user-attachments/assets/29152cf8-fce6-4245-9395-58a9680125c1" />



### Cache 3 — Your go-to tutor
**Owner:** Maria Lucia Benavides Domínguez


The UI surfaces a **returning (“go-to”) tutor** for a given student and course, loaded from the analytics API and reused across navigation (e.g. home and course detail).

#### Problem identified

Without caching, every screen entry or rebuild risks **repeated HTTP calls** for the same `(studentId, courseId)` pair, increasing latency and battery use. When the network is unavailable, the user should still see the **last successfully fetched** tutor instead of an empty or broken state.

#### Caching strategy implemented

A **two-tier** approach:

1. **L1 — In-memory LRU with TTL** for the remote read (`/analytics/returning-tutor`), so duplicate requests in the same session are served from RAM when still valid.
2. **L2 — SQLite row** keyed by `(student_id, course_id)` storing the last successful JSON payload, used **only when the remote call fails** (offline or error path).

#### Technical implementation


| Layer         | Mechanism                                                                                                              | Parameters / shape                                                                                                                                                                                                                               | Code location                                                                                                                                                                        |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **L1 LRU**    | `LRUCache` backed by insertion-ordered `LinkedHashMap`; least-recently-used eviction when full; entries expire by TTL. | Composite key `(studentId, courseId)`. Capacity and TTL defined in `HomeRemoteMemoryCachePolicy`: **24 entries**, **4 minutes** TTL. Cached value uses a small record type so a valid **“no tutor”** response is not confused with a cache miss. | `lib/core/cache/lru_cache.dart` · `lib/core/cache/home_remote_memory_cache_policy.dart` · `lib/features/home/data/repositories/analytics_repository_impl.dart` (`getReturningTutor`) |
| **L2 SQLite** | `INSERT … ON CONFLICT REPLACE` (upsert) after a successful API response; `SELECT` on failure.                          | Table `cached_go_to_tutor`; `payload` JSON; `last_updated` epoch ms.                                                                                                                                                                             | `lib/core/storage/app_database.dart` · `lib/features/home/data/repositories/student_tutoring_repository_impl.dart` (`getGoToTutor`)                                                  |


**Why LRU + TTL (L1):** bounded memory and time-bound freshness for repeated `(studentId, courseId)` lookups in the same session, without unbounded growth of heap structures.

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/89d3e633-e5e5-45b0-bab0-db4e44ca95d8" />
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/c016b0bb-c5d4-433f-a4c7-fac6e18f09ca" />


App
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/b1ae5dab-ca45-4214-bb7f-7eb6b45b1d7b" />


Diagram of how the caching strategy works:
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/b8f90dee-3811-4026-b7d7-e3043e31fafe" />


#### Expected impact

- **Performance:** fewer identical network round-trips during a single session.
- **Responsiveness:** immediate RAM hit when the LRU entry is valid.
- **User experience:** last known go-to tutor remains available when offline, consistent with the remote-first / local-fallback pattern described in **§5 Local Storage Strategies**.


---

### Cache 4— Upcoming Sessions (offline-capable list)
**Owner:** Maria Lucia Benavides Domínguez

#### Feature

The UI displays a student's **upcoming tutoring sessions**, derived from the full session list returned by the tutoring sessions API and rendered in chronological order.

#### Problem identified

Without caching, every home reload triggers a full fetch + JSON parse of the student's session list, increasing latency and network usage. When the device is offline (or the API call fails), the user should still see the **last known upcoming sessions** rather than an empty state.

#### Caching strategy implemented

A **two-tier** approach (remote-first with local fallback):

1. **L1 — In-memory LRU with TTL** at the repository that performs the HTTP call (`getStudentSessions(studentId)`) to deduplicate repeated loads during the same app session.
2. **L2 — SQLite row** keyed by `student_id` storing the **derived upcoming list** (filtered + sorted) as JSON, used **only when the remote call fails** (offline or error path).

#### Technical implementation

| Layer         | Mechanism                                                                                                              | Parameters / shape                                                                                                                                                                                                                                           | Code location                                                                                                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **L1 LRU**    | `LRUCache<String, List<SessionEntity>>` with TTL. `getStudentSessions` returns RAM hit when valid; otherwise fetches HTTP, parses JSON, and stores into LRU. | Key: `studentId`. Policy in `HomeRemoteMemoryCachePolicy`: **4 entries**, **90 seconds** TTL (short TTL because sessions can change after book/cancel/reschedule).                                                                                          | `lib/features/home/data/repositories/session_repository_impl.dart` (`getStudentSessions`) · `lib/core/cache/lru_cache.dart` · `lib/core/cache/home_remote_memory_cache_policy.dart`                            |
| **L2 SQLite** | Upsert on success; `SELECT` on failure. Stores `payload` JSON + `last_updated`.                                        | Table `cache_upcoming_sessions`; key `student_id`; `payload` is the **filtered + sorted upcoming list** (not the raw server list). This keeps the offline view aligned with what the UI actually renders.                                                    | `lib/core/storage/app_database.dart` (`tableUpcomingSessions`) · `lib/features/home/data/repositories/student_tutoring_repository_impl.dart` (`getUpcomingSessions`, `_readUpcomingSessionsCache`, `_safeUpsert`) |

**Why two tiers:** L1 absorbs short bursts (navigation home ↔ detail, pull-to-refresh double taps) with minimal overhead; L2 guarantees a durable offline fallback across app restarts.

#### Expected impact

- **Performance:** fewer redundant HTTP requests and JSON parses during a session (L1).
- **Offline UX:** upcoming sessions remain visible when network calls fail (L2).
- **Correctness:** session list is re-derived on each successful refresh (filter “future”, sort by start time) before being persisted, ensuring consistency with the UI.

App:
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/a8f0c5ca-ffdf-4491-ba86-a75168201f02" />

Diagram that shows the caching strategy
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/1df21fcf-621f-4913-a242-d26ffc00186c" />

    
---


### Cache 5 — Motion (emergency) alerts
**Owner:** Maria Lucia Benavides Domínguez

The **motion alert** flow stores **user-configured alert settings** (email, student name, location, enabled flag) so the monitoring service and UI can read them without re-querying disk on every access, while still **persisting** across app restarts.

#### Problem identified

Reading `SharedPreferences` on every frame or service tick would be inefficient; losing settings on process death would **force re-entry** of sensitive configuration. The app also needs a **consistent in-memory view** for `ValueNotifier`-driven UI updates.

#### Caching strategy implemented

A **process-local memory mirror** plus **persisted key-value storage** (not an LRU eviction policy — settings are a single small object per device, not a large keyed collection):

1. **In-memory field** updated on every successful `load()` / `save()` and exposed through `ValueNotifier` for reactive UI.
2. **SharedPreferences** as the durable store for the same fields.

#### Technical implementation


| Concern           | Mechanism                                           | Stored shape                                         | Code location                                     |
| ----------------- | --------------------------------------------------- | ---------------------------------------------------- | ------------------------------------------------- |
| **Hot read path** | Static `_memoryCache` + `changes` (`ValueNotifier`) | `MotionAlertSettings` (immutable record-style class) | `lib/core/services/motion_alert_preferences.dart` |
| **Persistence**   | `SharedPreferences` string/bool keys                | Scalar keys (`motion_alert_email`, etc.)             | Same file: `load()`, `save()`, `clear()`          |


**Why this pattern:** alert settings are **low cardinality** (one active configuration set), **read often** by UI and services, and **must survive restarts** — a full LRU layer would add complexity without benefit. Clearing on logout is handled elsewhere in the coordinator flow (see **§5** privacy notes); this section only covers the **cache-like** read path.

<img width="35%" alt="image" src="https://github.com/user-attachments/assets/f548f78d-84fc-4744-ab94-d1408dbc94cd" />
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/95202c51-5ffe-4ed5-bdb6-465a9da863eb" />


App:
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/cfa20617-8c36-4513-acd7-1c42ca848411" />

Diagram that shows the caching strategy
<img width="35%" alt="image" src="https://github.com/user-attachments/assets/0ae88a36-ca94-49aa-9845-1456506f11f3" />

#### Results / expected impact

- **Performance:** avoids redundant async disk reads once settings are loaded.
- **Responsiveness:** UI listens to `ValueNotifier` for immediate feedback after save.
- **User experience:** settings survive app restarts; memory stays aligned with disk after `load()` / `save()`.


### Kotlin

---

#### 1. Home Page
**Owner:** Nikol Katherin Rodriguez Ortiz


**Cache layers:**

| Layer | Implementation | Eviction policy | Max size | Speed |
|-------|---------------|-----------------|----------|-------|
| **L1 — In-Memory** | `LinkedHashMap` with access-order enabled | LRU — oldest accessed entry removed first | 20 entries | Nanoseconds — no I/O |
| **L2 — SQLite** | `cache_home` table (`json_data`, `timestamp`) | TTL-based — `isFresh = now - timestamp < expiryMs` (5 min) | Single row per section | Milliseconds — disk read |

**Read policy:**

| Priority | Source | Condition |
|----------|--------|-----------|
| 1st | L1 LinkedHashMap | Key present in memory |
| 2nd | L2 SQLite | L1 miss → TTL still valid |
| 3rd | API | L2 miss or TTL expired and online |
| Fallback | L2 SQLite (stale) | API failure or offline |

**Invalidation policies:**

| Event | L1 action | L2 action |
|-------|-----------|-----------|
| Successful API response | Populate / overwrite | Write new `json_data` + `timestamp` |
| API failure | No change | Serve stale (no invalidation) |
| Normal read (cache hit) | No invalidation | No invalidation |
| User navigates away | No invalidation | No invalidation |

<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/Home%20Page%20Caching.png" />

---

#### 2. Availabilities
**Owner:** Nikol Katherin Rodriguez Ortiz

**Cache layers:**

| Layer | Implementation | Purpose | Speed |
|-------|---------------|---------|-------|
| **L1 — Read cache** | `LinkedHashMap` (in-memory) | Holds the currently loaded availability list for instant UI access | Nanoseconds — no I/O |
| **L1 — Optimistic write cache** | `LinkedHashMap` with `local-{timestamp}` keys | Reflects mutations immediately in the UI before backend confirmation | Nanoseconds — no I/O |

**Optimistic write flow:**

| Step | Action | ID used |
|------|--------|---------|
| User mutates offline | Write to L1 + enqueue to SQLite | `local-{timestamp}` (temporary) |
| Sync completes | Replace entry in L1 | Real server ID replaces `local-{timestamp}` |
| Sync fails | Entry kept in L1 with temporary ID | `local-{timestamp}` retained until next sync |

**Invalidation policies:**

| Event | L1 action |
|-------|-----------|
| Successful mutation (online) | Clear read cache → reload from API |
| Sync completed | Replace `local-{timestamp}` keys with server IDs |
| Normal read (cache hit) | No invalidation |
| Offline mutation | Optimistic write — no invalidation of existing entries |
| Connectivity lost | No invalidation — stale L1 continues serving the UI |

<img width="100%" alt="image" src="https://github.com/Team23-ISIS3510/.github/blob/main/Availabilities%20Caching.png" />

---

#### 3. Cache Core & API Utilities
**Owner:** Daniel Camilo Quimbay Velásquez

**Cache layers:**

| Layer | Implementation | Eviction policy | Max size | Speed |
|-------|---------------|-----------------|----------|-------|
| **L1 — In-Memory** | `InMemoryCache.kt` (LinkedHashMap) | LRU — @Synchronized access-order | 20 entries | Nanoseconds — no I/O |
| **L1 — API Specialized** | `ApiResponseCache.kt` (LinkedHashMap) | LRU — Specific domain instances | 5-10 per type | Nanoseconds — no I/O |

**Technical Implementation:**

| Component | Logic | Location |
|-----------|-------|----------|
| **InMemoryCache** | Generic thread-safe container for high-speed object retrieval | `InMemoryCache.kt` (Lines 29-71) |
| **ApiResponseCache** | Specialized instances for Courses, Applications, and Approved lists | `ApiResponseCache.kt` (Lines 14-94) |
| **ServiceLocator** | Singleton registration and initialization for cache instances | `ServiceLocator.kt` (Lines 47-50) |

<img width="100%" alt="Cache1" src="https://github.com/user-attachments/assets/0d6ef472-fb23-46fa-98c6-7a68545912d5" />


---

## Additional Info

**APK Link (Flutter):** https://appdistribution.firebase.google.com/testerapps/1:1056254794426:android:a6657b597388a2d7188083/releases/2ga5b08b0s0v8?utm_source=firebase-console

**APK Link (Kotlin):** https://appdistribution.firebase.google.com/testerapps/1:1056254794426:android:7d1c09752dcd0486188083/releases/50m8ro4tq8duo?utm_source=firebase-console

**Ethics video:** https://youtu.be/gTEPZZqC3DU