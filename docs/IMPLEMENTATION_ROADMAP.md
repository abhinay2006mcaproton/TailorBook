# TailorBook — Implementation Roadmap (Phases 00 to 16)

This roadmap outlines the sequential, phase-wise implementation plan for TailorBook. Execution strictly follows the **one-task-at-a-time** and **one-screen-at-a-time** rules.

---

## Phase Breakdown

### PHASE 00 — Workspace Inspection and Project Planning
- **Objective**: Inspect workspace, audit installed tools (JDK, Android SDK, Gradle, ADB), verify wireless debugging readiness, prepare architecture documentation, roadmap, and tracker.
- **Tasks**:
  - `TASK-00-01`: Inspect workspace directory and existing source code.
  - `TASK-00-02`: Check Java/JDK, Android SDK, Gradle, and ADB installations.
  - `TASK-00-03`: Identify missing prerequisites and define remediation paths.
  - `TASK-00-04`: Create full documentation suite in `docs/`.
  - `TASK-00-05`: Establish Phase/Task/Screen Implementation Tracker.
- **Deliverables**: Comprehensive Phase 00 Inspection Report, completed architecture documents, task tracker.
- **Approval Gate**: Stop and obtain user approval before Phase 01.

---

### PHASE 01 — Project Foundation
- **Objective**: Establish modular Android project shell, Gradle configuration with Kotlin DSL, dependencies, Navigation Compose scaffolding, localization setup, and initial test harness.
- **Tasks**:
  - `TASK-01-01`: Initialize Gradle Kotlin DSL project structure (`app` and `core/*` modules).
  - `TASK-01-02`: Configure Android Gradle Plugin, Kotlin 1.9+, Java 17/21 toolchain, Jetpack Compose compiler.
  - `TASK-01-03`: Configure Room, Coroutines, Flow, Navigation Compose, and WorkManager dependencies.
  - `TASK-01-04`: Establish English and Telugu string resources (`strings.xml` and `values-te/strings.xml`).
  - `TASK-01-05`: Set up Timber/logging and base Application class.
  - `TASK-01-06`: Implement and execute basic smoke test and compile verification.
- **Deliverables**: Compilable Android application shell with multi-module Gradle configuration.

---

### PHASE 02 — Design System and Common Components
- **Objective**: Implement reusable "Premium Boutique" Material 3 design system tokens and foundational UI components.
- **Tasks**:
  - `TASK-02-01`: Define TailorBook Color tokens (Warm Ivory, Deep Teal, Muted Sage, Soft Gold, White, Charcoal).
  - `TASK-02-02`: Implement Typography system supporting English (Roboto) and Telugu (Noto Sans Telugu).
  - `TASK-02-03`: Implement custom `TailorButton` (Primary, Secondary, Text) and `TailorFloatingActionButton`.
  - `TASK-02-04`: Implement custom `TailorTextField` with numeric/measurement decimal input support.
  - `TASK-02-05`: Implement elevated and bordered `TailorCard` components.
  - `TASK-02-06`: Implement standard `TailorTopAppBar` and Left-Side Navigation Drawer (`TailorNavigationDrawer`).
  - `TASK-02-07`: Implement universal screen state components (Loading Shimmer, Empty State, Error State, Offline Banner, Sync Indicator).
  - `TASK-02-08`: Compose preview and UI tests for all design system components.
- **Deliverables**: Design system component library verified on real Android device.

---

### PHASE 03 — Authentication and Shop Onboarding
- **Objective**: Screen-wise implementation of splash, onboarding wizard, shop registration, login, and session persistence.
- **Screens**:
  1. Splash Screen
  2. Onboarding / Welcome Carousel
  3. Language Selection Dialog / Step
  4. Shop Registration Screen
  5. Login / OTP Verification Screen
  6. Shop Profile Setup Screen
- **Tasks**: `TASK-03-01` to `TASK-03-08` covering Room session DAO, ViewModels, UI screens, validation, and wireless device verification.

---

### PHASE 04 — Dashboard and Navigation
- **Objective**: Implement the primary operational hub of TailorBook with live business metrics, quick actions, and left navigation drawer.
- **Screens**:
  1. Dashboard Screen (Metrics cards, today's deliveries, quick actions, offline banner)
  2. Left Navigation Drawer integration
- **Tasks**: `TASK-04-01` to `TASK-04-06` covering metrics calculations, navigation wiring, real device UI verification.

---

### PHASE 05 — Customer Management & Family Profiles
- **Objective**: Full customer relationship management including family members, search, phone actions, and balance tracking.
- **Screens**:
  1. Customer List Screen (Search by name/phone, balance badge)
  2. Add Customer Screen
  3. Edit Customer Screen
  4. Customer Profile Screen (Orders, measurements, family members, payment history)
  5. Family Member Add/Edit Dialog / Screen
- **Tasks**: `TASK-05-01` to `TASK-05-10` covering Room entities, repositories, screens, WhatsApp intent triggers, tests.

---

### PHASE 06 — Measurement Management
- **Objective**: Garment templates, numeric input with fraction support, version history, and comparison tool.
- **Screens**:
  1. Garment Template Selector Screen (Blouse, Shirt, Pants, Kurti, Dress, Churidar, T-shirt, Custom)
  2. Measurement Entry & Edit Screen (Dynamic fields, guide diagrams, units toggle)
  3. Measurement History & Version List Screen
  4. Measurement Comparison (Diff) Screen
- **Tasks**: `TASK-06-01` to `TASK-06-09` covering template JSON schema, Room versioning entities, immutable snapshot protection, verification.

---

### PHASE 07 — Design Gallery
- **Objective**: Photo capture, gallery selection, local compression, tagging, and order attachment.
- **Screens**:
  1. Design Gallery Grid Screen
  2. Photo Capture / Picker & Crop Screen
  3. Photo Details & Tagging Screen
- **Tasks**: `TASK-07-01` to `TASK-07-07` covering Android camera/picker intents, image file management, offline upload queue.

---

### PHASE 08 — Order Management
- **Objective**: Complete end-to-end order booking flow, measurement snapshots, status workflow, and timeline.
- **Screens**:
  1. Order Creation Wizard (Customer -> Garment -> Measurement Snapshot -> Instructions -> Delivery Date -> Price/Advance)
  2. Order List Screen (Filtered by status tabs: All, New, In Progress, Ready, Delivered)
  3. Order Details Screen (Items, measurement snapshot, status stepper, payment status, design photos)
  4. Order Status Update Bottom Sheet
  5. Order Cancellation Dialog
- **Tasks**: `TASK-08-01` to `TASK-08-11` covering order numbering, atomic snapshot creation, status transitions, device testing.

---

### PHASE 09 — Delivery Calendar
- **Objective**: Visual delivery fulfillment schedule with daily, weekly, and monthly views.
- **Screens**:
  1. Delivery Calendar Screen (Monthly calendar grid with order badges)
  2. Daily Delivery Schedule Screen / Bottom Sheet
  3. Reschedule Delivery Date Dialog
- **Tasks**: `TASK-09-01` to `TASK-09-06` covering date calculations, overdue filters, status triggers, device testing.

---

### PHASE 10 — Payments and Receipts
- **Objective**: Transaction-safe financial recording, partial payments, receipts, and refund/reversal workflows.
- **Screens**:
  1. Record Payment Screen / Bottom Sheet (Cash, UPI, Bank)
  2. Payment History Screen
  3. Digital Receipt Card Screen (Shop header, order info, balance)
  4. Payment Reversal / Refund Dialog
- **Tasks**: `TASK-10-01` to `TASK-10-08` covering financial ledger invariants, receipt generation, Android Sharesheet sharing.

---

### PHASE 11 — Reports and Notifications
- **Objective**: Business intelligence reports (revenue vs bookings, punctuality, popular garments) and notification center.
- **Screens**:
  1. Business Reports Screen (Date-range filters, metrics summary, charts)
  2. Notification Center Screen (Delivery alerts, payment dues)
- **Tasks**: `TASK-11-01` to `TASK-11-06` covering aggregation queries, charts, local notification manager.

---

### PHASE 12 — Staff Management (MVP Basic)
- **Objective**: Staff team member invitations, roles (Owner, Manager, Worker), and audit logs.
- **Screens**:
  1. Staff List Screen
  2. Invite / Add Staff Member Screen
  3. Staff Permission Roles Dialog
- **Tasks**: `TASK-12-01` to `TASK-12-05` covering staff DAOs, permission enforcement, assignment to orders.

---

### PHASE 13 — Subscription & Advertisements
- **Objective**: Free vs Premium tiers, non-intrusive banner placement, feature entitlement gates.
- **Screens**:
  1. Subscription / Upgrade Screen
  2. Non-intrusive Banner Ad wrapper component
- **Tasks**: `TASK-13-01` to `TASK-13-05` covering placement validation (strictly outside creation/payment forms).

---

### PHASE 14 — Supabase Integration & Offline Synchronization
- **Objective**: Full remote cloud sync, PostgreSQL schema migrations, RLS policies, WorkManager workers, idempotent sync.
- **Tasks**:
  - `TASK-14-01`: Formulate SQL migrations with RLS policies for all 14 tables.
  - `TASK-14-02`: Supabase Client initialization and authentication handling.
  - `TASK-14-03`: WorkManager `TailorSyncWorker` implementation for sync queue processing.
  - `TASK-14-04`: WorkManager `ImageUploadWorker` for design photos.
  - `TASK-14-05`: Comprehensive offline-online transition and conflict resolution tests.

---

### PHASE 15 — Complete Testing & Stabilization
- **Objective**: Comprehensive test pass across unit tests, Room DAO tests, UI Compose tests, offline simulation, and physical device test matrix.
- **Tasks**: `TASK-15-01` to `TASK-15-07`.

---

### PHASE 16 — Release Preparation
- **Objective**: Production configuration, ProGuard/R8 rules, app icons, splash screen, privacy policy, APK/AAB build.
- **Tasks**: `TASK-16-01` to `TASK-16-06`.
