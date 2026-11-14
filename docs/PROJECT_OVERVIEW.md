# TailorBook — Project Overview

## 1. Executive Summary
**TailorBook** is an offline-first Android application designed specifically for independent tailors, home-based tailoring micro-businesses, and premium fashion boutiques across India.

In the tailoring industry, paper-based measurement notebooks, loose order slips, and scattered WhatsApp chats lead to lost customer measurements, misplaced design instructions, delivery delays, cash reconciliation errors, and missed revenue. TailorBook replaces these fragmented workflows with an intuitive, unified digital business management application.

TailorBook is built as a production-grade commercial product with enterprise-level data integrity, transaction safety, robust offline functionality, and bilingual localization (English and Telugu).

---

## 2. Core Value Proposition
- **Guaranteed Offline-First Operations**: Tailors often work in areas with intermittent cellular connectivity or basements. Every operational workflow (measurements, orders, customer lookup, payments) works completely offline using an embedded local database (Room SQLite).
- **Measurement Integrity & Version History**: Customer bodies change over time. TailorBook preserves full measurement version history and creates immutable snapshots for every order. Existing orders are never retroactively corrupted by new measurements.
- **Auditable Financial Ledger**: Stitching orders involve advances, fabric costs, partial payments, and delivery balances. TailorBook enforces transaction-safe, idempotent financial logging with clear distinctions between booked order value and actual cash/UPI collected.
- **Family Profiles**: In Indian tailoring, one customer (e.g., household head or mother) routinely manages stitching orders for an entire family. TailorBook natively organizes family members with individual measurement profiles under a single billing contact.
- **WhatsApp Integration via Android Sharesheet**: Tailors communicate updates, balance reminders, and delivery alerts directly via WhatsApp without requiring costly third-party SMS gateways.
- **Cloud Sync & Data Safety**: All local data synchronizes bidirectionally with a Supabase PostgreSQL backend when internet connectivity is available, guaranteeing complete data backup against phone loss, damage, or theft.

---

## 3. Product Scope & Functional Modules

| Module | Purpose | Key Capabilities |
| :--- | :--- | :--- |
| **Shop Setup & Auth** | Shop onboarding and profile | Phone/Email authentication, shop profile, business type, language selection (English/Telugu). |
| **Dashboard** | Daily business pulse | Customer counts, active orders, today's deliveries, pending balance sum, quick actions, sync status. |
| **Customer Management** | CRM and directory | Add/edit customer, phone search, WhatsApp/call links, family member grouping, order & payment history. |
| **Family Profiles** | Multi-person households | Linked sub-profiles per customer with individual measurements and garment preferences. |
| **Measurement Management** | Garment sizing repository | 8 garment templates (Blouse, Shirt, T-shirt, Pants, Kurti, Dress, Churidar, Custom), version history, comparisons, immutable snapshots. |
| **Design Gallery** | Visual references | Photo capture/import, pattern annotations, tagging, attachments to orders and customer profiles. |
| **Order Management** | Production lifecycle | Order intake, unique order ID, garment configuration, measurement snapshot, advance payment, status pipeline. |
| **Delivery Calendar** | Fulfillment schedule | Daily/weekly/monthly views, overdue order alerts, delivery rescheduling, one-click fulfillment. |
| **Payments & Balances** | Financial management | Cash, UPI, and bank recording, advance tracking, receipt generation, refund/reversal workflows, balance calculation. |
| **Business Reports** | Operational intelligence | Revenue received vs. order value, top garments, delivery punctuality, monthly order counts. |
| **Staff Management** | Team access control | Role assignment (Owner, Tailor/Worker, Counter Staff), shop access, audit logs for order changes. |
| **Notifications** | Timely reminders | Overdue deliveries, upcoming delivery alerts, payment balance follow-ups, sync warnings. |
| **Settings & Backup** | Configuration & safety | Measurement units (inches/cm), language toggle, local database export, cloud sync status, account security. |
| **Subscription & Ads** | Business monetization | Free tier with nonintrusive ads (strictly outside creation forms), premium tier hypothesis (₹149/mo, ₹1199/yr) for multi-device & cloud backup. |

---

## 4. Technology Stack Summary
- **Client OS**: Android 8.0+ (API level 26 minimum, API 34 target).
- **Programming Language**: Kotlin 1.9+ / Gradle Kotlin DSL.
- **UI Framework**: Jetpack Compose with Material 3.
- **Architecture**: Clean MVVM with Coroutines, Kotlin Flow, Navigation Compose.
- **Local Persistence**: Android Room (SQLite) with multi-table relationships and migration tests.
- **Background Operations**: Android WorkManager for idempotent sync and file uploads.
- **Cloud Backend**: Supabase (PostgreSQL, Supabase Auth, Supabase Storage, PostgreSQL RLS).
- **Future Backend Readiness**: Clean abstraction layer ready to connect to ASP.NET Core Web API (C#) if required.

---

## 5. Development Governance & Standards
1. **Phased Execution**: 17 structured phases (Phase 00 through Phase 16).
2. **One-Task-At-A-Time**: Strict sequential execution with verification evidence before moving forward.
3. **No False Completion**: Only verified code with automated tests and physical device testing is marked complete.
4. **Physical Device Testing**: Every UI screen and functional iteration must be verified on a real Android device using Wireless ADB or USB debugging.
