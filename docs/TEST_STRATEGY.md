# TailorBook — Test Strategy & Verification Protocol

## 1. Quality Policy & Acceptance Gates
A task or screen cannot be marked **VERIFIED** merely because the code compiles. Every feature must pass the three-tier verification protocol:

```
[Level 1: Automated Unit & Math Tests]
                 │
                 ▼
[Level 2: Room Database & Integration Tests]
                 │
                 ▼
[Level 3: Physical Android Device Verification via Wireless/USB ADB]
```

---

## 2. Test Tiers & Methodologies

### 2.1 Unit Testing (Local JVM)
- **Scope**: Business logic, mathematical calculations, validation, state machines.
- **Key Suites**:
  - `PaymentCalculationTest`: Verifies partial payment sum, advance subtraction, discount calculations, exact balance math, decimal precision without rounding errors.
  - `MeasurementVersionTest`: Verifies that adding new measurements increments version numbers and never mutates historical snapshots.
  - `OrderStatusTransitionTest`: Validates legal status transitions (`NEW` -> `CUTTING` -> `STITCHING` -> `FINISHING` -> `READY` -> `DELIVERED`, and cancellation restrictions).
  - `PhoneValidationTest`: Validates 10-digit Indian mobile numbers (+91, leading 0, prefixes 6-9).

### 2.2 Database Integration Testing (Room In-Memory)
- **Scope**: Room DAOs, queries, foreign key cascade behaviors, and transactions.
- **Key Suites**:
  - `CustomerDaoTest`: Insertion, search query matching (name & phone prefix), soft-delete behavior.
  - `OrderSnapshotTest`: Insertion of multi-item order with measurement JSON snapshot; verifies data persistence independently of measurement entity changes.
  - `SyncQueueDaoTest`: Enqueuing mutations, FIFO retrieval, status updates (`PENDING` -> `SYNCING` -> `SYNCED`).

### 2.3 Offline-First Simulation Testing
- **Scenario**:
  1. Record customer while device network is disabled.
  2. Record 2 measurements (Blouse, Kurti).
  3. Create stitching order using measurement snapshot.
  4. Record partial cash payment of ₹500.
  5. Restart the application process completely (`adb shell am force-stop`).
  6. Relaunch app: verify all records persist and balance matches exact calculation.
  7. Re-enable network: verify WorkManager drains sync queue idempotently without duplicating financial records.

### 2.4 Physical Device UI & Interaction Testing (Wireless ADB)
- **Scope**: Verified on the user's real Android smartphone.
- **Checklist per Screen**:
  - Proper font rendering for both English and Telugu script.
  - Correct soft keyboard behaviors (decimal numeric pad for measurements and money).
  - Navigation backstack behavior (Android system back gesture/button does not corrupt state or crash).
  - Small screen responsiveness (no truncated text or clipped action buttons on narrow displays).
  - Android Sharesheet opens and formats text/receipt for WhatsApp correctly.

### 2.5 Multi-Version Android Compatibility Matrix
Verification must be conducted on:
1. **Target Android Version**: Android 14 (API 34) or Android 15 (API 35).
2. **Back Version 1**: Android 13 (API 33 - Tiramisu) — verifies runtime `POST_NOTIFICATIONS` and modern photo picker.
3. **Back Version 2**: Android 12 / 12L (API 31/32) or Android 11 (API 30) — verifies legacy notification fallback, SAF storage access, and backward-compatible edge-to-edge UI.
