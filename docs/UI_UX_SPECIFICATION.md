# TailorBook — UI/UX Specification

## 1. Visual Direction: "Premium Boutique"
TailorBook provides a warm, elegant, craft-centered visual aesthetic tailored for tailoring shops and boutiques. It balances traditional craftsmanship with modern Material 3 design principles.

---

## 2. Color Palette & Tokens

| Design Token | Hex Code | Visual Swatch | Semantic Usage |
| :--- | :--- | :--- | :--- |
| **Warm Ivory** | `#F8F5EF` | Background / Canvas | Primary application surface, screen backgrounds |
| **Deep Teal** | `#203B36` | Primary Brand Color | App bars, prominent CTA buttons, active state indicators |
| **Muted Sage** | `#84978A` | Secondary Accent | Subdued chips, delivered order tags, secondary actions |
| **Soft Gold** | `#B58A5A` | Accent / Highlight | Star indicators, premium badges, ready-for-delivery alerts |
| **White** | `#FFFFFF` | Card Surface | Elevated cards, dialog boxes, bottom sheet sheets |
| **Charcoal** | `#263238` | Text & High Contrast | Primary typography, headers, high-emphasis icons |
| **Light Beige** | `#E7E2D9` | Border & Outline | Card borders, dividers, disabled states |
| **Status Red** | `#C62828` | Alert / Danger | Overdue orders, balance dues, cancellation tags |
| **Status Green** | `#2E7D32` | Success | Completed payments, fully delivered orders |

---

## 3. Typography & Localization
- **English**: Roboto / Inter font family with crisp hierarchy:
  - Display: 32sp, SemiBold
  - Headline: 24sp, SemiBold
  - Title: 18sp, Medium
  - Body: 14sp, Regular (line height 20sp)
  - Label / Caption: 12sp, Medium
- **Telugu (తెలుగు)**: Noto Sans Telugu:
  - Specially tuned vertical line metrics and line-height multipliers (1.3x) to prevent Telugu vowel mark clipping (క, కొ, కు).
- **Locale-Aware Formatting**:
  - Currency: Indian Rupee format `₹1,450.00`
  - Dates: `DD/MM/YYYY` or `15 Nov 2026`
  - Phone numbers: `+91 98765 43210`

---

## 4. Navigation Architecture: Left-Side Navigation Drawer
TailorBook uses a standard, accessible left-side navigation drawer (`ModalNavigationDrawer` in Material 3) for primary top-level routing, combined with contextual bottom sheets and top app bars.

### Drawer Destinations:
1. **Dashboard** (Home icon)
2. **Customers** (People icon)
3. **Measurements** (Straighten / Ruler icon)
4. **Orders** (Shopping bag / Scissors icon)
5. **Delivery Calendar** (Calendar month icon)
6. **Payments** (Receipt / Account balance wallet icon)
7. **Design Gallery** (Photo library icon)
8. **Reports** (Analytics / Bar chart icon)
9. **Staff Management** (Badge / Supervisor account icon)
10. **Subscription & Premium** (Workspace premium / Diamond icon)
11. **Notifications** (Notifications active icon)
12. **Settings** (Settings icon)
13. **Help & Support** (Help outline icon)

---

## 5. Standard Component Specifications

### 5.1 Reusable Cards
- **Corner Radius**: `16dp` for elevated cards, `12dp` for inner item cards.
- **Elevation**: 1dp resting, 3dp pressed.
- **Card Border**: 1dp solid `#E7E2D9` on `#FFFFFF` card surface.

### 5.2 Input Fields (`TailorTextField`)
- **Variant**: Outlined text field with rounded corners (`12dp`).
- **Colors**: Border `#84978A`, Focused border `#203B36`, Cursor `#203B36`.
- **Keyboard types**: Custom `KeyboardType.Decimal` for measurements and money inputs.

### 5.3 Buttons
- **Primary Button**: Deep Teal (`#203B36`) background with White text, rounded corners (`12dp`), height `52dp`.
- **Secondary Button**: Outlined Deep Teal with Warm Ivory background.
- **Floating Action Button (FAB)**: Deep Teal background with Soft Gold or White icon.

### 5.4 Screen State Guidelines
Every screen must cleanly handle and render 5 universal states:
1. **Loading State**: Shimmer skeleton placeholder or central Deep Teal circular indicator.
2. **Content State**: Normal interactive list/form.
3. **Empty State**: Tailor-themed vector illustration, friendly title, descriptive helper text, and a prominent call-to-action button (e.g., "No customers found. Add your first customer!").
4. **Error State**: Non-technical user-friendly error message with a "Retry" button.
5. **Offline Banner**: Persistent, slim top bar showing "Working Offline — Changes will sync when connected".

---

## 6. Small Screen Responsiveness
Target support: Screens down to **5.0 inches** (HD 720x1280, 320dp width):
- Responsive scrollable columns (`verticalScroll`) on all input forms.
- Dynamic grid wrapping for measurement fields (2 columns on wide screens, 1 column on narrow screens).
- Minimum touch target: 48dp x 48dp for all interactive buttons and icons.
