package com.tailorbook.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// TailorBook Premium Boutique — Color Palette
// ─────────────────────────────────────────────────────────────────────────────

// Primary brand colors
val WarmIvory     = Color(0xFFF8F5EF)   // App background
val DeepTeal      = Color(0xFF203B36)   // Primary / AppBar / FAB
val DeepTealDark  = Color(0xFF142623)   // Darker teal for pressed states
val DeepTealLight = Color(0xFF2E5248)   // Lighter teal for hover / focus ring
val MutedSage     = Color(0xFF84978A)   // Secondary / outlines / subtle text
val SoftGold      = Color(0xFFB58A5A)   // Accent / highlights / FAB icon

// Surface colors
val CardWhite     = Color(0xFFFFFFFF)   // Card / dialog background
val Charcoal      = Color(0xFF263238)   // Primary text
val CharcoalSubtle= Color(0xFF546E7A)   // Secondary / body text
val LightBeige    = Color(0xFFE7E2D9)   // Dividers / card borders / chips bg

// Status / semantic colors
val StatusRed     = Color(0xFFC62828)   // Error / cancelled / overdue
val StatusRedLight= Color(0xFFFFEBEE)   // Error container background
val StatusGreen   = Color(0xFF2E7D32)   // Success / delivered / synced
val StatusGreenLight = Color(0xFFE8F5E9) // Success container background
val StatusAmber   = Color(0xFFF57F17)   // Warning / pending / stitching
val StatusAmberLight = Color(0xFFFFF8E1) // Warning container background
val StatusBlue    = Color(0xFF1565C0)   // Info / cutting
val StatusBlueLight  = Color(0xFFE3F2FD) // Info container background

// Order status badge colors (maps to order statuses for quick access)
val StatusNew       = StatusBlue
val StatusNewBg     = StatusBlueLight
val StatusCutting   = StatusBlue
val StatusCuttingBg = StatusBlueLight
val StatusStitching = StatusAmber
val StatusStitchingBg = StatusAmberLight
val StatusFinishing = Color(0xFF6A1B9A)  // Purple for finishing stage
val StatusFinishingBg = Color(0xFFF3E5F5)
val StatusReady     = StatusGreen
val StatusReadyBg   = StatusGreenLight
val StatusDelivered = MutedSage
val StatusDeliveredBg = LightBeige
val StatusCancelled = StatusRed
val StatusCancelledBg = StatusRedLight

// Offline / sync indicator colors
val OfflineBannerBg   = Color(0xFFE0E0E0)
val OfflineBannerText = Charcoal
val SyncingColor      = StatusAmber
val SyncedColor       = StatusGreen

// Scrim / overlay
val Scrim = Color(0x99000000)           // 60% black for modal overlays

// Navigation drawer
val DrawerHeaderBg    = DeepTeal
val DrawerItemSelected = DeepTeal.copy(alpha = 0.12f)
val DrawerItemHover    = DeepTeal.copy(alpha = 0.06f)
