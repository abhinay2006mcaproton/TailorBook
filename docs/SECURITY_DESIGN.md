# TailorBook — Security & Tenant Isolation Design

## 1. Security Architecture Principles
1. **Multi-Tenant Isolation**: TailorBook operates as a multi-tenant platform where each tailoring shop is an isolated tenant. Under no circumstances may data from Shop A be visible or mutable by Shop B.
2. **Zero Trust in the Client**: The Android application only uses the public Supabase Anon key and passes the authenticated user's JWT. All row-level data access rules are strictly enforced by the PostgreSQL database engine.
3. **No Secret Keys in App**: Supabase `service_role` keys are strictly prohibited in the Android source code or compiled APK/AAB binaries.
4. **Audit Trail**: Financial transactions and order status transitions are permanently stamped with the acting user's ID and timestamp.

---

## 2. Row Level Security (RLS) Policies (PostgreSQL)

Every PostgreSQL table containing `shop_id` has RLS enabled:
```sql
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE measurement_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE design_references ENABLE ROW LEVEL SECURITY;
```

### 2.1 Shop Membership Helper Function
```sql
CREATE OR REPLACE FUNCTION current_user_shop_ids()
RETURNS TABLE (shop_id UUID) AS $$
BEGIN
    RETURN QUERY
    SELECT sm.shop_id
    FROM shop_memberships sm
    JOIN users u ON u.id = sm.user_id
    WHERE u.auth_id = auth.uid()
      AND sm.is_active = TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 2.2 Customer & Order Table Policies
```sql
-- Read Policy
CREATE POLICY "Allow shop members to view shop customers"
ON customers FOR SELECT
USING (shop_id IN (SELECT current_user_shop_ids()));

-- Insert Policy
CREATE POLICY "Allow shop members to insert customers"
ON customers FOR INSERT
WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- Update Policy
CREATE POLICY "Allow shop members to update customers"
ON customers FOR UPDATE
USING (shop_id IN (SELECT current_user_shop_ids()))
WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
```

---

## 3. Role-Based Access Control (RBAC)

| Role | Permissions |
| :--- | :--- |
| **OWNER** | Full CRUD on all shop entities, invite/revoke staff, view business revenue reports, manage subscription, delete shop. |
| **MANAGER** | Full CRUD on customers, measurements, orders, and payments; view operational reports. Cannot alter shop billing or delete the shop. |
| **TAILOR / WORKER** | Read assigned orders and measurement snapshots; update order status (`CUTTING`, `STITCHING`, `FINISHING`, `READY`); cannot record payments or alter prices. |

---

## 4. Local Storage Security
- **Encrypted SharedPreferences / Encrypted DataStore**: Used for storing the Supabase Auth session token, user refresh token, and local encryption keys via Android Keystore.
- **Room SQLite Integrity**: Database file stored exclusively in app-private sandbox directory (`context.getDatabasePath()`).
- **Safeguarding Customer Contact Info**: Customer phone numbers and addresses are stored in local sandbox and never transmitted to third-party ad networks or untrusted analytics.
