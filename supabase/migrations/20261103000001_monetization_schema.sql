-- ==============================================================================
-- TailorBook — Monetization, Entitlements & Subscription Schema
-- Migration: 20261103000001_monetization_schema.sql
-- Covers Google AdMob Rewarded Video verification, Google Play In-App Subscriptions,
-- Customer Entitlement limits (1-10 Free, 11-50 Ads, 51+ Premium), and VIP Free Access
-- ==============================================================================

-- 1. Table Definitions

-- 1.1 customer_entitlements (Tracks tier limit and unlock status per shop tenant)
CREATE TABLE IF NOT EXISTS customer_entitlements (
    shop_id UUID PRIMARY KEY REFERENCES shops(id) ON DELETE CASCADE,
    tier TEXT NOT NULL DEFAULT 'FREE_STARTER' CHECK (tier IN ('FREE_STARTER', 'AD_SUPPORTED', 'PREMIUM', 'VIP_COMPLIMENTARY')),
    max_customers INTEGER NOT NULL DEFAULT 10,
    unlocked_ad_slots INTEGER NOT NULL DEFAULT 0,
    is_unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    is_vip_active BOOLEAN NOT NULL DEFAULT FALSE,
    vip_granted_by TEXT,
    vip_granted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.2 reward_transactions (Single-use verified AdMob rewarded ad tokens)
CREATE TABLE IF NOT EXISTS reward_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    reward_token TEXT UNIQUE NOT NULL,
    ad_unit_id TEXT NOT NULL,
    reward_type TEXT NOT NULL DEFAULT 'CUSTOMER_SLOT',
    reward_amount INTEGER NOT NULL DEFAULT 1,
    is_consumed BOOLEAN NOT NULL DEFAULT FALSE,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.3 shop_subscriptions (Google Play in-app purchases & VIP subscriptions)
CREATE TABLE IF NOT EXISTS shop_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    product_id TEXT NOT NULL,
    purchase_token TEXT UNIQUE NOT NULL,
    order_id TEXT,
    purchase_state TEXT NOT NULL DEFAULT 'PURCHASED',
    is_acknowledged BOOLEAN NOT NULL DEFAULT TRUE,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'PENDING', 'GRACE_PERIOD')),
    starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    is_vip_complimentary BOOLEAN NOT NULL DEFAULT FALSE,
    vip_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.4 vip_whitelisted_accounts (Selected users designated for lifetime free access)
CREATE TABLE IF NOT EXISTS vip_whitelisted_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone TEXT UNIQUE,
    email TEXT UNIQUE,
    shop_name TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Pre-seed approved VIP development / admin accounts
INSERT INTO vip_whitelisted_accounts (phone, email, shop_name, notes)
VALUES
('9876543210', 'owner@tailorbook.com', 'Kethepalle Tailoring', 'Master owner account'),
('9848012345', 'vip@tailorbook.com', 'Boutique Designer', 'VIP early partner tailor')
ON CONFLICT DO NOTHING;

-- ==============================================================================
-- 2. Indexes for High Performance Queries
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_reward_transactions_shop ON reward_transactions(shop_id);
CREATE INDEX IF NOT EXISTS idx_reward_transactions_token ON reward_transactions(reward_token);
CREATE INDEX IF NOT EXISTS idx_shop_subscriptions_shop ON shop_subscriptions(shop_id);
CREATE INDEX IF NOT EXISTS idx_shop_subscriptions_status ON shop_subscriptions(shop_id, status);
CREATE INDEX IF NOT EXISTS idx_vip_whitelist_phone ON vip_whitelisted_accounts(phone);
CREATE INDEX IF NOT EXISTS idx_vip_whitelist_email ON vip_whitelisted_accounts(email);

-- ==============================================================================
-- 3. Row-Level Security (RLS) Policies
-- ==============================================================================

ALTER TABLE customer_entitlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE reward_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE vip_whitelisted_accounts ENABLE ROW LEVEL SECURITY;

-- 3.1 customer_entitlements policies
CREATE POLICY "Shop members view customer_entitlements" ON customer_entitlements
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert customer_entitlements" ON customer_entitlements
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update customer_entitlements" ON customer_entitlements
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()));

-- 3.2 reward_transactions policies
CREATE POLICY "Shop members view reward_transactions" ON reward_transactions
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert reward_transactions" ON reward_transactions
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update reward_transactions" ON reward_transactions
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()));

-- 3.3 shop_subscriptions policies
CREATE POLICY "Shop members view shop_subscriptions" ON shop_subscriptions
    FOR SELECT USING (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members insert shop_subscriptions" ON shop_subscriptions
    FOR INSERT WITH CHECK (shop_id IN (SELECT current_user_shop_ids()));
CREATE POLICY "Shop members update shop_subscriptions" ON shop_subscriptions
    FOR UPDATE USING (shop_id IN (SELECT current_user_shop_ids()));

-- 3.4 vip_whitelisted_accounts policies (Read-only for authenticated users to verify eligibility)
CREATE POLICY "Authenticated users view vip_whitelisted_accounts" ON vip_whitelisted_accounts
    FOR SELECT USING (auth.role() = 'authenticated');

-- ==============================================================================
-- 4. Server-Side Atomic Stored Procedures
-- ==============================================================================

-- 4.1 Atomically verifies and consumes a rewarded video token
CREATE OR REPLACE FUNCTION verify_and_consume_reward_token(
    p_shop_id UUID,
    p_reward_token TEXT,
    p_customer_id UUID DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_found_id UUID;
    v_is_consumed BOOLEAN;
BEGIN
    SELECT id, is_consumed INTO v_found_id, v_is_consumed
    FROM reward_transactions
    WHERE shop_id = p_shop_id AND reward_token = p_reward_token
    FOR UPDATE;

    IF v_found_id IS NULL THEN
        -- Token does not exist
        RETURN FALSE;
    END IF;

    IF v_is_consumed THEN
        -- Token already used
        RETURN FALSE;
    END IF;

    -- Mark token as consumed
    UPDATE reward_transactions
    SET is_consumed = TRUE,
        consumed_at = NOW(),
        customer_id = p_customer_id
    WHERE id = v_found_id;

    -- Increment unlocked ad slots in entitlements table
    UPDATE customer_entitlements
    SET unlocked_ad_slots = unlocked_ad_slots + 1,
        tier = 'AD_SUPPORTED',
        updated_at = NOW()
    WHERE shop_id = p_shop_id;

    RETURN TRUE;
END;
$$;
