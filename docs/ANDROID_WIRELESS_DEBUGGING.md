# TailorBook — Android Wireless & USB Debugging Guide

This guide provides step-by-step instructions to connect your physical Android phone to your Windows development computer for installing, running, and testing TailorBook after each iteration.

---

## 1. Prerequisites on Phone & Computer
1. **Operating System**: Android 11 (API level 30) or higher is required for native Wireless Debugging without initial USB connection. (If Android 10 or lower, use USB Debugging as detailed in Section 6).
2. **Wi-Fi Network**: Both the Android phone and your Windows computer must be connected to the **same Wi-Fi network** (or phone connected to computer mobile hotspot).
3. **Android Platform Tools (ADB)**: Installed on the Windows computer.

---

## 2. Enabling Wireless Debugging on Android Phone
1. Open **Settings** on your Android phone.
2. Go to **About Phone** (or **System** > **About Phone**).
3. Tap **Build Number** 7 times consecutively until you see the message: *"You are now a developer!"*
4. Go back to **Settings** > **System** (or **Additional Settings**) > **Developer Options**.
5. Scroll down to the **Debugging** section:
   - Toggle **USB Debugging** -> **ON**.
   - Toggle **Wireless Debugging** -> **ON**.
   - If prompted *"Allow wireless debugging on this network?"*, tap **Allow** (check *"Always allow on this network"*).

---

## 3. Step-by-Step Pairing & Connection (One-Time Setup per Wi-Fi)

### Step 3.1: Get Pairing Information from Phone
1. Inside **Developer Options**, tap directly on the text **Wireless Debugging** (not just the toggle).
2. Tap **Pair device with pairing code**.
3. A popup will display:
   - **Wi-Fi pairing code** (e.g., `123456`)
   - **IP address & Port** (e.g., `192.168.1.50:37123` — Note: this port is the *Pairing Port*).

### Step 3.2: Pair using ADB Command on Windows
Open PowerShell on your computer and execute:
```powershell
adb pair 192.168.X.X:PAIRING_PORT
```
*When prompted, enter the 6-digit Wi-Fi pairing code from the phone screen.*
You will see: `Successfully paired to 192.168.X.X:PAIRING_PORT`

### Step 3.3: Connect using the Connection Port
Look at the main Wireless Debugging screen on your phone under **IP address & Port** (e.g., `192.168.1.50:42135` — Note: this port is usually different from the pairing port).
Run:
```powershell
adb connect 192.168.X.X:CONNECT_PORT
```
You will see: `connected to 192.168.X.X:CONNECT_PORT`

### Step 3.4: Verify Active Connection
```powershell
adb devices -l
```
You should see your phone listed with status `device`:
```text
List of devices attached
192.168.1.50:42135   device product:... model:... device:...
```

---

## 4. Iteration Commands: Build, Install, Launch & Logs
After each iteration, use these standard commands to test on your phone:

### 4.1 Build Debug APK
```powershell
.\gradlew assembleDebug
```

### 4.2 Install / Update APK on Phone
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 4.3 Launch TailorBook on Phone
```powershell
adb shell am start -n com.tailorbook.app/.MainActivity
```

### 4.4 View Real-time App Logs
```powershell
adb logcat -s TailorBook:V AndroidRuntime:E
```

---

## 5. Troubleshooting Wireless Debugging Issues

| Problem | Cause | Solution |
| :--- | :--- | :--- |
| `adb: command not found` | ADB is not in system PATH | Add Android SDK `platform-tools` to Windows PATH or use full path. |
| `failed to connect` / `Connection refused` | Port changed after Wi-Fi reconnect | On Android, the connection port changes whenever Wi-Fi disconnects. Check the new port on your phone and run `adb connect 192.168.X.X:NEW_PORT`. |
| Phone not found / ping fails | Router AP Isolation / Guest network | Some routers block device-to-device communication. Connect phone to your PC's Mobile Hotspot instead. |
| Pairing code fails | Timeout or mismatched IP | The pairing dialog must remain open on the phone while typing the code in PowerShell. |

---

## 6. USB Debugging Fallback
If Wi-Fi debugging is ever unstable or unavailable:
1. Connect phone to PC using a USB cable.
2. In Developer Options, enable **USB Debugging**.
3. On phone screen, tap **Allow USB Debugging** (check *"Always allow from this computer"*).
4. Run `adb devices -l` to verify immediate connection.
