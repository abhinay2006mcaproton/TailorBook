# TailorBook — Product Requirements Document (PRD)

## 1. Introduction
This document defines the functional and non-functional requirements for the MVP release of **TailorBook**, an offline-first Android application designed for Indian tailoring shops, home boutiques, and independent master tailors.

---

## 2. Target Users & Personas
1. **Ramesh (Independent Master Tailor)**: Works in a neighborhood shop; relies on memory and physical books; struggles with lost paper slips and disputes with customers regarding measurements or agreed delivery dates.
2. **Kavitha (Home Boutique Owner)**: Stitches designer kurtis, blouses, and dresses; manages 100+ regular customers; manages complex customization photos sent via WhatsApp and needs to attach them directly to orders.
3. **Suresh (Multi-worker Tailoring Unit)**: Employs 2-3 cutting masters and stitchers; needs to track order statuses (Cutting, Stitching, Finishing, Ready, Delivered) and staff responsibility.

---

## 3. Functional Requirements by Module

### A. Authentication & Shop Setup
- **REQ-AUTH-01**: Shop Registration with shop name, owner name, mobile number, business category (Gent's Tailor, Ladies Boutique, Unisex, Alteration Specialist).
- **REQ-AUTH-02**: Authentication via Mobile Phone (OTP / Supabase Auth) or Email/Password fallback.
- **REQ-AUTH-03**: Language selection during onboarding: English or Telugu (తెలుగు), switchable anytime in Settings.
- **REQ-AUTH-04**: Offline Session Persistence: The user remains authenticated locally via encrypted SharedPreferences / DataStore even when internet is completely disconnected.
- **REQ-AUTH-05**: Shop Profile configuration: address, shop logo, contact number, default measurement unit (Inches / Centimeters), standard delivery lead days.

### B. Dashboard
- **REQ-DASH-01**: Summary metrics cards:
  - Total Active Orders
  - Orders Due Today & Overdue Orders
  - Ready for Delivery
  - Outstanding Balance Sum (₹)
- **REQ-DASH-02**: Quick Action Floating/Grid buttons: New Customer, New Order, New Measurement, Record Payment.
- **REQ-DASH-03**: Today's Deliveries carousel/list with instant call/WhatsApp actions.
- **REQ-DASH-04**: Sync Status & Offline Indicator banner displaying whether the device is offline, syncing, or synchronized.
- **REQ-DASH-05**: Non-intrusive banner ad area compliant with placement rules (strictly non-blocking).

### C. Customer Management
- **REQ-CUST-01**: Add Customer with Name, Primary Mobile Number, Secondary Phone, Address, Notes, and Gender.
- **REQ-CUST-02**: Instant Customer Search by Name and 10-digit Mobile Number with fuzzy and prefix matching.
- **REQ-CUST-03**: Customer Profile View:
  - Total orders placed & lifetime value
  - Current outstanding balance
  - Family members linked
  - Measurement history
  - Order timeline
- **REQ-CUST-04**: Direct Quick Actions: "Call Customer" (initiates standard Android dialer) and "WhatsApp Customer" (opens WhatsApp chat with prefilled greetings).
- **REQ-CUST-05**: Delete/Archive Customer: Soft deletion with foreign key safeguards (cannot hard delete if orders or pending balances exist).

### D. Family Profiles
- **REQ-FAM-01**: Link multiple family members (e.g., Wife, Daughter, Son, Mother) under a single primary customer account.
- **REQ-FAM-02**: Each family member possesses their own name, relationship tag, individual measurement profiles, and individual order history.
- **REQ-FAM-03**: Unified search: Searching a family member's name surfaces the parent customer account with direct access to that member.

### E. Measurement Management
- **REQ-MEAS-01**: Dedicated templates with standard industry measurement fields:
  1. **Blouse**: Length, Bust/Chest, Upper Chest, Waist, Shoulder, Front Neck, Back Neck, Armhole, Sleeve Length, Sleeve Round, Cross Front, Cross Back, Apex/Point Length, Apex Width.
  2. **Shirt**: Length, Chest, Waist, Hip, Shoulder, Sleeve Length, Collar/Neck, Cuff, Bicep.
  3. **T-shirt**: Length, Chest, Shoulder, Sleeve Length, Neck.
  4. **Pants/Trousers**: Length, Waist, Hip, Inseam, Thigh, Knee, Bottom/Cuff, Rise, Fork.
  5. **Kurti/Kameez**: Length, Bust, Waist, Hip, Shoulder, Armhole, Sleeve Length, Sleeve Round, Front Neck, Back Neck, Slit Length.
  6. **Dress**: Full Length, Upper Body Length, Bust, Waist, Hip, Flare, Shoulder, Sleeve Length.
  7. **Churidar/Salwar**: Length, Waist/Belt, Hip, Thigh, Knee, Calf, Ankle Opening.
  8. **Custom Garment**: Dynamic key-value pairs with custom labels.
- **REQ-MEAS-02**: Numeric validation with fractional support (e.g., 14.5 or 14 1/2 inches) and unit toggle (Inches / cm).
- **REQ-MEAS-03**: Visual diagrams and field-level tooltips explaining how to measure each dimension.
- **REQ-MEAS-04**: **Measurement Versioning (CRITICAL)**:
  - Whenever measurements are updated, a new immutable version (v1, v2, v3...) is stored with timestamp and optional change notes.
  - Historical versions are NEVER overwritten.
  - Previous orders permanently reference their specific measurement snapshot.
- **REQ-MEAS-05**: Measurement Comparison Tool: Side-by-side diff highlighting changes between two versions of measurements.

### F. Design Gallery
- **REQ-DSGN-01**: Capture image via Camera or select from device Photo Gallery.
- **REQ-DSGN-02**: Compress image locally before saving to app sandbox storage to conserve bandwidth and storage.
- **REQ-DSGN-03**: Attach designs to specific Customers, Family Members, or Orders.
- **REQ-DSGN-04**: Add design annotations, neck style tags, embroidery notes, and fabric swatches.
- **REQ-DSGN-05**: Offline image queue: Local file references are retained immediately; background WorkManager uploads to Supabase Storage once online.

### G. Order Management
- **REQ-ORD-01**: Unique Auto-incrementing Order Number with optional prefix (e.g., `TB-2026-0001`).
- **REQ-ORD-02**: Order Intake flow: Customer selection -> Family member selection -> Garment template -> Measurement selection/snapshot -> Design photo attachment -> Stitching instructions & lining notes -> Promised delivery date -> Pricing & Advance payment.
- **REQ-ORD-03**: **Measurement Snapshot**: At the moment an order is created, an immutable snapshot of the selected measurement version is frozen into the order record.
- **REQ-ORD-04**: Order Status Workflow with timestamped audit history:
  1. `NEW` (Order booked)
  2. `CUTTING` (Cloth cutting underway)
  3. `STITCHING` (Sewing in progress)
  4. `FINISHING` (Ironing, buttoning, hooks, quality check)
  5. `READY` (Ready for pickup/delivery)
  6. `DELIVERED` (Handed over to customer)
  7. `CANCELLED` (Order cancelled with documented reason)
- **REQ-ORD-05**: Multi-item Orders: Support orders with multiple garments (e.g., 2 Blouses + 1 Kurti in a single order).
- **REQ-ORD-06**: Order Timeline & History: View complete log of who changed the status and when.

### H. Delivery Calendar
- **REQ-CAL-01**: Monthly, Weekly, and Daily calendar views showing scheduled deliveries.
- **REQ-CAL-02**: Color-coded delivery markers (Overdue: Red, Due Today: Amber, Upcoming: Teal, Delivered: Sage).
- **REQ-CAL-03**: Quick Actions from Calendar: Tap order to reschedule delivery date or mark as Delivered.
- **REQ-CAL-04**: Delivery filtering by status, garment type, or assigned worker.

### I. Payments & Balances
- **REQ-PAY-01**: Transaction-safe recording of payments:
  - Total Stitching Cost
  - Fabric / Material Charges
  - Advance Paid
  - Outstanding Balance (`Total - Advance - Subsequent Payments`)
- **REQ-PAY-02**: Supported payment methods: Cash, UPI (PhonePe, Google Pay, Paytm), Bank Transfer, Cheque, Other.
- **REQ-PAY-03**: Multiple partial payments per order with individual receipts.
- **REQ-PAY-04**: Payment Receipts: Generate clean digital receipt card with shop header, order details, amount paid, and remaining balance.
- **REQ-PAY-05**: Share receipt via Android Sharesheet directly to WhatsApp / SMS.
- **REQ-PAY-06**: Refund and Reversal Workflow: Auditable reversal of erroneous entries with mandatory reason note; no silent hard deletions of financial records.

### J. Business Reports
- **REQ-REP-01**: Cash Flow Report: Clear distinction between Booked Order Value vs. Actual Money Collected in selected date range.
- **REQ-REP-02**: Orders by Status breakdown (Pie/Bar chart).
- **REQ-REP-03**: Outstanding Balances List sorted by overdue days and amount.
- **REQ-REP-04**: Popular Garments & Fabric Trends analytics.
- **REQ-REP-05**: Delivery punctuality rate (% on-time deliveries).

### K. Notifications
- **REQ-NOTIF-01**: In-app Notification center and Android system push/local notifications:
  - Deliveries due tomorrow / due today
  - Deliveries overdue
  - Unpaid balance reminders
  - Cloud sync errors or pending offline items
- **REQ-NOTIF-02**: Read/Unread tracking with one-tap clear or action redirect.

### L. Staff Management (MVP Basic)
- **REQ-STAF-01**: Add staff member with Name, Phone, and Role:
  - `OWNER` (Full access, billing, settings)
  - `MANAGER` (Customer, orders, payments, reports)
  - `TAILOR / WORKER` (View assigned orders, update order status to Cutting/Stitching/Ready, view measurements)
- **REQ-STAF-02**: Enable/disable staff access at any time.
- **REQ-STAF-03**: Audit logging: Record staff member ID against status changes and payment entries.

### M. Settings & Data Portability
- **REQ-SET-01**: Measurement Unit preference (Inches vs Centimeters).
- **REQ-SET-02**: App Language toggle: English ⟷ Telugu (తెలుగు) with dynamic re-composition without app restart.
- **REQ-SET-03**: Local database backup & JSON export via Android Storage Access Framework (SAF).
- **REQ-SET-04**: Cloud Sync status dashboard showing last sync time, pending queue count, and "Sync Now" button.
- **REQ-SET-05**: Privacy policy, terms of service, and support contact.

### N. Subscription & Advertisements
- **REQ-SUB-01**: Free Tier: Complete offline core functionality (customers, measurements, orders, payments) supported by non-intrusive banner ads.
- **REQ-SUB-02**: **Strict Ad Placement Rules**:
  - ZERO ads during Order Creation flow.
  - ZERO ads during Measurement Entry.
  - ZERO ads during Payment Recording.
  - ZERO ads during Delivery Confirmation.
  - NO forced interruptive full-screen interstitials during core business actions.
  - NEVER lock or hold hostage user data behind a paywall.
- **REQ-SUB-03**: Premium Tier Feature Hypotheses: Ad-free experience, unlimited cloud sync across multiple devices, PDF professional receipts, multi-staff sync.
- **REQ-SUB-04**: Configurable pricing hypothesis (₹149/month, ₹1,199/year); zero hardcoding of payment gateways until explicitly authorized.

---

## 4. Non-Functional Requirements
- **NFR-01 (Offline First)**: 100% of read and write operations function without network access.
- **NFR-02 (Performance)**: App cold start under 1.5s on mid-tier Android devices (e.g. Snapdragon 680 / Helio G85). Smooth 60fps Compose scrolling.
- **NFR-03 (Data Integrity)**: Zero data loss during unexpected app termination or power loss; all multi-step transactions wrapped in Room transactions.
- **NFR-04 (Screen Adaptability)**: Pixel-perfect responsive UI for screen densities from `mdpi` to `xxxhdpi` and screen diagonals as small as 5.0 inches.
- **NFR-05 (Security)**: End-to-end tenant isolation via Supabase RLS; no secret or service-role keys embedded in client binary.
