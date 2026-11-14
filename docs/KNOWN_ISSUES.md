# TailorBook — Known Issues & Remediation Log

This document tracks known technical blockers, environment dependencies, defects, and remediation steps.

---

## 1. Environment & Prerequisites (Discovered in Phase 00)

| Issue ID | Category | Description | Severity | Remediation Plan | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `ENV-001` | Tooling | **Java / JDK Missing**: `JAVA_HOME` pointed to non-existent `C:\Program Files\Java\jdk-19\bin`. Modern Android Gradle Plugin requires JDK 17 or JDK 21. | **BLOCKER for Phase 01** | Install OpenJDK 17 or Microsoft OpenJDK 17 via `winget` or direct installer, and configure `JAVA_HOME` and PATH. | OPEN |
| `ENV-002` | Tooling | **Android SDK & ADB Missing**: Android SDK is not currently present at standard location (`C:\Users\Akshara\AppData\Local\Android\Sdk`). `adb.exe` is required for wireless mobile testing. | **BLOCKER for Phase 01** | Install Android Command Line Tools / Android Studio / Platform Tools, set `ANDROID_HOME`, and configure `platform-tools` in PATH. | OPEN |

---

## 2. Technical Decisions & Architecture Constraints
- **DEC-001**: Measurement historical snapshots must be frozen into `order_measurement_snapshots` as immutable copies. Future edits to customer measurements must NEVER cascade to existing placed orders.
- **DEC-002**: Payment records are strictly append-only. No hard deletes or silent overwrites are permitted. Erroneous payments must use the auditable refund/reversal workflow.
