# Forge OS — Complete System Architecture Analysis

## Executive Summary

**Forge OS** is a sophisticated Android AI agent framework that combines:
- A **ReAct agent runtime** with tool registry, skill learning, and reflection
- **Secure Python sandbox** (Chaquopy) for untrusted code execution
- **Multi-channel input** (voice, text, Telegram, companion check-ins)
- **On-device AI inference** with fallback to cloud APIs (OpenAI, Claude, Gemini)
- **Advanced memory systems** (episodic, semantic, skill memory)
- **Permission & security gates** (SecurityPolicy, PermissionManager, SandboxManager)
- **Desktop companion app** (Forge Desktop) for remote control & monitoring
- **System-level accessibility** integration (AutoPhone bridge to AutoPhoneAccessibilityService)

---

## Architecture Overview

### **High-Level Stack**

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                              │
│  MainActivity → ChatScreen | BrowserScreen | SettingsScreen | etc      │
│  Theme: Material Design 3 (Jetpack Compose)                            │
└────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌────────────────────────────────────────────────────────────────────────┐
│                      Domain Layer (Business Logic)                      │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Agent Layer (ReActAgent, ToolProvider, ToolRegistry)           │  │
│  │  ├─ Skill recording & episodic learning                        │  │
│  │  ├─ Reflection engine (errors → memory → behavior change)      │  │
│  │  └─ Tool execution planner                                     │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Tool Providers (Domain-specific)                               │  │
│  │  ├─ FileToolProvider                                           │  │
│  │  ├─ ProjectToolProvider (git, python, web server)              │  │
│  │  ├─ SystemToolProvider (shell, browser, wifi, bluetooth)       │  │
│  │  ├─ AndroidToolProvider (AutoPhone bridge)                     │  │
│  │  ├─ CompanionToolProvider (voice, notifications)               │  │
│  │  └─ ExternalToolProvider (MCP, custom bridges)                 │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Memory Systems                                                  │  │
│  │  ├─ EpisodicMemory (conversation history, actions taken)       │  │
│  │  ├─ LongtermMemory (facts, patterns, learnings)                │  │
│  │  ├─ SkillMemory (recorded procedures, reusable steps)          │  │
│  │  ├─ ConversationIndex (semantic search, context ranking)       │  │
│  │  └─ MemoryArchive (persistent disk storage)                    │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Companion System                                                │  │
│  │  ├─ PersonaManager (personality selection)                     │  │
│  │  ├─ RelationshipState (familiarity, emotional context)         │  │
│  │  ├─ CompanionVoice (TTS, voice selection)                      │  │
│  │  ├─ DependencyMonitor (proactive check-ins)                    │  │
│  │  ├─ SafetyFilter (crisis lines, mental health)                 │  │
│  │  └─ EpisodicMemoryStore (companion-specific context)           │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ Configuration & Control                                         │  │
│  │  ├─ ForgeConfig (all feature toggles, rate limits, gates)      │  │
│  │  ├─ BehaviorRules (confirmDestructive, autoConfirm lists)      │  │
│  │  ├─ AgentControlPlane (user consent, verification engine)      │  │
│  │  ├─ UserConsentLedger (decision audit trail)                   │  │
│  │  └─ ConfigMutationEngine (safe config updates)                 │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌────────────────────────────────────────────────────────────────────────┐
│                          Data Layer                                    │
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ Sandbox Manager      │  │ Security Policy      │                   │
│  │  └─ resolveSafe()    │  │  └─ validateUrl(),   │                   │
│  │  └─ Python Runner    │  │    validateGitUrl(), │                   │
│  │  └─ Shell Executor   │  │    validateDelete()  │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ API Manager          │  │ Web/Browser Layer    │                   │
│  │  └─ BYOK endpoints   │  │  └─ HeadlessBrowser  │                   │
│  │  └─ Cost metering    │  │  └─ WebScreenshotter │                   │
│  │  └─ Error handling   │  │  └─ BrowserHistory   │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐                   │
│  │ Android Interfaces   │  │ External Bridges     │                   │
│  │  └─ AutoPhoneConn    │  │  └─ ForgeBridgeDiscov│                   │
│  │  └─ AndroidController│  │  └─ ExternalApiBridge│                   │
│  │  └─ System Services  │  │  └─ MCP Client       │                   │
│  └──────────────────────┘  └──────────────────────┘                   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Channel & Communication                                         │  │
│  │  ├─ CompanionConversationRepository (Telegram, check-ins)       │  │
│  │  ├─ VoiceInputManager (wake-word, speech recognition)          │  │
│  │  ├─ AgentNotifier (notification delivery, action registry)      │  │
│  │  └─ HeartbeatMonitor (health checks, background tasks)          │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Scheduled Execution                                             │  │
│  │  ├─ CronManager, CronScheduler                                  │  │
│  │  ├─ AlarmManager (exact alarms via Android AlarmManager)        │  │
│  │  ├─ WorkManager for background workers                          │  │
│  │  │   ├─ HeartbeatWorker                                         │  │
│  │  │   ├─ CronExecutionWorker                                     │  │
│  │  │   ├─ CompanionCheckInWorker                                  │  │
│  │  │   └─ MemoryCompressionWorker                                 │  │
│  │  └─ ProactiveScheduler (pattern-based task synthesis)           │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Persistence                                                     │  │
│  │  ├─ Room Database (conversations, cron jobs, alarms)            │  │
│  │  ├─ LocationDatabase (GPS fixes for clustering)                 │  │
│  │  ├─ File storage (tool_usage.json, backups, logs)               │  │
│  │  └─ Backup/Sync (BackupManager, MultiDeviceSyncManager)         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Core Components Deep Dive

### 1. **Agent Runtime** (`domain/agent/*`)

#### **ReActAgent**
- Orchestrates the ReAct (Reasoning + Acting) loop
- Receives user input → plans actions → executes tools → observes → reflects → loops
- Maintains execution history for reflection & skill learning
- Supports multi-turn conversations with context window management

#### **ToolRegistry & ToolProvider**
- Central registry for all AI-accessible tools
- Tools grouped by domain (file, project, system, android, companion, etc.)
- Each ToolProvider validates inputs, checks permissions, and dispatches actions
- Examples:
  - `FileToolProvider` — read, write, delete files with SecurityPolicy validation
  - `ProjectToolProvider` — git operations, python execution, web server hosting
  - `AndroidToolProvider` — bridges to AutoPhoneAccessibilityService (tap, swipe, type)

#### **Skill Recorder & Episodic Learning**
- Records agent actions in SkillMemory
- Stores multi-step procedures (e.g., "clone git repo → build → test")
- Agent can replay & refine recorded skills
- Linked to episodic memory for context

#### **Reflection Engine**
- Triggered on tool failures or user corrections
- Analyzes what went wrong
- Updates mental models & adjusts future behavior
- Stores insights in LongtermMemory

---

### 2. **Memory Systems** (`domain/memory/*`)

#### **EpisodicMemory**
- Stores conversation turns, tool outputs, observations
- Indexed by timestamp and semantic content
- Enables "remember what we did yesterday"

#### **LongtermMemory**
- Persistent facts, patterns, learnings
- Examples: "user prefers Python", "project X uses GitHub Actions"
- Semantic factindex for efficient retrieval

#### **SkillMemory**
- Recorded multi-step procedures
- Learnable: agent refines skills through practice
- Reusable: can be invoked by name in future tasks

#### **ConversationIndex & ContextReranker**
- Semantic search over past conversations
- Ranks relevant context by relevance for token budget
- Powers the "I remember you saying..." recall

---

### 3. **Security Architecture** (`data/sandbox/`, `domain/security/`)

#### **SecurityPolicy** (`data/sandbox/SecurityPolicy.kt`)
- Validates URLs (SSRF guards on curl, browser, web tools)
- Validates Git URLs (no `file://`, `..` traversal)
- Validates project paths (no `../`, absolute paths forbidden)
- Validates delete operations (confirm before rm -rf)
- Validates phone numbers (exact match for SMS auth)

#### **PermissionManager** (`domain/security/PermissionManager.kt`)
- Tracks tool usage per day (rate limiting, audit trail)
- `checkFileRead()` → boolean (allowed/denied)
- `checkFileWrite()` / `checkFileDelete()` → PermissionCheckResult (allowed/denied with reason)
- Persistent daily usage log to `tool_usage.json`

#### **SandboxManager** (`data/sandbox/SandboxManager.kt`)
- **`resolveSafe()` — SUSPEND FUNCTION**
  - Accepts file paths, URLs, project configs
  - Resolves paths safely (no traversal)
  - Must be called from suspend functions only
  - Returns clean, safe paths for downstream code
- Bridges to Python sandbox & shell execution

#### **ForgeConfig & BehaviorRules**
```kotlin
BehaviorRules(
  confirmDestructive = listOf("delete_file", "delete_directory", "sms_send", "location_share", ...),
  autoConfirmToolCalls = false, // User must approve each tool
  networkPermissions = NetworkPermissions(
    allowedDomains = listOf("github.com", "api.openai.com", ...),
    blockVpn = true
  ),
  filePermissionRules = FilePermissionRules(
    readGlobs = listOf("/workspace/**", "/cache/**"),
    writeGlobs = listOf("/workspace/**", ...),
    deleteGlobs = listOf() // No auto-deletes
  )
)
```
- All security controls are **configurable** — agent always gets clear errors

---

### 4. **Sandbox & Execution** (`data/sandbox/*`)

#### **PythonRunner** (Chaquopy)
- Executes untrusted Python scripts
- Embeds Python 3.11 runtime
- Sandboxed to workspace directory
- Import restrictions via `security.py`

#### **ShellExecutor**
- Executes shell commands (bash/sh)
- Sanitized through `validateCommand()`
- Restricted to workspace & whitelisted tools
- Timeout & memory caps

#### **Example Flow:**
```
Agent: "Run tests for this project"
  → ProjectToolProvider.runTests()
    → SandboxManager.resolveSafe(projectPath)
    → PythonRunner.execute("pytest tests/")
      → Chaquopy runtime + workspace sandbox
      → Captures stdout/stderr
    → Returns test results
  → Agent observes & reflects
```

---

### 5. **Companion System** (`domain/companion/*`)

#### **Persona & Relationship**
- Selectable personas (e.g., "supportive mentor", "productivity buddy", "creative partner")
- RelationshipState tracks familiarity, emotional connection
- PersonaManager switches personas based on user preference

#### **Voice & TTS**
- CompanionVoice selects TTS engine and accent
- Integrates with Android's TextToSpeech API
- Capped at 4000 characters per message (config)

#### **Proactive Check-ins**
- DependencyMonitor analyzes user patterns
- Suggests check-ins if user hasn't engaged
- Sends notifications with contextual questions

#### **Safety Filter**
- Detects crisis language (suicidal ideation, abuse)
- Escalates to crisis lines (embedded `crisis_lines.json`)
- Prevents harmful companion responses

---

### 6. **Channels & Communication** (`domain/channels/*`, `data/conversations/*`)

#### **Multi-Channel Input**
- **Chat**: In-app conversation (ChatScreen)
- **Voice**: Wake-word detection → speech recognition
- **Telegram**: External channel integration
- **Companion**: Check-in notifications with quick actions

#### **Channel Session Overrides**
- Per-channel model selection (e.g., Claude on Telegram, Gemini on voice)
- Rate limiting per channel
- Message styling per channel

---

### 7. **Scheduled Execution** (`domain/cron/*`, `domain/alarms/*`)

#### **Cron Jobs**
- User-defined schedules (5-minute intervals to daily)
- Persisted to database
- Executed by CronExecutionWorker (background)

#### **Alarms**
- Exact alarms via Android AlarmManager
- Can wake device from doze
- Useful for time-critical tasks

#### **Proactive Scheduler**
- PatternAnalyzer watches user behavior
- Suggests automatable tasks
- Synthesizes skills into cron jobs

---

### 8. **Android Integration** (`data/android/*`)

#### **AutoPhone Bridge**
```kotlin
AutoPhoneConnection(service: AutoPhoneAccessibilityService)
  ├─ performTap(x, y)
  ├─ performSwipe(x1, y1, x2, y2)
  ├─ performTypeText(text, viewId)
  ├─ findNode(id, text)
  └─ takeScreenshot()
```
- Connects to the `forge-autophone` module
- Enables agent to control foreground app UI

#### **AndroidController**
- Enumerate installed apps
- Query app state
- Integrate with Android OS features

---

### 9. **Desktop Companion** (`forge-desktop/`)

**Technology Stack:**
- **Frontend**: Vite + TypeScript + React
- **Backend**: Python mock server (for demo)
- **Desktop**: Tauri (Rust wrapper around web UI)
- **Communication**: WebSocket or HTTP to Forge OS Android service

**Features:**
- Remote control of agent (send commands, view state)
- Conversation history sync
- Device state monitoring

---

### 10. **External Integrations**

#### **MCP (Model Context Protocol)** (`data/mcp/*`)
- Standardized tool interface from Claude/Anthropic
- Allows third-party tools (e.g., `web-requests`, `git`, `gmail`)
- McpClient wraps MCP servers
- Tools exposed through agent registry

#### **Forge Bridge Discovery**
- Detects companion desktop app on LAN
- Establishes bidirectional tunnel
- Enables remote agent access

#### **External API Calls**
- BYOK (Bring Your Own Keys) for OpenAI, Claude, Gemini, Mistral
- Cost metering & budget alerts
- Fallback hierarchy if primary model fails

---

## Security Audit Status

### **Recent Fixes (Commits 995ff4a & 6491bc0)**

**4 CRITICAL issues:**
- ✅ SSRF guards on `curl_exec`, `browser_navigate`, `browser_get_html`
- ✅ Anti-theft tools (SIM swap, SMS wipe) added to `confirmDestructive`
- ✅ SMS authentication hardened (exact phone match, 60s confirmation window)
- ✅ Master wipe requires explicit user gesture

**7 HIGH issues:**
- ✅ `resolveSafe()` added to `web_screenshot`, `project_serve`, `backup_import`, `sync_import`
- ✅ PermissionManager wired into FileToolProvider
- ✅ Git URL slug validation
- ✅ Contacts rate limit (30/hr)
- ✅ Location & contacts in `confirmDestructive`

**7 MEDIUM issues:**
- ✅ `validateGitUrl()` on `git_clone`
- ✅ `validateDelete()` on `rename`, `moveToTrash`
- ✅ Composio action name regex validation
- ✅ TTS capped at 4000 chars

**5 LOW issues:**
- ✅ Vibration capped at 5 seconds

---

## Build & Deployment

### **Android Build**
- **AGP**: 8.2.2
- **Kotlin**: 1.9.22
- **Target SDK**: 34 (latest stability)
- **Min SDK**: 26 (API 26 = Android 8.0)
- **Java**: 17
- **Gradle**: 8.4

### **CI/CD Pipeline** (`.github/workflows/android.yml`)
1. Checkout code
2. Set up JDK 17, Android SDK, NDK 25.2
3. **Keystore strategy:**
   - If `DEBUG_KEYSTORE_B64` secret exists → restore & verify
   - Otherwise → generate fresh keystore, upload as artifact (first-time setup)
4. Build debug APK with:
   - `-Pkotlin.incremental=false` (full compile, avoid stale daemon cache)
   - `-Dkotlin.compiler.execution.strategy=in-process` (bypass compile daemon)
5. Verify APK signing cert SHA-256 (ensures same key across builds)
6. Upload APK as artifact (30-day retention)

### **Python Integration**
- Chaquopy 16.0.0 classpath plugin
- Embeds Python 3.11 runtime
- Gradle task: `./gradlew :app:assembleDebug`

---

## Directory Structure

```
forge-os-main/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          ← Permission declarations
│   │   ├── aidl/                        ← AIDL interfaces (if any)
│   │   ├── java/com/forge/os/
│   │   │   ├── ForgeApplication.kt      ← @HiltAndroidApp entry point
│   │   │   ├── data/                    ← Repository/Service layer
│   │   │   │   ├── sandbox/             ← Security policy, SandboxManager, PythonRunner
│   │   │   │   ├── api/                 ← BYOK API manager
│   │   │   │   ├── android/             ← AutoPhone bridge
│   │   │   │   ├── mcp/                 ← MCP client
│   │   │   │   ├── browser/             ← HeadlessBrowser, WebScreenshotter
│   │   │   │   └── web/                 ← HTTP server, project serve
│   │   │   ├── domain/                  ← Business logic
│   │   │   │   ├── agent/               ← ReActAgent, ToolRegistry, ToolProvider
│   │   │   │   ├── memory/              ← Memory systems
│   │   │   │   ├── companion/           ← Persona, voice, check-ins
│   │   │   │   ├── channels/            ← Telegram, voice input
│   │   │   │   ├── security/            ← PermissionManager
│   │   │   │   ├── cron/                ← Cron jobs & scheduling
│   │   │   │   ├── config/              ← ForgeConfig, BehaviorRules
│   │   │   │   └── tools/               ← Tool implementations
│   │   │   ├── presentation/            ← Jetpack Compose UI
│   │   │   │   ├── screens/             ← Chat, Browser, Settings, etc.
│   │   │   │   ├── components/          ← Reusable Compose widgets
│   │   │   │   └── theme/               ← Material Design 3 theming
│   │   │   ├── external/                ← Intent API bridge
│   │   │   ├── security/                ← Anti-theft, SMS receiver
│   │   │   ├── service/                 ← Background services
│   │   │   └── di/                      ← Hilt @Module
│   │   ├── python/
│   │   │   ├── forge_sdk.py             ← Python SDK for scripts
│   │   │   └── forge_sandbox/           ← Sandbox restrictions
│   │   │       ├── security.py          ← Import restrictions
│   │   │       ├── file_ops.py          ← Safe file operations
│   │   │       └── python_runner.py     ← Execution context
│   │   └── res/
│   │       ├── values/                  ← Strings, colors, styles
│   │       ├── drawable/                ← App icons, splash logo
│   │       ├── xml/                     ← Device admin, backup rules, network config
│   │       └── mipmap-*/                ← Launcher icons (hdpi, xxhdpi, etc.)
│   ├── build.gradle                     ← App module build config
│   └── proguard-rules.pro                ← ProGuard rules for Kotlin/Hilt/Room
├── build.gradle                         ← Root Gradle script (AGP, Kotlin, Hilt plugins)
├── settings.gradle                      ← Gradle settings
├── gradlew / gradlew.bat                 ← Gradle wrapper
├── .github/workflows/
│   └── android.yml                      ← CI/CD pipeline
├── forge-desktop/                       ← Tauri desktop companion app
│   ├── src/                             ← TypeScript + React UI
│   ├── package.json
│   ├── vite.config.ts
│   └── tauri.conf.json
├── docs/
│   ├── EXTERNAL_API.md                  ← Intent API documentation
│   ├── COMPANION.md                     ← Companion mode design
│   ├── feature-audit.md                 ← Security audit results
│   └── roadmap_suggestions.md           ← Future features
├── fastlane/                            ← App store automation (Play Store)
├── AppIcons/                            ← Icon assets (Android + iOS)
├── .gitignore
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
└── README.md
```

---

## Key Design Patterns

### **1. Repository Pattern**
- Data access abstracted behind repositories
- Example: `ConversationRepository`, `ProjectRepository`, `ConfigRepository`

### **2. Dependency Injection (Hilt)**
- All services provided via Hilt @Module
- Testable with mock injection
- Singleton lifecycle for heavy components

### **3. MVVM (Presentation)**
- ViewModels manage screen state
- Compose collects state as Flow/StateFlow
- Clear separation of concerns

### **4. Tool Provider Pattern**
- Each domain has a ToolProvider (FileToolProvider, ProjectToolProvider, etc.)
- ToolProviders validate inputs & dispatch to service layer
- Centralizes permission checks

### **5. Suspend Functions for Async**
- Heavy I/O (file, network, sandbox) uses suspend functions
- `SandboxManager.resolveSafe()` is suspend — callers must respect this
- Coroutine scopes managed by lifecycleOwner

### **6. Configuration as Code**
- ForgeConfig drives all feature toggles
- No hardcoded thresholds — all configurable
- ConfigMutationEngine validates updates safely

---

## Permissions Granted by Agent

### **System Permissions** (Android Manifest)
- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`
- `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`
- `QUERY_ALL_PACKAGES` (enumerate installed apps)

### **Tool Permissions** (PermissionManager)
- Tracked per tool per day
- Rate limits enforced (e.g., 30 contacts/hour)
- Daily usage logged to `tool_usage.json`

### **Confirmation Gates** (ForgeConfig.BehaviorRules)
- `confirmDestructive`: Delete, SMS send, anti-theft wipe, location share, contacts export
- `autoConfirmToolCalls`: Default false (user approves each tool)

---

## Workflow Example: "Set a Reminder to Check in Tomorrow"

```
User: "Set a reminder to check in with me tomorrow at 9 AM about my mood"
                            ↓
                    ChatScreen captures input
                            ↓
                    AgentPersonality loads
                            ↓
    ReActAgent.plan() → needs cron job + companion check-in
                            ↓
    ToolRegistry.createCronJob({
      schedule: "tomorrow 09:00",
      command: "check_mood_companion"
    })
                            ↓
    CronManager.addJob() → Room database insert
                            ↓
    UserConsentLedger.log("cron.create" → approved)
                            ↓
    CronScheduler.schedule() → sets exact alarm
                            ↓
    Tomorrow 09:00 → AlarmManager triggers CronExecutionWorker
                            ↓
    Worker calls: CompanionVoice.speak("How's your mood today?")
                            ↓
    NotificationActionRegistry shows buttons: [Positive, Neutral, Negative]
                            ↓
    User taps → AgentNotificationBuilder captures → EpisodicMemory updated
                            ↓
    Agent reflects → "User had a rough day" → stores in LongtermMemory
```

---

## Future Roadmap (from `roadmap_suggestions.md`)

- [ ] Multi-agent delegation with SubAgent framework
- [ ] Advanced memory compression & long-horizon planning
- [ ] Proactive skill synthesis from usage patterns
- [ ] Cross-device sync with multi-device lock
- [ ] Forge Bridge enhancements (lower latency, better reliability)
- [ ] More MCP server integrations
- [ ] Advanced code review via CompanionCodeReviewManager
- [ ] Gesture recognition (on-device ML via ML Kit)

---

## Conclusion

Forge OS is a **production-grade AI agent framework** that combines:
1. **Sophisticated agent runtime** with reflection, skill learning, and memory
2. **Strong security model** with sandboxing, permission gates, and confirmation flows
3. **Rich I/O capabilities** (voice, text, system access, UI automation)
4. **Extensible architecture** (MCP, Forge Bridge, tool providers)
5. **User-centric design** (companion personas, relationship tracking, safety filters)

The codebase is well-structured, follows Android best practices, and demonstrates thoughtful security design for a powerful on-device AI agent.
