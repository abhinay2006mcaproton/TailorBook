-- ==============================================================================
-- TailorBook — Device Registration & Quota Tracking Schema
-- Migration: 20261111000000_device_registrations.sql
-- Enforces 1 Introductory Free Starter Quota (10 customers) per physical Android device (via ANDROID_ID)
-- Multiple accounts can be registered on the same phone, but subsequent accounts start at 0 free slots
-- and must watch rewarded video ads to add customers.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.device_registrations (
    device_id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    shop_name TEXT,
    free_starter_claimed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_device_registrations_email ON public.device_registrations(email);

-- Enable Row Level Security (RLS)
ALTER TABLE public.device_registrations ENABLE ROW LEVEL SECURITY;

-- Allow anonymous and authenticated clients to read device registrations (for quota check)
DROP POLICY IF EXISTS "Allow public read for device duplicate check" ON public.device_registrations;
CREATE POLICY "Allow public read for device duplicate check" ON public.device_registrations
    FOR SELECT
    TO anon, authenticated
    USING (true);

-- Allow anonymous and authenticated clients to insert / update device registrations
DROP POLICY IF EXISTS "Allow public insert for device registration" ON public.device_registrations;
CREATE POLICY "Allow public insert for device registration" ON public.device_registrations
    FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);

DROP POLICY IF EXISTS "Allow public update for device registration" ON public.device_registrations;
CREATE POLICY "Allow public update for device registration" ON public.device_registrations
    FOR UPDATE
    TO anon, authenticated
    USING (true);
