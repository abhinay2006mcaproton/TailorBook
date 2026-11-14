# TailorBook — Production Release Checklist & Play Store Playbook

This document defines the strict quality, compliance criteria, and instructions for deploying TailorBook to Google Play Store production.

---

## 1. Code Quality & Optimization (Option 1 - Completed)
- [x] Multi-module clean architecture maintained (`core/*`, `feature/*`).
- [x] R8 full-mode code shrinking & minification enabled (`isMinifyEnabled = true`).
- [x] Resource shrinking enabled (`isShrinkResources = true`).
- [x] Unused third-party locale stripping configured (`resourceConfigurations += ["en", "te"]`).
- [x] Timber logging stripped from release bytecode via ProGuard rules (`-assumenosideeffects class timber.log.Timber`).
- [x] Room DAOs, databases, and Kotlinx Serialization data classes guarded against obfuscation in `proguard-rules.pro`.

---

## 2. Data Integrity, Offline Functionality & Backup (Option 3 - Completed)
- [x] 100% of core actions (Customers, Measurements, Orders, Payments) work completely offline.
- [x] Room entity search indices optimized (`[shopId, primaryPhone]`, `[shopId, name]`, etc.).
- [x] Historical measurement versions strictly immutable.
- [x] Offline database backup export to phone (`OfflineBackupHelper.kt`) via secure `FileProvider`.
- [x] Sharesheet integration: tailors can export their full customer/order database to Downloads folder, Google Drive, or WhatsApp.
- [x] Financial calculations verified with zero rounding loss.
- [x] Supabase PostgreSQL RLS policies verified to prevent cross-tenant data leakage.

---

## 3. UI/UX, Haptics & Localization (Option 2 - Completed)
- [x] "Premium Boutique" design system tokens consistently applied (`DeepTeal`, `WarmIvory`, `SoftGold`, `Charcoal`).
- [x] Indian numbering format for currency (`₹500`, `₹1,500`, `₹25,000`, `₹1,50,000`) implemented and unit tested.
- [x] English and Telugu (తెలుగు) strings 100% translated; zero missing translations.
- [x] Tactile haptic feedback (`HapticHelper.kt`) added on primary customer and order workflow actions.
- [x] Ad placement rules strictly verified: Zero ads during order creation, measurement recording, or payment collection.

---

## 4. Security & Compliance (Option 4 - Completed)
- [x] Zero Supabase service-role or secret keys bundled in the APK/AAB.
- [x] Customer phone numbers and personal data protected in app-private sandbox storage.
- [x] Google Play User Messaging Platform (UMP) consent SDK integrated for GDPR/EEA & global ad consent.
- [x] Official Privacy Policy webpage created (`docs/privacy-policy.html` and `privacy-policy.html`).
- [x] In-app Privacy Policy dialog updated with direct "View Online" web action.
- [x] Target SDK set to 34 (complies with Google Play 2024/2026 requirements).

---

## 5. How to Build the Production Release Bundle (`.aab`)

When you are ready to produce the production release bundle for the Google Play Console:

### Step 1: Ensure Keystore is Available
The release keystore is configured at `keystore/release.jks`.
Default / Environment configuration:
```powershell
$env:KEYSTORE_PASSWORD = "your_keystore_password"
$env:KEY_ALIAS = "your_key_alias"
$env:KEY_PASSWORD = "your_key_password"
```

### Step 2: Run the Release Bundle Command
Run the following Gradle command from the root directory:
```powershell
.\gradlew.bat --no-daemon bundleRelease
```
The resulting Google Play upload bundle will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

*(Note: If you also need a standalone release APK for direct testing, run `.\gradlew.bat --no-daemon assembleRelease`).*

---

## 6. Google Play Console Listing Guide

1. **App Title**: `TailorBook: Boutique Manager`
2. **Short Description**: `Digital ledger, measurements, orders, and UPI receipts for tailoring boutiques.`
3. **Privacy Policy URL**:
   Host `docs/privacy-policy.html` on GitHub Pages (e.g., `https://<your-username>.github.io/<repo-name>/privacy-policy.html`) or your custom domain and paste the HTTPS URL into Google Play Console &rarr; Policy &rarr; App content &rarr; Privacy policy.
4. **Target Audience**: Business / Productivity (Ages 18+).
5. **Ads Declaration**: Select **"Yes, my app contains ads"** (handled via AdMob and Google UMP).
6. **In-App Purchases**: In Google Play Console &rarr; Monetization &rarr; Products &rarr; Subscriptions, add product IDs matching `PlayBillingManagerImpl.kt`:
   - `tailorbook_pro_monthly` (₹199 / month)
   - `tailorbook_pro_yearly` (₹1,499 / year)
