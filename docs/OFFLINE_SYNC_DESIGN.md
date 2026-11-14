# TailorBook — Offline-First & Data Synchronization Design

## 1. Offline-First Principles
TailorBook treats the local Room SQLite database as the immediate and authoritative source for user interaction. Every write, read, and search operation completes in milliseconds against the local database without waiting for network I/O.

```
[User Action in Compose UI]
            │
            ▼
[Repository writes to Room DB & inserts into sync_queue in single Room transaction]
            │
            ├─────────────────────────────────────────┐
            ▼                                         ▼
[UI updates instantly via Flow]        [WorkManager TailorSyncWorker Triggered]
                                                      │
                                                      ▼
                                       [Check Network Connectivity]
                                                      │
                                                      ├── (No Network) ──> Suspend until connected
                                                      │
                                                      ▼ (Online)
                                       [Read sync_queue items FIFO]
                                                      │
                                                      ▼
                                       [Push changes to Supabase via RLS]
                                                      │
                                                      ▼
                                       [On 200 OK: Mark SYNCED or Delete queue item]
                                                      │
                                                      ▼
                                       [Pull updated remote records into Room]
```

---

## 2. Sync Queue & Mutation Mechanics
Whenever an offline or online record mutation occurs:
1. The domain entity is saved to Room (e.g., `CustomerEntity`).
2. A `SyncQueueEntity` is created:
   - `id`: UUID (Primary Key)
   - `entity_type`: `CUSTOMER`, `ORDER`, `PAYMENT`, `MEASUREMENT`
   - `entity_id`: UUID
   - `operation`: `INSERT`, `UPDATE`, `DELETE`
   - `payload_json`: Full entity snapshot in JSON format
   - `created_at`: Epoch timestamp
   - `status`: `PENDING`
3. Both operations execute inside an atomic Room transaction:
   ```kotlin
   database.withTransaction {
       customerDao.insert(customer)
       syncQueueDao.enqueue(syncRecord)
   }
   ```

---

## 3. Conflict Resolution Strategy
Blind "Last-Write-Wins" (LWW) is strictly forbidden for financial and measurement records. TailorBook employs deterministic conflict resolution rules:

### 3.1 Financial Transactions (`payments` & `orders`)
- **Additive Ledger**: Payments are immutable append-only records. Duplicate submissions with the same `id` (client UUID) are treated as no-ops by Supabase PostgreSQL (Idempotent `ON CONFLICT (id) DO NOTHING`).
- Payment balance adjustments recalculate from the sum of valid payment records.

### 3.2 Measurements (`measurement_versions`)
- Measurement records are versioned (`version_number`). If a client creates `version_number = 3` and the server already has `version_number = 3` from another device, the incoming version is automatically incremented or flagged for user review. Historical versions are never overwritten.

### 3.3 Customer Demographics & Orders
- Field-level merge with server timestamp checking:
  - If remote `updated_at > local.updated_at` and fields differ, the more recent edit is preserved, but any conflicting local notes are appended rather than discarded.

---

## 4. Background Sync with Android WorkManager
- Sync tasks are managed using `PeriodicWorkRequestBuilder<TailorSyncWorker>` (every 15 minutes) and `OneTimeWorkRequestBuilder<TailorSyncWorker>` (triggered immediately on network reconnection).
- Network constraint: `NetworkType.CONNECTED`.
- Backoff policy: Exponential backoff starting at 10 seconds, doubling up to a maximum of 1 hour on repeated server errors (HTTP 5xx).
- HTTP 4xx (Authentication / validation) errors move the queue record to `BLOCKED` status and notify the user rather than retrying indefinitely.

---

## 5. Media (Photo) Upload Queue
- Design reference photos captured offline are stored locally in app-private storage: `context.filesDir/designs/<id>.jpg`.
- A dedicated `ImageUploadWorker` uploads photos sequentially to Supabase Storage bucket (`shop-designs/<shop_id>/<id>.jpg`).
- Once confirmed uploaded, `remote_storage_url` is updated in both Room and Supabase, and local thumbnail caching is preserved.
