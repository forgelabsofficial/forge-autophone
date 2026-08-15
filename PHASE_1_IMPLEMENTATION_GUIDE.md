# Phase 1 Implementation Guide

## What We Built

Phase 1 adds **4 major capability groups** to AutoPhone, transforming it from a basic accessibility wrapper into an intelligent automation framework.

---

## 1. OCR Text Recognition (`OcrTextExtractor`)

### **Problem Solved**
Many Android apps don't expose proper accessibility labels:
- Games with custom UI
- Image-heavy apps (social media, shopping)
- Apps with poor accessibility support
- Text rendered as images or canvas drawings

### **Solution**
ML Kit Text Recognition extracts visible text from screenshots, even when accessibility tree is empty.

### **API**

```kotlin
// Extract all text on screen
suspend fun ocrReadScreen(): List<OcrTextBlock>

// Find specific text (case-insensitive)
suspend fun ocrFindText(query: String): OcrTextBlock?

// Find all occurrences
suspend fun ocrFindAllText(query: String): List<OcrTextBlock>

// Find and tap in one call
suspend fun ocrTapText(query: String): Boolean
```

### **Data Structures**

```kotlin
data class OcrTextBlock(
    val text: String,
    val bounds: Rect,
    val confidence: Float,      // 0.0 - 1.0
    val lines: List<OcrTextLine>,
    val centerX: Float,         // Computed property
    val centerY: Float          // Computed property
)

data class OcrTextLine(
    val text: String,
    val bounds: Rect,
    val confidence: Float
)
```

### **Usage Examples**

#### Basic text extraction
```kotlin
val tools = AutoPhoneToolRegistry(service)

// Read everything on screen
val blocks = tools.ocrReadScreen()
blocks.forEach { block ->
    println("Found: '${block.text}' at (${block.centerX}, ${block.centerY}) " +
            "confidence: ${block.confidence}")
}
```

#### Find and interact with text
```kotlin
// Find a button by text (even if it's an image button)
val signInButton = tools.ocrFindText("Sign In")
if (signInButton != null) {
    tools.tap(signInButton.centerX, signInButton.centerY)
}

// Or use the shorthand
if (tools.ocrTapText("Continue")) {
    println("Tapped 'Continue' button")
} else {
    println("'Continue' not found on screen")
}
```

#### Handle multiple matches
```kotlin
// Find all "Delete" buttons (e.g., in a list of items)
val deleteButtons = tools.ocrFindAllText("Delete")
println("Found ${deleteButtons.size} delete buttons")

// Tap the first one
deleteButtons.firstOrNull()?.let { button ->
    tools.tap(button.centerX, button.centerY)
}
```

### **Performance Considerations**

- **Processing time**: ~200-500ms per screenshot on modern devices
- **Memory**: ~5-10 MB per frame
- **Battery**: Minimal impact if used sparingly (< 1 fps)
- **Best practice**: Cache results when possible, don't OCR every frame

### **Limitations**

- Works best with clear, high-contrast text
- Struggles with handwriting, heavily stylized fonts, or rotated text
- Confidence scores < 0.7 may be unreliable
- Does not handle non-Latin scripts as well (use language-specific models if needed)

---

## 2. Smart Wait Strategies (`SmartWaiter`)

### **Problem Solved**
Brittle automation with hardcoded delays:
```kotlin
// BAD: Prone to race conditions
tools.tap(submitButton.centerX, submitButton.centerY)
delay(2000) // Hope the UI updates in 2 seconds
val result = tools.findById("result_text")
```

### **Solution**
Semantic waits that poll for conditions or listen to events.

### **API**

```kotlin
// Generic condition waiter
suspend fun waitUntil(
    timeoutMs: Long = 10000,
    pollIntervalMs: Long = 100,
    condition: suspend () -> Boolean
): Boolean

// Wait for UI to settle
suspend fun waitUntilIdle(
    timeoutMs: Long = 5000,
    idleDurationMs: Long = 500
): Boolean

// Wait for window (app) switch
suspend fun waitForWindow(
    packageName: String,
    timeoutMs: Long = 5000
): Boolean

// Wait for node to appear
suspend fun waitForNode(
    viewId: String,
    timeoutMs: Long = 5000
): NodeSnapshot?

// Wait for text to appear
suspend fun waitForText(
    text: String,
    timeoutMs: Long = 5000
): NodeSnapshot?

// Wait for text change in a node
suspend fun waitForTextChange(
    viewId: String,
    timeoutMs: Long = 5000
): String?

// Wait for dialog
suspend fun waitForDialog(
    timeoutMs: Long = 5000
): Boolean

// Wait for toast message
suspend fun waitForToast(
    text: String,
    timeoutMs: Long = 5000
): String?

// Wait for any node matching predicate
suspend fun waitForNodeMatching(
    timeoutMs: Long = 5000,
    predicate: (NodeSnapshot) -> Boolean
): NodeSnapshot?
```

### **Usage Examples**

#### Wait for UI to settle after action
```kotlin
// Submit form
tools.tap(submitButton.centerX, submitButton.centerY)

// Wait for animations/loading to finish
tools.waitUntilIdle(timeoutMs = 5000)

// Now safe to read result
val result = tools.findById("result_message")
```

#### Wait for specific element
```kotlin
// Wait for success message to appear
val successNode = tools.waitForNode("com.example:id/success_banner", timeoutMs = 10000)
if (successNode != null) {
    println("Success: ${successNode.text}")
} else {
    println("Timeout: success banner never appeared")
}
```

#### Wait for text content
```kotlin
// Wait for specific text to appear anywhere
val welcomeNode = tools.waitForText("Welcome back", timeoutMs = 5000)
if (welcomeNode != null) {
    println("Login successful")
}
```

#### Wait for toast notifications
```kotlin
// Perform action
tools.tap(saveButton.centerX, saveButton.centerY)

// Wait for confirmation toast
val toast = tools.waitForToast("Saved", timeoutMs = 3000)
if (toast != null) {
    println("Got confirmation: $toast")
}
```

#### Wait for dynamic text updates
```kotlin
// Start process
tools.tap(startButton.centerX, startButton.centerY)

// Wait for status field to change from "Idle" to "Running"
val newStatus = tools.waitForTextChange("com.example:id/status", timeoutMs = 5000)
println("Status changed to: $newStatus")
```

#### Wait for dialogs
```kotlin
// Trigger action that may show dialog
tools.tap(deleteButton.centerX, deleteButton.centerY)

// Wait for confirmation dialog
if (tools.waitForDialog(timeoutMs = 2000)) {
    // Dialog appeared, find and tap "Confirm"
    val confirmButton = tools.ocrFindText("Confirm")
    confirmButton?.let { tools.tap(it.centerX, it.centerY) }
}
```

#### Custom conditions
```kotlin
// Wait for any clickable button to appear
val button = tools.waitForNodeMatching { node ->
    node.isClickable && node.text?.contains("Submit", ignoreCase = true) == true
}
```

### **Best Practices**

1. **Always use timeouts** — Prevents infinite hangs
2. **Prefer semantic waits over delays** — More reliable and faster
3. **Combine with OCR** — Wait for text that's not in accessibility tree
4. **Chain waits** — Wait for idle → wait for node → interact

```kotlin
// Good pattern
tools.waitUntilIdle()
val node = tools.waitForNode("com.example:id/button")
node?.let { tools.tap(it.centerX, it.centerY) }
```

---

## 3. Smart Scrolling (`ScrollHelper`)

### **Problem Solved**
Manual scroll loops are verbose and error-prone:
```kotlin
// BAD: Manual scroll loop
repeat(20) {
    val found = service.findByText("John Doe").firstOrNull()
    if (found != null) return found
    scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    delay(300)
}
```

### **Solution**
High-level scroll operations with automatic retry and detection.

### **API**

```kotlin
// Scroll until condition is met
suspend fun scrollUntilFound(
    scrollableId: String,
    targetMatcher: (NodeSnapshot) -> Boolean,
    maxScrolls: Int = 20,
    scrollDelayMs: Long = 300
): NodeSnapshot?

// Scroll until text is found
suspend fun scrollUntilText(
    scrollableId: String,
    text: String,
    maxScrolls: Int = 20
): NodeSnapshot?

// Scroll until view ID is found
suspend fun scrollUntilViewId(
    scrollableId: String,
    targetViewId: String,
    maxScrolls: Int = 20
): NodeSnapshot?

// Scroll to top (slow, methodical)
suspend fun scrollToTop(
    scrollableId: String,
    maxScrolls: Int = 50,
    scrollDelayMs: Long = 200
)

// Scroll to bottom (slow, methodical)
suspend fun scrollToBottom(
    scrollableId: String,
    maxScrolls: Int = 50,
    scrollDelayMs: Long = 200
)

// Fling to top (fast, momentum-based)
suspend fun flingToTop(scrollableId: String)

// Fling to bottom (fast, momentum-based)
suspend fun flingToBottom(scrollableId: String)

// Check scroll capability
fun canScrollForward(scrollableId: String): Boolean
fun canScrollBackward(scrollableId: String): Boolean
```

### **Usage Examples**

#### Find item in a list
```kotlin
// Scroll through contacts until "John Doe" is found
val contact = tools.scrollUntilText(
    scrollableId = "com.android.contacts:id/list",
    text = "John Doe",
    maxScrolls = 30
)

if (contact != null) {
    println("Found John Doe at position (${contact.centerX}, ${contact.centerY})")
    tools.tap(contact.centerX, contact.centerY)
} else {
    println("John Doe not found after 30 scrolls")
}
```

#### Navigate to top/bottom
```kotlin
// Go to top of feed
tools.scrollToTop("com.example:id/recycler_view")

// Or use fast fling (for long lists)
tools.flingToTop("com.example:id/recycler_view")
tools.waitUntilIdle() // Wait for fling animation to settle
```

#### Check if more content is available
```kotlin
val listId = "com.example:id/list"

if (tools.canScrollForward(listId)) {
    println("More content below")
} else {
    println("Reached end of list")
}

if (tools.canScrollBackward(listId)) {
    println("Can scroll back up")
}
```

#### Custom scroll conditions
```kotlin
// Scroll until a node with specific className is found
val videoNode = tools.scrollUntilFound(
    scrollableId = "com.example:id/feed",
    targetMatcher = { node ->
        node.className == "com.example.VideoView" && node.isEnabled
    },
    maxScrolls = 50
)
```

#### Handling infinite scroll
```kotlin
// Load all items (with safety limit)
var previousCount = 0
var currentCount = 0
val allItems = mutableListOf<NodeSnapshot>()

repeat(10) { attempt ->
    // Get visible items
    val snapshot = UITreeInspector(service).snapshot()
    val items = snapshot.filter { it.viewId == "com.example:id/item" }
    allItems.addAll(items)
    currentCount = allItems.size
    
    // Check if we're still loading new content
    if (currentCount == previousCount) {
        println("No new items loaded, reached end")
        break
    }
    previousCount = currentCount
    
    // Scroll down
    tools.scrollToBottom("com.example:id/list", maxScrolls = 5)
    tools.waitUntilIdle()
}

println("Loaded ${allItems.size} total items")
```

### **Best Practices**

1. **Always specify maxScrolls** — Prevents infinite loops
2. **Use fling for long lists** — Faster than incremental scrolls
3. **Combine with waitUntilIdle** — Ensures content is loaded
4. **Check scroll capability first** — Avoids unnecessary attempts

---

## 4. Real-Time Event Streaming (`UIEventBus`)

### **Problem Solved**
Polling-based UI inspection is inefficient:
```kotlin
// BAD: Polling every 100ms
while (true) {
    val dialog = service.findById("dialog").firstOrNull()
    if (dialog != null) break
    delay(100)
}
```

### **Solution**
Reactive event stream powered by `AccessibilityEvent` callbacks.

### **Event Types**

```kotlin
sealed class UIEvent {
    data class WindowChanged(packageName, className, timestamp)
    data class NodeAppeared(snapshot, timestamp)
    data class NodeDisappeared(viewId, timestamp)
    data class TextChanged(viewId, oldText, newText, timestamp)
    data class ToastShown(message, timestamp)
    data class ViewClicked(viewId, x, y, timestamp)
    data class ViewFocused(viewId, timestamp)
    data class ViewScrolled(viewId, scrollX, scrollY, timestamp)
    data class NotificationPosted(packageName, title, text, timestamp)
    data class AccessibilityEvent(eventType, packageName, className, timestamp)
}
```

### **API**

```kotlin
// Get event stream
fun observeUIEvents(): Flow<UIEvent>
```

### **Usage Examples**

#### Monitor window switches
```kotlin
tools.observeUIEvents()
    .filterIsInstance<UIEvent.WindowChanged>()
    .onEach { event ->
        println("User switched to ${event.packageName}")
        if (event.packageName == "com.android.chrome") {
            println("User opened Chrome")
        }
    }
    .launchIn(scope)
```

#### Detect toasts
```kotlin
tools.observeUIEvents()
    .filterIsInstance<UIEvent.ToastShown>()
    .onEach { event ->
        println("Toast: ${event.message}")
        
        if (event.message.contains("error", ignoreCase = true)) {
            // React to error toast
            println("ERROR DETECTED: ${event.message}")
        }
    }
    .launchIn(scope)
```

#### Track text input
```kotlin
tools.observeUIEvents()
    .filterIsInstance<UIEvent.TextChanged>()
    .filter { it.viewId == "com.example:id/search_field" }
    .onEach { event ->
        println("Search query updated: ${event.oldText} -> ${event.newText}")
    }
    .launchIn(scope)
```

#### React to clicks
```kotlin
tools.observeUIEvents()
    .filterIsInstance<UIEvent.ViewClicked>()
    .onEach { event ->
        println("User clicked ${event.viewId} at (${event.x}, ${event.y})")
    }
    .launchIn(scope)
```

#### Combined pattern: Wait for event
```kotlin
// Wait for specific event (reactive wait)
suspend fun waitForWindowChange(targetPackage: String): UIEvent.WindowChanged {
    return tools.observeUIEvents()
        .filterIsInstance<UIEvent.WindowChanged>()
        .first { it.packageName == targetPackage }
}

// Usage
val event = waitForWindowChange("com.android.settings")
println("Settings app opened at ${event.timestamp}")
```

#### Event logging for debugging
```kotlin
// Log all events to console
tools.observeUIEvents()
    .onEach { event ->
        val time = SimpleDateFormat("HH:mm:ss.SSS").format(Date(event.timestamp))
        println("[$time] ${event::class.simpleName}: $event")
    }
    .launchIn(scope)
```

### **Integration with SmartWaiter**

`SmartWaiter` internally uses `UIEventBus` for efficient waiting:

```kotlin
// Under the hood, this listens to UIEvent.WindowChanged
tools.waitForWindow("com.example.app")

// This listens to UIEvent.ToastShown
tools.waitForToast("Success")
```

You can combine both approaches:

```kotlin
// Start listening
val eventJob = tools.observeUIEvents()
    .filterIsInstance<UIEvent.ToastShown>()
    .onEach { println("Toast: ${it.message}") }
    .launchIn(scope)

// Perform actions
tools.tap(button.centerX, button.centerY)

// Wait for specific outcome
val toast = tools.waitForToast("Saved", timeoutMs = 5000)

// Stop listening
eventJob.cancel()
```

---

## Integration with Forge OS Agent

### Tool Registration

Update your `AndroidToolProvider` in Forge OS:

```kotlin
class AndroidToolProvider(private val autoPhone: AutoPhoneToolRegistry) : ToolProvider {
    
    override fun getTools(): List<Tool> = listOf(
        // Existing tools
        Tool("tap", ::tap),
        Tool("swipe", ::swipe),
        Tool("type_text", ::typeText),
        
        // New OCR tools
        Tool("ocr_read_screen", ::ocrReadScreen),
        Tool("ocr_find_text", ::ocrFindText),
        Tool("ocr_tap_text", ::ocrTapText),
        
        // New wait tools
        Tool("wait_until_idle", ::waitUntilIdle),
        Tool("wait_for_node", ::waitForNode),
        Tool("wait_for_text", ::waitForText),
        Tool("wait_for_toast", ::waitForToast),
        
        // New scroll tools
        Tool("scroll_until_text", ::scrollUntilText),
        Tool("scroll_to_top", ::scrollToTop),
        Tool("scroll_to_bottom", ::scrollToBottom),
        Tool("fling_to_top", ::flingToTop),
    )
    
    private suspend fun ocrReadScreen(args: Map<String, Any>): List<OcrTextBlock> {
        return autoPhone.ocrReadScreen()
    }
    
    private suspend fun ocrFindText(args: Map<String, Any>): OcrTextBlock? {
        val query = args["query"] as String
        return autoPhone.ocrFindText(query)
    }
    
    // ... implement other tool wrappers
}
```

### Agent Prompt Updates

Add new capabilities to system prompt:

```markdown
## AutoPhone Capabilities (Enhanced)

You can control the Android device's UI through these tools:

### Visual Recognition
- `ocr_read_screen()`: Extract all visible text on screen (even if not in accessibility tree)
- `ocr_find_text(query)`: Find text anywhere on screen, returns bounds and center coordinates
- `ocr_tap_text(query)`: Find and tap text in one operation

### Smart Waiting
- `wait_until_idle(timeout_ms)`: Wait for UI to settle (no animations/loading)
- `wait_for_node(view_id, timeout_ms)`: Wait for element to appear
- `wait_for_text(text, timeout_ms)`: Wait for text to appear
- `wait_for_toast(text, timeout_ms)`: Wait for toast notification

### Smart Scrolling
- `scroll_until_text(scrollable_id, text)`: Scroll through list until text is found
- `scroll_to_top(scrollable_id)`: Scroll to top of list
- `fling_to_bottom(scrollable_id)`: Fast scroll to bottom

### Usage Patterns

1. **OCR-first approach**: If accessibility tree is empty, use OCR
2. **Always wait for idle**: After taps/swipes, call `wait_until_idle()`
3. **Use semantic waits**: Replace `delay()` with `wait_for_*()`
4. **Scroll intelligently**: Use `scroll_until_text()` instead of manual loops
```

### Example Agent Workflow

```
User: "Find and tap the Sign In button"

Agent reasoning:
1. Try accessibility tree first: `find_by_text("Sign In")`
2. If not found, fall back to OCR: `ocr_find_text("Sign In")`
3. Tap at center: `tap(x, y)`
4. Wait for result: `wait_until_idle()`
5. Verify: `wait_for_text("Welcome")` or check for error toast

Agent actions:
Tool: find_by_text("Sign In") → null
Tool: ocr_find_text("Sign In") → OcrTextBlock{centerX: 540, centerY: 1200}
Tool: tap(540, 1200) → success
Tool: wait_until_idle(5000) → success
Tool: wait_for_text("Welcome", 5000) → NodeSnapshot{text: "Welcome back, John"}

Agent: "Successfully signed in. Welcome message appeared."
```

---

## Testing

### Unit Tests

```bash
./gradlew test
```

Tests cover:
- OCR text block geometry calculations
- UIEvent hierarchy and timestamps
- ScrollHelper scroll detection logic
- SmartWaiter condition evaluation

### Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

Tests require:
- Android emulator or device
- Accessibility service enabled
- Sample app for testing scrolling, OCR, etc.

---

## Performance Impact

| Feature | CPU | Memory | Battery | Notes |
|---------|-----|--------|---------|-------|
| OCR (per frame) | ~5-10% | ~10 MB | Low | Use sparingly |
| Event Streaming | <1% | ~2 MB | Negligible | Always-on is fine |
| Smart Waiting | <1% | <1 MB | Negligible | Polls at 10 Hz |
| Smart Scrolling | ~2-5% | <1 MB | Low | During scroll only |

**Recommendations:**
- OCR: Use only when accessibility tree is insufficient
- Events: Keep stream active, filter downstream
- Waits: Prefer over polling loops
- Scroll: Use fling for long lists (faster)

---

## Troubleshooting

### OCR not finding text
- Check confidence score (should be > 0.7)
- Ensure text is visible (not behind overlay)
- Try different lighting/contrast
- Verify ML Kit model is downloaded

### Waits timing out
- Increase timeout value
- Check if UI is actually changing
- Use event stream to debug what's happening
- Verify node ID/text is correct

### Scroll not finding target
- Increase maxScrolls limit
- Check if target is actually in list
- Verify scrollable ID is correct
- Use fling if list is very long

### Events not firing
- Verify accessibility service is enabled
- Check if app blocks accessibility events
- Confirm event type is correct
- Look at raw AccessibilityEvent logs

---

## Next Steps (Phase 2)

- Icon/image recognition (OpenCV template matching)
- Multi-finger gestures (pinch, zoom, rotate)
- App context awareness (detect screens, UI patterns)
- Action verification & rollback
- UI hierarchy diffing (track state changes)

See [AUTOPHONE_IMPROVEMENT_ROADMAP.md](./AUTOPHONE_IMPROVEMENT_ROADMAP.md) for full roadmap.
