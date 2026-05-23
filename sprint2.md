# Sprint 2 – App Implementation

## General Information

| | |
|---|---|
| **Sprint** | Sprint 2 – App Implementation |
| **Daniel Camilo Quimbay Velasquez** | d.quimbay@uniandes.edu.co |
| **Nikol Katherine Rodriguez Ortiz** | nk.rodriguez@uniandes.edu.co |
| **Paola Catherine Jimenez Jaque** | p.jimenezj@uniandes.edu.co |
| **Maria Lucia Benavides Domínguez** | m.benavidesd@uniandes.edu.co |

| Repository | Link |
|---|---|
| 🐦 Flutter App | [calico-mobile-flutter](https://github.com/Team23-ISIS3510/calico-mobile-flutter) |

| 🤖 Kotlin App | [calico-mobile-kotlin](https://github.com/Team23-ISIS3510/calico-mobile-kotlin) |
| ⚙️ Backend | [backend](https://github.com/Team23-ISIS3510/backend) |
| 📊 Analytics Pipelines | [backend/analytics](https://github.com/Team23-ISIS3510/backend/tree/dev/src/modules/analytics) |

---

## 1. Problem & Solution

### 🔴 Problem
The university peer-tutoring ecosystem is currently paralyzed by structural disorganization, relying on informal, decentralized channels like WhatsApp that create a significant "coordination and discovery gap". This fragmentation forces students into a 48-hour confirmation void while preventing qualified tutors from efficiently broadcasting their availability to those in need. Because there is no centralized platform for academic transparency, students are forced into a "quality gamble", risking time and money on superficial assistance rather than the deep foundational understanding required for long-term success. Ultimately, this lack of a formal marketplace transforms what should be a simple academic exchange into a frustrating, inefficient, and unreliable process for both parties.


### 🟢 Solution
Our solution is a Centralized Peer-Tutoring Marketplace that replaces informal negotiation with a professionalized, dual-sided ecosystem built on a synchronous booking engine and verified academic transparency. By centralizing the tutor-student connection, the platform allows students to move instantly from "academic crisis" to a confirmed session via real-time scheduling and verified tutor profiles that emphasize teaching depth. Simultaneously, the platform empowers tutors by providing a professional management dashboard to track performance, earnings, and availability, effectively transforming fragmented peer-help into a reliable, scalable, and data-driven academic economy.

---

## 2. Business Questions (BQs)

| ID | Business Question | Type | Owner |
|----|-------------------|------|-------|
| **BQ1** | How stable and performant is the application during peak academic periods (crash rate, API latency, and failed booking/payment events)? | Type 1 — Technical telemetry to ensure system reliability during high-demand weeks. | Daniel Camilo Quimbay Velásquez |
| **BQ3** | For a given student, which tutors with rating >4.5 and availability within the next 4 hours should be recommended? | **Type 2** — Real-time query of availability and ratings shown directly to students | Maria Lucia Benavides Domínguez |
| **BQ4** | What is the student search volume per hour for a tutor's specific subjects compared to their current availability? | **Type 2** — Combines real-time search logs with availability data to balance supply and demand | Nikol Katherin Rodriguez Ortiz |
| **BQ5** | What percentage of booking attempts for the earliest available slot succeed instantly without manual confirmation? | **Type 2** — Measures the success rate of the "Earliest Available" flow | Paola Catherine Jimenez Jaque |

---

## 3. Analytics Pipeline (AP)

### BQ1 – App Stability & Performance (Telemetry)

> **Question:** What is the real-time health of the application in terms of crashes and service latency?

#### Processing Steps (Automated)

| Step | Layer | Description |
|------|-------|-------------|
| 1. Crash Interception | Frontend | Firebase Crashlytics — automatically captures stack traces of unhandled exceptions in Kotlin |
| 2. Network Tracing | Frontend | Firebase Performance — monitors `http` request duration to NestJS and Google Calendar |
| 3. Event Logging | Frontend | Firebase Analytics — logs custom `failed_booking_rate` and `failed_payment_rate` events |
| 4. Visualization | Dashboard | Google Cloud Monitoring — aggregates trends into a 4-line weekly time-series chart |

#### Business Value
- Identifies technical friction before it impacts the "Academic Crisis" window.
- Allows the team to distinguish between user-error and system-failure during bookings.
- Provides data-driven proof of platform reliability for university stakeholders.


### BQ3 – Available High-Rated Tutors (Next 4 Hours)

> **Question:** Which tutors with rating > 4.5 are available for a session within the next 4 hours?

#### Processing Steps

| Step | Layer | Description |
|------|-------|-------------|
| 1. Course Filter | Bronze → Silver | Firestore query — retrieve tutors teaching the requested course |
| 2. Rating Filter | Silver | In-memory — keep only tutors where `rating > 4.5` |
| 3. Availability Filter | Silver | Parallel queries — find active slots within the next 4 hours per tutor |
| 4. Aggregation | Gold | Sort by `startDateTime`, count `availableSlotsCount`, rank by rating DESC |

#### Derived Metrics

| Metric | Definition |
|--------|-----------|
| **Next Available Slot** | Earliest available session within the time window |
| **Available Slots Count** | Number of valid slots within the next 4 hours |

#### Pipeline Architecture

| Layer | Contents |
|-------|----------|
| **Bronze** | `users`, `availabilities` |
| **Silver** | Course filtering → Rating filtering → Availability filtering |
| **Gold** | AGGREGATE → SHAPE → WRAP → SERVE `GET /analytics/available-tutors?course=X` |

#### Business Value
- Fast discovery of high-quality tutors
- Reduced student decision time
- Increased booking conversion rates
- Better utilization of high-performing tutors

### Diagram
<img width="1981" height="512" alt="Context Canvas-Medallion drawio" src="https://github.com/user-attachments/assets/d58bd516-f08b-4479-a786-c60fa5d7662c" />

---

### Smart Feature Flutter – Personalized "Go-To Tutor"

> Identifies the tutor a student trusts most and checks their upcoming availability.

| Step | Action |
|------|--------|
| 1. History Aggregation | Group sessions by student + course + tutor → compute `bookingCount` per tutor |
| 2. Ranking | Sort by `bookingCount DESC` → select top tutor per course |
| 3. Availability Check | Query next available slot and total available slots for selected tutor |

**Business Value:** Delivers a highly personalized experience, reduces friction in repeat bookings, and increases retention.

---

### BQ4 – Tutor Occupancy

> **Question:** What is the student search volume per hour for a tutor's specific subjects compared to their current availability?

#### Key Metrics

| Metric | Formula |
|--------|---------|
| **Occupancy Rate** | (Total session hours / Total available hours) × 100 |
| **Sessions per Hour** | Total sessions / Total available hours |

#### Processing Steps

| Step | Description |
|------|-------------|
| 1 | Retrieve all tutor sessions within the last 2 years |
| 2 | Filter valid sessions (with scheduled date) |
| 3 | Group sessions by subject |
| 4 | Retrieve tutor availability in the same period |
| 5 | Compute global metrics |
| 6 | Split data into high-demand vs normal periods |
| 7 | Compute metrics for each period |

#### Pipeline Architecture

| Layer | Contents |
|-------|----------|
| **Bronze** | `tutoringSessions`, `availabilities` |
| **Silver** | Grouping → Summing → Splitting by demand periods → In-memory aggregation |
| **Gold** | AGGREGATE → SHAPE → WRAP → SERVE (Kotlin frontend) |

#### Diagram
<img src="https://github.com/Team23-ISIS3510/.github/blob/main/Sprint%202/Context%20Canvas-BQ4.drawio.png" />

---

### BQ5 – Instant Booking Success Rate

> **Question:** What percentage of booking attempts for the earliest available slot succeed instantly without requiring manual confirmation?

#### Core Logic

| Step | Action |
|------|--------|
| 1. Tutor Discovery | `GET /analytics/bookable-tutors?course=<courseId>` — filters by rating > 4.5, enriches with `parentAvailabilityId` + `slotIndex` |
| 2. Slot Validation | Checks `slot_bookings` collection — verifies slot is not already taken |
| 3. Instant Booking | `POST /tutoring-sessions` with `requiresApproval: false` → `status: scheduled`, `tutorApprovalStatus: approved` |
| 4. Metric Collection | Records outcome in `tutoringSessions` — `approved` = instant success, `pending` = manual confirmation needed |

#### Success Rate Formula
```
Success Rate = 
  COUNT(tutoringSessions where tutorApprovalStatus = "approved" AND status = "scheduled")
  ─────────────────────────────────────────────────────────────────────────────────────
  COUNT(all tutoringSessions)
× 100
```

#### Pipeline Architecture

| Layer | Contents |
|-------|----------|
| **Bronze** | `users`, `availabilities`, `slot_bookings`, `tutoringSessions` |
| **Silver** | Tutor filtering → Slot enrichment → Real-time validation → Session creation → Slot locking |
| **Gold** | AGGREGATE (approved/total) → SHAPE (session object) → WRAP (confirmation) → SERVE `BookingBottomSheet ✅` |

#### Business Value
- Eliminates the 48-hour confirmation void
- Increases booking conversion rate
- Builds student trust in the platform
- Provides measurable data to evaluate the booking engine

#### Diagram
<img width="700" height="700" alt="DiagramBW" src="https://github.com/user-attachments/assets/acd35b54-2be5-434c-a6d2-c4e3b84c473c" />

---

## 4. Architectural Design

### 4.1 Backend Architecture

#### Architectural Styles

| Style | Description | Rationale |
|-------|-------------|-----------|
| **Client‑Server** | Mobile apps communicate with a centralized NestJS backend | Centralizes business logic, authentication, and data management |
| **Layered Architecture** | Controllers → Services → Repositories → Infrastructure | Improves maintainability, testability, and separation of concerns |
| **Modular Monolith** | Domain modules deployed as a single unit | Loosely coupled modules with simpler deployment than microservices |

#### Architectural Patterns

| Pattern | Where Used | Rationale |
|---------|------------|-----------|
| **Service Layer** | Backend services (e.g. `TutoringSessionService`) | Keeps controllers thin; promotes reuse and testability |
| **DTO Pattern** | `CreateAvailabilityDto`, etc. | Ensures data integrity and clear API contracts |
| **Guard Pattern** | `FirebaseAuthGuard` | Separates auth concerns from business logic |
| **Facade Pattern** | `CalicoCalendarService` | Hides Google Calendar API complexity |
| **Factory Pattern** | `AvailabilityResponseDto.fromEntity()` | Encapsulates instantiation logic of complex objects |
| **Transaction Pattern** | `TutoringSessionRepository` | Ensures ACID properties for booking operations |
| **Strategy Pattern** | `mapFirebaseError()` | Centralized, clean Firebase error handling |

#### Architectural Tactics

| Tactic | Description | Quality Attribute |
|--------|-------------|-------------------|
| Separation of Concerns | Layers and modules have distinct responsibilities | Maintainability |
| Input Validation | DTOs validate all incoming data | Security, Robustness |
| Authentication Boundary | Firebase Auth + Guards protect all sensitive endpoints | Security, Integrity |
| Dependency Injection | Backend uses constructor injection | Testability, Loose Coupling |
| Retry with Idempotency | Booking requests are idempotent; retries are safe | Reliability |
| Graceful Degradation | If Google Calendar is down, app shows last known availability | Resilience |
| Audit Logging | All critical actions logged for monitoring | Maintainability, Security |
| Error Mapping | Firebase errors mapped to NestJS exceptions | Robustness |

#### Diagrams

**Component Diagram**
<img alt="Component Diagram" src="https://github.com/Team23-ISIS3510/.github/blob/main/Sprint%202/Component%20Diagram.png" />

**Deployment Diagram**
<img alt="Deployment Diagram" src="https://github.com/Team23-ISIS3510/.github/blob/main/Sprint%202/Deployment%20Diagram.png" />

**Data Flow Diagram (Booking a Session)**
<img src="https://github.com/Team23-ISIS3510/.github/blob/main/Sprint%202/Data%20Flow%20Diagram%20(Booking%20a%20Session).png" />

---

### 4.2 Flutter Architecture

#### Architectural Styles

| Style | Scope | Evidence |
|---|---|---|
| **Client–Server** | Network boundary | `ApiClient` sends HTTP requests to NestJS; no persistent local store |
| **Feature-First Layered** | Internal structure | Vertical slices (`auth/`, `home/`, `profile/`), each with `domain → data → presentation` |

#### Internal Layer Structure

<img width="649" height="583" alt="image" src="https://github.com/user-attachments/assets/e2742465-2a89-4e21-bbfd-02bad5507fd2" />

> `domain/` is pure Dart — no Flutter imports, no network imports. Independently testable.
```
lib/
├── core/
│   ├── network/api_client.dart              ← single HTTP façade (GET/POST/PATCH)
│   ├── errors/app_exception.dart            ← unified error type
│   ├── validators/form_validators.dart      ← client-side validation rules
│   └── widgets/                             ← shared reusable widgets
│
└── features/
    ├── auth/
    │   ├── domain/
    │   │   ├── models/
    │   │   │   ├── login_request.dart
    │   │   │   └── register_request.dart
    │   │   └── repositories/
    │   │       └── auth_repository.dart          ← abstract contract
    │   ├── data/repositories/
    │   │   └── auth_repository_impl.dart          ← HTTP impl
    │   └── presentation/
    │       ├── controllers/
    │       │   ├── login_controller.dart          ← ChangeNotifier
    │       │   └── register_controller.dart       ← ChangeNotifier
    │       └── screens/
    │           ├── login_screen.dart              ← StatefulWidget
    │           └── register_screen.dart           ← StatefulWidget
    │
    ├── home/
    │   ├── domain/repositories/
    │   │   ├── course_repository.dart             ← abstract
    │   │   ├── session_repository.dart            ← abstract
    │   │   └── analytics_repository.dart          ← abstract (smart features)
    │   ├── data/
    │   │   ├── models/
    │   │   │   ├── course_model.dart
    │   │   │   ├── session_model.dart
    │   │   │   └── available_tutor_model.dart
    │   │   └── repositories/
    │   │       ├── course_repository_impl.dart
    │   │       ├── session_repository_impl.dart
    │   │       └── analytics_repository_impl.dart
    │   └── presentation/
    │       ├── controllers/
    │       │   └── home_controller.dart
    │       ├── screens/
    │       │   ├── home_screen.dart
    │       │   ├── course_detail_screen.dart
    │       │   └── session_detail_screen.dart
    │       └── widgets/
    │           ├── booking_bottom_sheet.dart
    │           ├── course_card.dart
    │           ├── session_card.dart
    │           └── tutor_carousel_card.dart
    │
    └── profile/
        ├── domain/
        │   ├── models/user_profile.dart
        │   └── repositories/profile_repository.dart    ← abstract contract
        ├── data/repositories/
        │   └── profile_repository_impl.dart            ← HTTP impl (GET/PATCH)
        └── presentation/
            ├── controllers/
            │   └── profile_controller.dart             ← ChangeNotifier
            └── screens/
                └── profile_screen.dart                 ← StatefulWidget
```

#### Architectural Patterns

| Pattern | Where Applied | Rationale |
|---|---|---|
| **Repository Pattern** | `domain/repositories/` + `data/repositories/` | Presentation depends only on abstract interfaces; swapping transport requires no UI changes |
| **MVC with ChangeNotifier** | `controllers/` + `screens/` | Flutter-native state management — no Bloc, Provider, or Riverpod |
| **Observer Pattern** | `addListener` / `removeListener` | Screen registers in `initState`, unregisters in `dispose` — no memory leaks |
| **Composite Pattern** | All UI screens | Small, single-purpose widgets; `core/widgets/` never imports from `features/` |
| **Façade Pattern** | `ApiClient` | Wraps `http.Client`, serializes JSON, applies 15s timeout, converts all failures to `AppException` |

#### Architectural Tactics

| Tactic | Implementation | Quality Attribute |
|---|---|---|
| **Separation of Concerns** | Three distinct layers per feature; `core/` never imports from `features/` | Maintainability |
| **Dependency Inversion** | Screens depend on abstract `Repository` interfaces, not `RepositoryImpl` | Testability |
| **Client-Side Validation** | `FormValidators` fires before any HTTP call | Security, UX |
| **Timeout & Error Handling** | 15s timeout; all errors normalized to `AppException` | Reliability |
| **Observer Lifecycle Safety** | `addListener` in `initState`, `removeListener` in `dispose` | Memory Safety |
| **Authentication Boundary** | Firebase `idToken` obtained before any backend call | Security |

#### Data Flow Diagram
<img width="1780" height="2220" alt="Flutter Home Screen – Request Lifecycle" src="https://github.com/user-attachments/assets/616cea33-3df2-4895-90ea-81fa31499169" />


#### Authentication Flow
<img width="953" height="934" alt="image" src="https://github.com/user-attachments/assets/a2add816-a429-4227-9639-47c64300b571" />

---

### 4.3 Kotlin Architecture

#### Architectural Style

| Style | Description | Rationale |
|-------|-------------|-----------|
| **Clean Architecture + MVVM** | Unidirectional data flow across Presentation, Domain, and Data layers | Clear separation of concerns; highly testable and framework-independent |

#### Architectural Patterns

| Pattern | Where Used | Rationale |
|---------|------------|-----------|
| **MVVM** | Jetpack Compose + `StateFlow` | Separates UI from business logic; ViewModel survives configuration changes |
| **Repository Pattern** | `domain/repository/` + `data/repository/` | UI has no knowledge of HTTP, JSON, or the backend |
| **Observer Pattern** | `StateFlow<AuthState>` | Screens recompose automatically on state changes |
| **Composite Pattern** | Reusable composables | Core components never import from feature-specific ones |
| **Guard Pattern** | Token interceptors | Separates authentication concerns from business logic |

#### Architectural Tactics

| Tactic | Description | Quality Attribute |
|--------|-------------|-------------------|
| Separation of Concerns | Data, Domain, Presentation layers have distinct responsibilities | Maintainability |
| Input Validation | User input validated before sending requests | Security |
| Authentication Boundary | Tokens managed via interceptors + `EncryptedSharedPreferences` | Security |
| Dependency Injection | `ServiceLocator` resolves all dependencies | Testability |
| Retry with Idempotency | `RetryQueue` handles transient errors with safe retries | Reliability |
| Error Mapping | Sealed class `Result<T>` provides type-safe error handling | Robustness |
| Secure Storage | `EncryptedSharedPreferences` stores sensitive tokens | Security |
| Token Auto-Refresh | `TokenAuthenticator` intercepts 401s and refreshes tokens | Usability |
| Hardware Abstraction | The `ShakeDetector` wraps the `Accelerometer` sensor logic, decoupling hardware events from UI navigation | Usability / Modularity |
| Real-time Telemetry | Automated Firebase SDK integration provides "zero-code" monitoring of API latency and crash rates | Maintainability |

#### Diagrams

**Component Diagram**
<img alt="Component Diagram" src="https://github.com/Team23-ISIS3510/.github/blob/main/Sprint%202/Components%20diagrams%20kotlin.png" />

**Deployment Diagram**
<img alt="Deployment Diagram" src="https://github.com/Team23-ISIS3510/.github/blob/main/Sprint%202/Deployment%20diagram%20kotlin.png" />

**Data Flow Diagram**
<img width="700" height="700" alt="DataFlow" src="https://github.com/user-attachments/assets/4f55cb9b-4653-48fe-ba02-2375550c45e6" />


---

## 5. Implemented Features

### 5.1 Kotlin Features

| Category | Feature | Description |
|----------|---------|-------------|
| **Authentication** | Secure Firebase Auth | Complete login/register flow using Firebase Auth. Implements EncryptedSharedPreferences for secure token persistence. |
| **Type 1 — BQ1** | Stability Dashboard | Live telemetry tracking crash-free user percentages and API response times across the tutor application. |
| **Sensor Feature** | **Shake-to-Report Bug** | Uses the device's **Accelerometer** to detect a shake gesture, automatically opening an Email Intent with attached device metadata for debugging. |
| **Core UI** | Session Overview | Home screen implementation showing "Incoming" and "Upcoming" sessions fetched from the NestJS backend. |
| **Type 2 — BQ4** | Occupancy | Calculates and displays tutor occupancy by analyzing sessions per hour per subject vs available time slots. Highlights most in-demand weeks. |
| **Smart Feature** | Tutor Recommendations | Suggests relevant tutors based on past bookings and searches using collaborative filtering. |
| **External Service** | Previous & Upcoming Sessions | Displays the two previous and two upcoming sessions for the tutor. |

---

### 5.2 Flutter Features

| Category | Feature | Description |
|---|---|---|
| **Type 2 — BQ3** | Top Rated & Available Soon | Queries tutors with rating > 4.5 available in the next 4 hours for the selected course. Shown as a horizontal carousel in the Course Detail screen. |
| **Type 2 — BQ5** | Instant Slot Booking | Tapping a tutor card opens a bottom sheet with the next slot and a "Book Now" button. Session confirmed instantly with `requiresApproval: false` — no manual tutor step. |
| **Context-Aware** | Time & Session Awareness | Greeting banner (*Buenos días / tardes / noches*) + 6 session-aware motivational messages. Tutor cards show live countdown and *"Today / Tomorrow"* slot labels. |
| **Smart Feature** | Personalized Tutor Recommendations | Real-time carousel filtered by quality (rating > 4.5) and availability. "Go-To Tutor" card with *"Booked N×"* social proof badge. |
| **Authentication** | Full Auth Flow | Email/password registration + login, Google Sign-In via Firebase Auth, Forgot Password via `sendPasswordResetEmail`, Firebase UID propagated to all screens. |
| **External Services** | Backend & Integrations | Central `ApiClient` (GET/POST/PATCH, 15s timeout), Firebase Auth SDK, Google Sign-In SDK, `/analytics/bookable-tutors` endpoint, `url_launcher` for tutor application form. |
| **Core Browsing** | Course & Session Browsing | Course catalogue with real-time client-side search. Course detail view. Upcoming sessions list and session detail with colour-coded status badge. |
| **Profile Management** | Student Profile | Avatar, name, email. Editable description (persisted via PATCH). "Change to Tutor Mode" button opens external Google Form. |
| **Validation & Resilience** | Error Handling & Recovery | 15s timeout + `AppException` normalization. Client-side form validation. Retry button on failure. Silent failure on analytics sections. |

##Ethics component
[Video Link](https://youtu.be/962tCIjgPvY)