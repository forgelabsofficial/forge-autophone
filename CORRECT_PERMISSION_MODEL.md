# 🔐 AutoPhone Permission Model (Correct)

## The Two-Tier System

AutoPhone and Forge OS are **intertwined** - they work together as a unified system. Other apps can use AutoPhone too, but with limitations and user approval.

---

## 🏆 Tier 1: Forge OS (Privileged Access)

### Automatic Trust
- **No permission prompts** - Forge OS is automatically trusted
- **Full tool access** - All 78 automation tools available
- **Priority connection** - Forge OS gets preferential access
- **Real-time events** - Full event streaming
- **No restrictions** - Complete flexibility

### Why?
Forge OS and AutoPhone are designed to work together. AutoPhone is Forge OS's accessibility layer - they're part of the same ecosystem.

**Think of it like:**
- Android Settings → Can access everything (system app)
- Forge OS + AutoPhone → Tight integration (ecosystem apps)

---

## 📱 Tier 2: Third-Party Apps (Limited Access)

### Require Approval
- **User approval needed** - Permission dialog shown
- **Limited access** - Restricted tool subset
- **Can be revoked** - User can deny/revoke anytime
- **Auditable** - All access logged
- **Less flexible** - Security restrictions

### Why?
Third-party apps shouldn't have unrestricted access to sensitive automation capabilities. User must explicitly grant permission.

---

## 🔄 How It Works

### Forge OS Connection

```
User enables AutoPhone service (Android Settings)
        ↓
AutoPhone starts running
        ↓
Forge OS opens/connects
        ↓
AutoPhone detects: "This is Forge OS!" ✅
        ↓
Automatic privileged access granted
        ↓
No prompts, full access, works immediately
```

### Third-Party App Connection

```
User enables AutoPhone service (Android Settings)
        ↓
AutoPhone starts running
        ↓
Unknown app tries to connect
        ↓
AutoPhone detects: "Unknown app" ⚠️
        ↓
Show permission dialog to user:
"AppName wants to use AutoPhone
 [Deny] [Allow]"
        ↓
If user allows: Limited access granted
If user denies: Access blocked
```

---

## 🎯 Permission Checks

### In AutoPhone Service

```kotlin
// When any app connects:
fun onAppConnected(packageName: String) {
    val permission = permissionManager.checkPermission(packageName)
    
    when (permission) {
        PermissionStatus.FORGE_OS_TRUSTED -> {
            // ✅ Forge OS - automatic full access
            grantFullAccess()
            notifyUI("Forge OS connected 🟢")
        }
        
        PermissionStatus.APPROVED -> {
            // ✓ Previously approved third-party app
            grantLimitedAccess()
            notifyUI("$appName connected 🔵")
        }
        
        PermissionStatus.DENIED -> {
            // ❌ Previously denied
            rejectConnection()
            notifyUI("$appName access denied ⛔")
        }
        
        PermissionStatus.PENDING -> {
            // ⚠️ Unknown app - ask user
            showPermissionDialog(packageName, appName)
        }
    }
}
```

### Package Name Check

```kotlin
companion object {
    // Forge OS is hardcoded as trusted
    const val FORGE_OS_PACKAGE = "com.forge.os"
}

fun checkPermission(packageName: String): PermissionStatus {
    // Special handling for Forge OS
    if (packageName == FORGE_OS_PACKAGE) {
        return PermissionStatus.FORGE_OS_TRUSTED
    }
    
    // Check user-granted permissions for others
    return checkUserPermissions(packageName)
}
```

---

## 📊 UI Display

### Connection Status Card

**When Forge OS is connected:**
```
┌─────────────────────────────┐
│ 🔗 Forge OS Connection      │  ← Green background
│                             │
│ 🟢 Forge OS Connected       │
│                             │
│ ✓ Full privileged access    │
│ ✓ All 78 tools available    │
│ ✓ No permission prompts     │
└─────────────────────────────┘
```

**When third-party app is connected:**
```
┌─────────────────────────────┐
│ 🔗 Forge OS Connection      │  ← Gray background
│                             │
│ 🔵 Other app connected      │
│                             │
│ Currently: com.example.app  │
│ ⚠️ Limited access only      │
└─────────────────────────────┘
```

**When nothing is connected:**
```
┌─────────────────────────────┐
│ 🔗 Forge OS Connection      │
│                             │
│ 🔵 Ready for connection     │
│                             │
│ Waiting for Forge OS...     │
│ (Automatic privileged access)│
└─────────────────────────────┘
```

### Permission Model Card

```
┌─────────────────────────────┐
│ 🔐 Permission Model         │
│                             │
│ Two-tier system:            │
│                             │
│ ┌─────────────────────────┐ │
│ │ 🏆 Forge OS (Privileged)│ │  ← Green/primary
│ │ • Automatic trust       │ │
│ │ • Full tool access      │ │
│ │ • No prompts            │ │
│ │ • Priority connection   │ │
│ └─────────────────────────┘ │
│                             │
│ ┌─────────────────────────┐ │
│ │ 📱 Other Apps (Limited) │ │  ← Blue/secondary
│ │ • User approval needed  │ │
│ │ • Limited tool access   │ │
│ │ • Permission prompts    │ │
│ │ • Can be revoked        │ │
│ └─────────────────────────┘ │
│                             │
│ Tight Forge OS integration  │
│ + Security for third-party  │
└─────────────────────────────┘
```

---

## 🔒 Security Model

### Why This Model?

1. **Forge OS Integration**
   - AutoPhone is built FOR Forge OS
   - Tight coupling by design
   - No barriers between ecosystem apps
   - Seamless user experience

2. **Third-Party Security**
   - Unknown apps need approval
   - User controls access
   - Limited capabilities
   - Revocable permissions
   - Audit trail

3. **User Trust**
   - User installs both Forge OS + AutoPhone
   - Implicit trust in ecosystem
   - Explicit approval for outsiders
   - Always in control

---

## 📝 Implementation Details

### PermissionManager Class

```kotlin
@Singleton
class PermissionManager {
    companion object {
        const val FORGE_OS_PACKAGE = "com.forge.os"
    }
    
    fun checkPermission(packageName: String): PermissionStatus {
        // Forge OS: automatic trust
        if (packageName == FORGE_OS_PACKAGE) {
            return PermissionStatus.FORGE_OS_TRUSTED
        }
        
        // Others: check user permissions
        return checkUserApproval(packageName)
    }
    
    fun grantPermission(packageName: String) {
        // Save user approval
        approvedApps.add(packageName)
    }
    
    fun revokePermission(packageName: String) {
        // Remove approval
        approvedApps.remove(packageName)
    }
}
```

### Permission Dialog (Future)

```kotlin
// When unknown app tries to connect
fun showPermissionDialog(packageName: String, appName: String) {
    AlertDialog(
        title = "AutoPhone Access Request",
        text = "$appName wants to use AutoPhone automation.\n\n" +
               "This will allow $appName to:\n" +
               "• Read screen content\n" +
               "• Simulate touches\n" +
               "• Extract text\n\n" +
               "Note: Forge OS has automatic privileged access.",
        confirmButton = {
            Button(onClick = {
                permissionManager.grantPermission(packageName)
                // Grant limited access
            }) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                permissionManager.denyPermission(packageName)
                // Reject connection
            }) {
                Text("Deny")
            }
        }
    )
}
```

---

## 🎮 User Controls

### Permission Management Screen (Future)

```
┌─────────────────────────────┐
│ ⚙️ App Permissions          │
│                             │
│ 🏆 Forge OS                 │
│    Trusted (Automatic)      │
│    [Cannot revoke]          │
│                             │
│ ✓ Example App               │
│    Approved by you          │
│    [Revoke Access]          │
│                             │
│ ❌ Blocked App              │
│    Denied by you            │
│    [Allow Access]           │
│                             │
└─────────────────────────────┘
```

---

## 🔍 Access Levels

### Forge OS (Full Access)

**Tools Available: 78**
- ✅ All UI inspection tools
- ✅ All text input tools
- ✅ All gesture tools
- ✅ OCR tools
- ✅ Icon matching
- ✅ Smart waiting
- ✅ Smart scrolling
- ✅ Event streaming (real-time)
- ✅ Context awareness
- ✅ Verification tools
- ✅ Self-healing selectors
- ✅ Gesture recording
- ✅ Form automation
- ✅ Telemetry (full access)

### Third-Party Apps (Limited Access)

**Tools Available: ~30-40**
- ✅ Basic UI inspection
- ✅ Basic text input
- ✅ Basic gestures (click, swipe)
- ⚠️ OCR (rate limited)
- ❌ Icon matching (not available)
- ✅ Simple waiting
- ✅ Simple scrolling
- ❌ Event streaming (not available)
- ❌ Context awareness (not available)
- ⚠️ Verification (limited)
- ❌ Self-healing (not available)
- ❌ Gesture recording (not available)
- ⚠️ Form automation (basic only)
- ❌ Telemetry (not available)

---

## 💡 Key Differences

| Feature | Forge OS | Third-Party Apps |
|---------|----------|------------------|
| **Permission** | Automatic | Requires approval |
| **Access Level** | Full (78 tools) | Limited (~30 tools) |
| **Real-time Events** | ✅ Yes | ❌ No |
| **OCR** | Unlimited | Rate limited |
| **Icon Matching** | ✅ Yes | ❌ No |
| **Self-Healing** | ✅ Yes | ❌ No |
| **Telemetry** | Full access | No access |
| **Priority** | High | Normal |
| **Revocable** | No | Yes |

---

## 🎯 Benefits

### For Users
- **Seamless Forge OS experience** - No friction, just works
- **Security for unknowns** - Third-party apps need approval
- **Always in control** - Can revoke third-party access
- **Transparency** - See which apps are connected

### For Forge OS
- **Tight integration** - No permission barriers
- **Full capabilities** - Use all 78 tools
- **Real-time control** - Event streaming
- **Priority access** - Never blocked

### For Third-Party Developers
- **Still usable** - Can access AutoPhone (with limits)
- **Clear process** - Request permission, get approval
- **Documented limits** - Know what's available
- **Fair system** - Everyone follows same rules

---

## 📚 Summary

### The Model

```
┌──────────────────────────────────┐
│ AutoPhone Service                │
│                                  │
│  ┌────────────────────────────┐ │
│  │ Forge OS                   │ │ ← Automatic trust
│  │ Package: com.forge.os      │ │   Full access (78 tools)
│  │ Status: TRUSTED            │ │   No prompts
│  └────────────────────────────┘ │
│                                  │
│  ┌────────────────────────────┐ │
│  │ Third-Party App            │ │ ← User approval
│  │ Package: com.example.app   │ │   Limited access (~30 tools)
│  │ Status: APPROVED           │ │   With prompts
│  └────────────────────────────┘ │
│                                  │
│  ┌────────────────────────────┐ │
│  │ Unknown App                │ │ ← Blocked
│  │ Package: com.unknown.app   │ │   Must request
│  │ Status: PENDING            │ │   User decides
│  └────────────────────────────┘ │
└──────────────────────────────────┘
```

### Key Points

1. ✅ **Forge OS is privileged** - automatic full access
2. ⚠️ **Third-party apps are limited** - need approval + restrictions
3. 🔒 **User controls everything** - can approve/deny/revoke
4. 🔗 **Tight integration** - Forge OS + AutoPhone work as one
5. 🛡️ **Security maintained** - unknowns can't abuse access

---

**🤖 AutoPhone: Built for Forge OS, Usable by Others (with permission) 🔐**
