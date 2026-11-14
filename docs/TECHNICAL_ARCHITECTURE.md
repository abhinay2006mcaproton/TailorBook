# TailorBook — Technical Architecture

## 1. Architectural Philosophy
TailorBook adopts the **Clean Architecture** and **Modern Android Architecture (MVI/MVVM)** guidelines recommended by Google Android Architecture Guides, with strict offline-first principles.

```
+-------------------------------------------------------------------+
|                        UI Layer (Jetpack Compose)                 |
|  Screens -> Composables -> ViewModels -> UI State (Kotlin Flow)   |
+---------------------------------+---------------------------------+
                                  |
+---------------------------------v---------------------------------+
|                       Domain / Business Layer                     |
|            Use Cases / Interactors -> Validation Logic            |
+---------------------------------+---------------------------------+
                                  |
+---------------------------------v---------------------------------+
|                         Repository Layer                          |
|             Unified Repository Interfaces & Implementations       |
+-------------------+-------------------------------+---------------+
                    |                               |
+-------------------v---------------+   +-----------v---------------+
|    Local Data Source (Room/SQLite)|   | Remote Source (Supabase)  |
| - Offline Single Source of Truth  |   | - PostgreSQL RLS          |
| - DAOs & Entities                 |   | - Supabase Auth & Storage |
| - Pending Sync Queue              |   | - Sync Worker (WorkManager|
+-----------------------------------+   +---------------------------+
```

---

## 2. Multi-Module Project Structure
To ensure clean separation of concerns, fast incremental build times, and high testability, the project is structured as a modular Gradle project:

```
TailorBook/
├── app/                              # App application module, DI graph root, MainActivity
│
├── core/
│   ├── designsystem/                 # Theme, Color, Typography, Shapes, Common Custom Composables
│   ├── navigation/                   # Navigation destinations, type-safe routes, Deep links
│   ├── localization/                 # English/Telugu string management, locale manager
│   ├── database/                     # Room database, Entities, TypeConverters, Migrations, DAOs
│   ├── network/                      # Supabase client wrapper, HTTP interceptors, Connectivity monitor
│   ├── sync/                         # WorkManager sync workers, Sync queue manager, Conflict handlers
│   └── common/                       # Result monad, Dispatcher providers, Currency/Date formatters
│
└── feature/
    ├── auth/                         # Login, OTP verification, Registration
    ├── onboarding/                   # Welcome carousel, Shop setup wizard, Language selection
    ├── dashboard/                    # Main dashboard, Daily summary, Quick actions
    ├── customers/                    # Customer list, Customer details, Add/Edit customer
    ├── measurements/                 # Measurement templates, Version history, Comparison diff
    ├── orders/                       # Order creation flow, Order list, Order details, Status updater
    ├── calendar/                     # Delivery calendar (month/week/day), reschedule dialog
    ├── payments/                     # Payment entry, Receipt generator, Refund/Reversal modal
    ├── gallery/                      # Photo picker/camera, Image compression, Tagging
    ├── reports/                      # Revenue breakdown, Garment analytics, Export utilities
    ├── notifications/                # In-app notifications screen, Local alarm receivers
    ├── staff/                        # Staff list, Invite staff, Permission roles
    ├── subscription/                 # Free vs Premium status, Ad container components
    └── settings/                     # Shop settings, Language switcher, Backup & Restore
```

---

## 3. Core Android Technologies & Backwards Compatibility
- **Target SDK**: API 34 (Android 14) / API 35 (Android 15).
- **Minimum SDK (`minSdk`)**: API 26 (Android 8.0 Oreo).
- **Backwards Compatibility Guarantee**: Full functional support across **at least 2+ major previous Android OS generations** (tested explicitly on Android 12, Android 13, and Android 14/15).
- **OS Version Compatibility Adapters**:
  1. **Notification Permissions**:
     - Android 13+ (API 33+): Runtime `POST_NOTIFICATIONS` permission prompt flow.
     - Android 12 and below (API <= 32): Auto-granted on install; gracefully skips runtime request.
  2. **Photo / Media Picker**:
     - Android 13+ (API 33+): Modern Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
     - Android 12 and below: Fallback to `ActivityResultContracts.GetContent` / Storage Access Framework without broad storage permission prompts.
  3. **Window Insets & Edge-to-Edge**:
     - Android 14/15: Native edge-to-edge system bars.
     - Android 10-13: `WindowCompat.setDecorFitsSystemWindows` with light/dark scrim compensation.
  4. **Exact Alarms & Reminders**:
     - Android 12+ (API 31+): Checked via `AlarmManager.canScheduleExactAlarms()` before setting alarms.
- **Kotlin 1.9+**: Strong typing, Coroutines, Flow, Serialization.
- **Jetpack Compose & Material 3**: Declarative UI, dynamic color tokens, responsive scaffolding.
- **Navigation Compose**: Type-safe navigation between screens.
- **Android Architecture Components**:
  - `ViewModel`: Survives configuration changes and maintains screen UI state.
  - `StateFlow` & `SharedFlow`: Unidirectional data flow (UDF) from ViewModel to Compose.
- **Room Persistence Library**:
  - SQLite abstraction.
  - Embedded relations (`@Relation`, `@Embedded`).
  - Strict type converters for date-time and JSON payloads.
  - Room In-Memory database for fast automated unit and DAO testing.
- **Android WorkManager**:
  - `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`
  - Exponential backoff retry policies for syncing operations and uploading media.
- **Coil (or equivalent lightweight Compose image loader)**: Asynchronous, memory-cached image rendering.

---

## 4. Cloud Backend & Synchronization Architecture
TailorBook uses **Supabase** as its cloud backend service:
- **Database**: PostgreSQL with Row-Level Security (RLS) enabled on 100% of tables.
- **Authentication**: Supabase Auth (JWT).
- **Storage**: Supabase S3-compatible Storage for design reference photos.
- **Offline Sync Contract**:
  - All writes hit the local Room database first.
  - An entry is synchronously appended to `sync_queue` table in Room within the same database transaction.
  - Background `TailorSyncWorker` drains the queue in FIFO order.
  - Operations are idempotent: uses client-generated UUIDs as primary keys.
  - Server confirms sync, and only then is the local sync indicator updated to `SYNCED`.

---

## 5. Future Backend Readiness (ASP.NET Core / C#)
The data contracts, entity models, and repository interfaces in `core/network` and `core/database` avoid proprietary vendor coupling:
- Standard REST / JSON payloads.
- Standard ISO-8601 UTC timestamps.
- Standard UUID v4 primary keys.
- Clean repository abstraction `TailorRemoteDataSource`: If the backend ever migrates from Supabase to an ASP.NET Core 8 Web API / PostgreSQL server, only the remote network adapter implementation changes; no UI, ViewModel, or Room database code is altered.
