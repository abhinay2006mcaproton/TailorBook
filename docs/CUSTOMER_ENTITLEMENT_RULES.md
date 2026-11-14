# TailorBook — Customer Entitlement Rules

**Version**: 1.0.0  
**Status**: APPROVED by Product Owner (Nov 3, 2026)  
**Module**: `feature/subscription/domain/CustomerEntitlementEngine.kt`  

---

## 1. Commercial Tier Thresholds

The approved commercial thresholds are:

| Tier | Active Customer Range | Entitlement Policy | Ad Requirement |
|---|:---:|---|---|
| **Tier 1: Free Starter** | **1 to 10** | Normal creation allowed. | **No ads required.** |
| **Tier 2: Ad-Supported Growth** | **11 to 50** | Allowed **only** after 1 verified Rewarded Video Ad. | **1 Rewarded Video Ad per customer creation.** |
| **Tier 3: TailorBook Premium** | **51+** | Blocked on free tier; requires active Premium subscription. | **Unlimited customers, completely ad-free.** |

---

## 2. Customer Counting & Exclusions

1. **Active Customers**: Counted strictly as `isArchived == 0` for the current tenant `shopId`.
2. **Archived / Soft-Deleted Customers**: `isArchived == 1` records are excluded from the active customer limit count.
3. **Data Protection Principle**: If Premium expires or changes, existing customer records are **never deleted or hidden**. Only the creation of customer 51+ is blocked.

---

## 3. Rewarded Ad Single-Use Token Lifecycle

1. **Generation**: When a tailor completes watching a rewarded video, `grantRewardToken()` issues a cryptographically secure token (`tb_rwd_<UUID>_<timestamp>`).
2. **Evaluation**: While the token is pending, `evaluateCreationDecision()` transitions from `RequiresRewardedAd` to `AllowedWithReward(token)`.
3. **Consumption**: Upon tapping "Save Customer", `consumeRewardToken(token)` consumes the token atomically using a thread-safe `Mutex`.
4. **Idempotency**: Consumed tokens are permanently stored in an in-memory set and invalidated. The same token cannot be reused to create multiple customers.
