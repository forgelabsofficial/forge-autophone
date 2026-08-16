# 🔗 AutoPhone + Forge OS Integration (Correct Architecture)

## 🎯 The Right Way

After inspecting Forge OS codebase, I found that **Forge OS already has a comprehensive external API permission system** via `ExternalApiBridge` and `ExternalCallerRegistry`.

**AutoPhone should integrate with this existing system**, not create its own parallel permission layer.

---

## 🏗️ Forge OS Permission Architecture

### Key Components

#### 1. `ExternalApiBridge`
**Location:** `app/src/main/java/com/forge/os/external/ExternalApiBridge.kt`

**Purpose:** Single chokepoint for all external API access
- AIDL service calls
- ContentProvider queries
- Intent activities

**Key Features:**
- Master enable/disable switch
- Authorization checks per operation
- Capability validation
- Rate limiting (calls/minute, tokens/day)
- Audit logging
- Token usage tracking

#### 2. `ExternalCallerRegistry`
**Location:** `app/src/main/java/com/forge/os/external/ExternalCallerRegistry.kt`

**Purpose:** Persists approved/denied external apps
- Stores in `workspace/system/external_callers.json`
- Tracks package name, signing cert, status
- Provides StateFlow for UI updates

#### 3. `ExternalCaller` Data Model
**Location:** `app/src/main/java/com/forge/os/external/ExternalCaller.kt`

```kotlin
data class ExternalCaller(
    val packageName: String,
    val displayName: String,
    val signingCertSha256: String,
    val firstSeen: Long,
    val lastUsed: Long,
    val status: GrantStatus,           // PENDING, GRANTED, DENIED, REVOKED
    val capabilities: Capabilities,
    val rateLimit: RateLimit
)

data class Capabilities(
    val listTools: Boolean,
    val invokeTools: Boolean,
    val toolAllowlist: List<String>,   // ["*"] or specific tools
    val askAgent: Boolean,
    val readMemory: Boolean,
    val memoryTagFilter: String,
    val writeMemory: Boolean,
    val runSkills: Boolean,
    val skillAllowlist: List<String>
)

data class RateLimit(
    val callsPerMinute: Int = 30,
    val tokensPerDay: Int = 50_000
)
```

---

## ✅ Correct Integration Approach

### AutoPhone Should:

1. **✅ Use Forge OS's ExternalCallerRegistry** - Don't create separate permission storage
2. **✅ Connect via AIDL like other external apps** - But with automatic GRANTED status
3. **✅ Be registered as trusted in Forge OS** - Hardcoded package name
4. **✅ Check Forge OS permission before executing sensitive tools** - Respect the capability system

### What This Means:

```
┌────────────────────────────────────────┐
│ Forge OS                               │
│                                        │
│  ExternalApiBridge                     │
│  ├─ Authorization check                │
│  ├─ Capability validation              │
│  ├─ Rate limiting                      │
│  └─ Audit logging                      │
│                                        │
│  ExternalCallerRegistry                │
│  ├─ com.forge.autophone → GRANTED     │  ← AutoPhone is pre-approved
│  ├─ com.example.app → PENDING         │  ← Third-party needs approval
│  └─ com.another.app → DENIED          │
│                                        │
└────────────────────────────────────────┘
           ↓
           ↓ AIDL binding
           ↓
┌────────────────────────────────────────┐
│ AutoPhone                              │
│                                        │
│  Connects to Forge OS via AIDL         │
│  Package: com.forge.autophone          │
│  Status: Auto-approved (GRANTED)       │
│  Capabilities: Full access             │
│                                        │
└────────────────────────────────────────┘
```

---

## 🔧 Implementation Changes Needed

### 1. Remove AutoPhone's Own PermissionManager ❌

**Delete:**
- `src/main/java/com/forge/autophone/permissions/PermissionManager.kt`

**Why:** Forge OS already has this functionality

### 2. AutoPhone Should Connect to Forge OS Like Any External App ✅

**But with special treatment:**

```kotlin
// In Forge OS ExternalCallerRegistry
fun observeAutoPhone(uid: Int): ExternalCaller? {
    val caller = observe(uid)
    
    // Special handling for AutoPhone
    if (caller?.packageName == "com.forge.autophone") {
        // Auto-approve with full capabilities
        return caller.copy(
            status = GrantStatus.GRANTED,
            capabilities = Capabilities(
                listTools = true,
                invokeTools = true,
                toolAllowlist = listOf("*"),  // All tools
                askAgent = true,
                readMemory = true,
                memoryTagFilter = "",         // All memory
                writeMemory = true,
                runSkills = true,
                skillAllowlist = listOf("*")  // All skills
            ),
            rateLimit = RateLimit(
                callsPerMinute = 1000,        // High limit
                tokensPerDay = 1_000_000      // High limit
            )
        ).also { upsert(it) }
    }
    
    return caller
}
```

### 3. Forge OS Authorization Layer

**In `ExternalApiBridge.authorize()`:**

```kotlin
fun authorize(uid: Int, op: String, target: String = ""): Decision {
    if (!masterEnabled()) {
        return Decision.Deny(403, "External API disabled")
    }
    
    val caller = registry.observe(uid)
        ?: return Decision.Deny(403, "Caller not resolvable")
    
    // AutoPhone gets automatic approval
    if (caller.packageName == "com.forge.autophone") {
        if (caller.status != GrantStatus.GRANTED) {
            // Auto-grant on first access
            registry.setStatus(caller.packageName, GrantStatus.GRANTED)
            registry.setCapabilities(
                caller.packageName,
                Capabilities(/* full access as shown above */)
            )
        }
        return Decision.Allow(caller)
    }
    
    // Regular external app authorization continues...
    if (caller.status != GrantStatus.GRANTED) {
        return Decision.Deny(403, "Not granted")
    }
    
    // ... capability checks, rate limiting, etc.
}
```

---

## 📱 User Experience

### Forge OS UI - External Apps Screen

```
┌────────────────────────────────────┐
│ External API Access                │
├────────────────────────────────────┤
│                                    │
│ 🤖 AutoPhone                       │  ← Trusted system app
│    Package: com.forge.autophone    │
│    Status: Trusted (System)        │
│    Access: Full (78 tools)         │
│    [Cannot revoke]                 │  ← Locked, can't disable
│                                    │
│ ─────────────────────────────────  │
│                                    │
│ ✅ Example App                     │  ← User-approved third-party
│    Package: com.example.app        │
│    Status: Granted                 │
│    Access: Limited (15 tools)      │
│    [Revoke Access]                 │  ← User can revoke
│                                    │
│ ⏳ Another App                     │  ← Pending approval
│    Package: com.another.app        │
│    Status: Pending Approval        │
│    [Approve] [Deny]                │
│                                    │
│ ❌ Blocked App                     │  ← User denied
│    Package: com.blocked.app        │
│    Status: Denied                  │
│    [Allow Access]                  │
│                                    │
└────────────────────────────────────┘
```

---

## 🔐 Security Model

### Three Tiers of Access

#### Tier 1: Forge OS Internal (Highest)
- **Package:** `com.forge.os` (Forge OS itself)
- **Access:** Everything (no restrictions)
- **Method:** Direct internal calls
- **UI:** Not shown in external apps list

#### Tier 2: Trusted System Apps (High)
- **Packages:** `com.forge.autophone`, potentially others
- **Access:** Full tool suite, high rate limits
- **Method:** AIDL with auto-granted status
- **UI:** Shown as "Trusted (System)", cannot revoke
- **Example:** AutoPhone

#### Tier 3: External Apps (Limited)
- **Packages:** Any third-party app
- **Access:** Limited tools, normal rate limits
- **Method:** AIDL with user approval required
- **UI:** Shown with approve/deny/revoke controls
- **Examples:** Task automation apps, testing tools

---

## 🎯 Benefits of This Approach

### 1. Single Source of Truth ✅
- One permission system (Forge OS's `ExternalCallerRegistry`)
- No duplicate permission storage
- Consistent UI for all external access

### 2. Proper Architecture ✅
- AutoPhone is an external app (from Android's perspective)
- But it's trusted (from Forge OS's perspective)
- Uses same AIDL interface as other apps
- Just gets automatic approval + full capabilities

### 3. User Transparency ✅
- Users see AutoPhone in external apps list
- Clearly marked as "Trusted (System)"
- Cannot accidentally revoke it
- Understand it's part of Forge OS ecosystem

### 4. Audit Trail ✅
- All AutoPhone ←→ Forge OS communication logged
- Part of Forge OS's `ExternalAuditLog`
- Debugging and monitoring
- Security auditing

### 5. Future Flexibility ✅
- Can add more trusted system apps
- Can adjust AutoPhone's capabilities
- Can implement fine-grained control if needed
- Same framework scales

---

## 🔄 Communication Flow

### When AutoPhone Needs to Use Forge OS Tools

```
1. AutoPhone wants to execute tool
   ↓
2. Binds to Forge OS AIDL service
   IForgeOsService.Stub.asInterface(binder)
   ↓
3. Calls invokeTool(toolName, jsonArgs)
   ↓
4. Forge OS ExternalApiBridge.authorize(uid, "invokeTool", toolName)
   ↓
5. ExternalCallerRegistry.observe(uid)
   ├─ Package: com.forge.autophone
   ├─ Auto-grant if not already granted
   └─ Return ExternalCaller with full capabilities
   ↓
6. Authorization succeeds (Decision.Allow)
   ↓
7. Tool executes
   ↓
8. Result returned to AutoPhone
   ↓
9. Audit log recorded
```

### When Third-Party App Tries to Use Forge OS

```
1. Unknown app wants to execute tool
   ↓
2. Binds to Forge OS AIDL service
   ↓
3. Calls invokeTool(toolName, jsonArgs)
   ↓
4. ExternalApiBridge.authorize(uid, "invokeTool", toolName)
   ↓
5. ExternalCallerRegistry.observe(uid)
   ├─ Package: com.example.app
   ├─ Status: PENDING (first time)
   └─ Show approval dialog to user
   ↓
6. User decides:
   ├─ Approve → Status = GRANTED, continue
   └─ Deny → Status = DENIED, reject
```

---

## 📝 Implementation Checklist

### In AutoPhone

- [ ] Remove `permissions/PermissionManager.kt`
- [ ] Update UI to remove permission cards
- [ ] Add AIDL client to bind to Forge OS
- [ ] Document that AutoPhone connects via AIDL
- [ ] Update integration docs

### In Forge OS

- [ ] Add AutoPhone package to trusted list
- [ ] Auto-grant full capabilities to AutoPhone
- [ ] Show AutoPhone in external apps UI as "Trusted (System)"
- [ ] Prevent user from revoking AutoPhone access
- [ ] Document AutoPhone as trusted system component

### Documentation

- [ ] Update PERMISSIONS_AND_INTEGRATION.md
- [ ] Create FORGE_OS_AIDL_CLIENT.md
- [ ] Document ExternalApiBridge integration
- [ ] Update architecture diagrams
- [ ] Add example code for AIDL binding

---

## 💡 Key Insight

**AutoPhone is not a separate permission layer.**

**AutoPhone is a trusted external app** that:
- Uses Forge OS's existing External API system
- Gets automatic approval (GRANTED status)
- Has full capabilities (all tools, high limits)
- Cannot be revoked by user
- Shows up in Forge OS's external apps UI

This is the **correct architecture** because:
1. Reuses existing, tested permission system
2. Maintains single source of truth
3. Proper separation of concerns
4. Auditable and transparent
5. Scales for future trusted apps

---

**🏗️ Architecture: One Permission System, Multiple Trust Levels 🔐**
