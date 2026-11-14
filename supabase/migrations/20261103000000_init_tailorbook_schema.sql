-- ==============================================================================
-- TailorBook — Production PostgreSQL Schema & Row Level Security (RLS) Policies
-- Migration: 20261103000000_init_tailorbook_schema.sql
-- Covers all 14 tables with multi-tenant shop isolation
-- ==============================================================================

-- 0. Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 1. Table Definitions
-- ==============================================================================

-- 1.1 shops (Tenant Root)
CREATE TABLE IF NOT EXISTS shops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    owner_phone TEXT NOT NULL,
    owner_name TEXT NOT NULL,
    address TEXT,
    gstin TEXT,
    logo_url TEXT,
    currency TEXT NOT NULL DEFAULT 'INR',
    locale TEXT NOT NULL DEFAULT 'te-IN',
    subscription_plan TEXT NOT NULL DEFAULT 'FREE' CHECK (subscription_plan IN ('FREE', 'PRO_MONTHLY', 'PRO_YEARLY')),
    subscription_expiry TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.2 users (User Profiles linked to Supabase Auth)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    phone TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'TAILOR' CHECK (role IN ('MASTER_TAILOR', 'TAILOR', 'APPRENTICE', 'ACCOUNTANT')),
    locale TEXT NOT NULL DEFAULT 'te-IN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.3 shop_memberships (Users linked to Shops)
CREATE TABLE IF NOT EXISTS shop_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'TAILOR' CHECK (role IN ('MASTER_TAILOR', 'TAILOR', 'APPRENTICE', 'ACCOUNTANT')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (shop_id, user_id)
);

-- 1.4 customers (Client Profiles)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    gender TEXT CHECK (gender IN ('FEMALE', 'MALE', 'OTHER')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.5 family_members (Linked sub-profiles)
CREATE TABLE IF NOT EXISTS family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    relationship TEXT NOT NULL,
    gender TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.6 measurement_templates (Pre-configured Garment definitions)
CREATE TABLE IF NOT EXISTS measurement_templates (
    id TEXT PRIMARY KEY,
    name_en TEXT NOT NULL,
    name_te TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('WOMEN', 'MEN', 'UNISEX')),
    default_fields_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.7 measurement_versions (Immutable Measurement History)
CREATE TABLE IF NOT EXISTS measurement_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    family_member_id UUID REFERENCES family_members(id) ON DELETE SET NULL,
    template_id TEXT NOT NULL REFERENCES measurement_templates(id),
    version_number INTEGER NOT NULL,
    measurements_json JSONB NOT NULL,
    unit TEXT NOT NULL DEFAULT 'INCH' CHECK (unit IN ('INCH', 'CM')),
    notes TEXT,
    measured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (customer_id, family_member_id, template_id, version_number)
);

-- 1.8 orders (Tailoring Orders)
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    order_number TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'CUTTING', 'STITCHING', 'READY', 'DELIVERED', 'CANCELLED')),
    due_date TIMESTAMPTZ NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    advance_paid NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    balance_due NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    assigned_to UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (shop_id, order_number)
);

-- 1.9 order_items (Garments within an Order)
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    template_id TEXT NOT NULL REFERENCES measurement_templates(id),
    garment_name TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.10 order_measurement_snapshots (Immutable frozen measurements for an order)
CREATE TABLE IF NOT EXISTS order_measurement_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    template_id TEXT NOT NULL REFERENCES measurement_templates(id),
    version_number INTEGER NOT NULL,
    frozen_measurements_json JSONB NOT NULL,
    unit TEXT NOT NULL DEFAULT 'INCH',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.11 order_status_history (Audit Trail of Order Lifecycle)
CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    from_status TEXT,
    to_status TEXT NOT NULL,
    changed_by UUID REFERENCES users(id),
    notes TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.12 payments (Financial Transactions Ledger)
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    receipt_number TEXT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE')),
    transaction_ref TEXT,
    notes TEXT,
    recorded_by UUID REFERENCES users(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (shop_id, receipt_number)
);

-- 1.13 payment_refunds (Refund audit records)
CREATE TABLE IF NOT EXISTS payment_refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    reason TEXT NOT NULL,
    authorized_by UUID REFERENCES users(id),
    refunded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.14 design_references (Design Inspiration Photos & Patterns)
CREATE TABLE IF NOT EXISTS design_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    image_url TEXT NOT NULL,
    tags TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 2. Performance Indexes
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_customers_shop_id ON customers(shop_id);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(shop_id, phone);
CREATE INDEX IF NOT EXISTS idx_family_members_cust ON family_members(customer_id);
CREATE INDEX IF NOT EXISTS idx_measurement_versions_lookup ON measurement_versions(customer_id, template_id);
CREATE INDEX IF NOT EXISTS idx_orders_shop_status ON orders(shop_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_due_date ON orders(shop_id, due_date);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_snapshots_item ON order_measurement_snapshots(order_item_id);
CREATE INDEX IF NOT EXISTS idx_payments_shop_order ON payments(shop_id, order_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_design_refs_shop ON design_references(shop_id);

-- ==============================================================================
-- 3. Multi-Tenant Security & Row Level Security (RLS)
-- ==============================================================================

-- 3.1 Security Definer Function: Resolves shops for authenticated user
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

-- 3.2 Enable RLS on all 14 tables
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE measurement_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE measurement_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_measurement_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_refunds ENABLE ROW LEVEL SECURITY;
ALTER TABLE design_references ENABLE ROW LEVEL SECURITY;

-- ==============================================================================
-- 4. RLS Policies
-- ==============================================================================

-- 4.1 shops
CREATE POLICY "Shop members view their shop" ON shops
    FOR SELECT USING (id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Master tailors update their shop" ON shops
    FOR UPDATE USING (id IN (SELECT current_user_shop_ids()));

-- 4.2 users
CREATE POLICY "Users read own profile" ON users
    FOR SELECT USING (auth_id = auth.uid());
CREATE POLICY "Users update own profile" ON users
    FOR UPDATE USING (auth_id = auth.uid()) WITH CHECK (auth_id = auth.uid());

-- 4.3 shop_memberships
CREATE POLICY "Members view co-workers in same shop" ON shop_memberships
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));

-- 4.4 measurement_templates (Read-only catalog for all authenticated tailors)
CREATE POLICY "Authenticated users view templates" ON measurement_templates
    FOR SELECT TO authenticated USING (true);

-- 4.5 customers
CREATE POLICY "Shop members select customers" ON customers
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert customers" ON customers
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update customers" ON customers
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()))
    WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members delete customers" ON customers
    FOR DELETE USING (shop_id IN (SELECT current_user_shop_ids()));

-- 4.6 family_members
CREATE POLICY "Shop members select family_members" ON family_members
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert family_members" ON family_members
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update family_members" ON family_members
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()))
    WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members delete family_members" ON family_members
    FOR DELETE USING (shop_id IN (SELECT current_user_shop_ids()));

-- 4.7 measurement_versions
CREATE POLICY "Shop members select measurement_versions" ON measurement_versions
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert measurement_versions" ON measurement_versions
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- 4.8 orders
CREATE POLICY "Shop members select orders" ON orders
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert orders" ON orders
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update orders" ON orders
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()))
    WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- 4.9 order_items
CREATE POLICY "Shop members select order_items" ON order_items
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert order_items" ON order_items
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update order_items" ON order_items
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()));

-- 4.10 order_measurement_snapshots
CREATE POLICY "Shop members select order_measurement_snapshots" ON order_measurement_snapshots
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert order_measurement_snapshots" ON order_measurement_snapshots
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- 4.11 order_status_history
CREATE POLICY "Shop members select order_status_history" ON order_status_history
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert order_status_history" ON order_status_history
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- 4.12 payments
CREATE POLICY "Shop members select payments" ON payments
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert payments" ON payments
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- 4.13 payment_refunds
CREATE POLICY "Shop members select payment_refunds" ON payment_refunds
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert payment_refunds" ON payment_refunds
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));

-- 4.14 design_references
CREATE POLICY "Shop members select design_references" ON design_references
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert design_references" ON design_references
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members delete design_references" ON design_references
    FOR DELETE USING (shop_id IN (SELECT current_user_shop_ids()));

-- ==============================================================================
-- 5. Default Measurement Templates Seed
-- ==============================================================================

INSERT INTO measurement_templates (id, name_en, name_te, category, default_fields_json)
VALUES
('BLOUSE', 'Blouse', 'బ్లౌజ్', 'WOMEN', '[
    {"key": "length", "name_en": "Length", "name_te": "పొడవు", "default_val": 14.0},
    {"key": "chest", "name_en": "Bust / Chest", "name_te": "ఛాతీ", "default_val": 36.0},
    {"key": "waist", "name_en": "Waist", "name_te": "నడుము", "default_val": 30.0},
    {"key": "shoulder", "name_en": "Shoulder", "name_te": "భుజం", "default_val": 14.0},
    {"key": "front_neck", "name_en": "Front Neck", "name_te": "ముందు మెడ", "default_val": 6.5},
    {"key": "back_neck", "name_en": "Back Neck", "name_te": "వెనుక మెడ", "default_val": 8.0},
    {"key": "armhole", "name_en": "Armhole", "name_te": "చంక చుట్టు", "default_val": 16.0},
    {"key": "sleeve_length", "name_en": "Sleeve Length", "name_te": "చేయి పొడవు", "default_val": 10.0},
    {"key": "sleeve_round", "name_en": "Sleeve Round", "name_te": "చేయి లూజు", "default_val": 12.0},
    {"key": "apex_length", "name_en": "Apex / Point Length", "name_te": "డాట్ పొడవు", "default_val": 10.0},
    {"key": "apex_width", "name_en": "Apex Width", "name_te": "డాట్ల వెడల్పు", "default_val": 7.0}
]'::jsonb),
('KURTI', 'Kurti / Kameez', 'కుర్తీ / కమీజ్', 'WOMEN', '[
    {"key": "length", "name_en": "Length", "name_te": "పొడవు", "default_val": 40.0},
    {"key": "bust", "name_en": "Bust", "name_te": "ఛాతీ", "default_val": 36.0},
    {"key": "waist", "name_en": "Waist", "name_te": "నడుము", "default_val": 32.0},
    {"key": "hip", "name_en": "Hip", "name_te": "సీటు లూజు", "default_val": 38.0},
    {"key": "shoulder", "name_en": "Shoulder", "name_te": "భుజం", "default_val": 14.5},
    {"key": "armhole", "name_en": "Armhole", "name_te": "చంక చుట్టు", "default_val": 16.5},
    {"key": "sleeve_length", "name_en": "Sleeve Length", "name_te": "చేయి పొడవు", "default_val": 16.0},
    {"key": "slit_length", "name_en": "Side Slit Length", "name_te": "కట్స్ పొడవు", "default_val": 14.0}
]'::jsonb),
('SHIRT', 'Shirt', 'షర్ట్', 'MEN', '[
    {"key": "length", "name_en": "Length", "name_te": "పొడవు", "default_val": 29.0},
    {"key": "chest", "name_en": "Chest", "name_te": "ఛాతీ", "default_val": 40.0},
    {"key": "shoulder", "name_en": "Shoulder", "name_te": "భుజం", "default_val": 18.0},
    {"key": "sleeve_length", "name_en": "Sleeve Length", "name_te": "చేయి పొడవు", "default_val": 24.5},
    {"key": "collar", "name_en": "Collar", "name_te": "కాలర్", "default_val": 15.5}
]'::jsonb),
('PANTS', 'Pants / Trousers', 'ప్యాంట్ / ట్రౌజర్స్', 'MEN', '[
    {"key": "length", "name_en": "Length", "name_te": "పొడవు", "default_val": 40.0},
    {"key": "waist", "name_en": "Waist", "name_te": "నడుము", "default_val": 34.0},
    {"key": "hip", "name_en": "Hip / Seat", "name_te": "సీటు లూజు", "default_val": 40.0},
    {"key": "thigh", "name_en": "Thigh", "name_te": "తొడ లూజు", "default_val": 24.0},
    {"key": "bottom", "name_en": "Bottom / Hem", "name_te": "బాటమ్ లూజు", "default_val": 15.0}
]'::jsonb)
ON CONFLICT (id) DO NOTHING;
