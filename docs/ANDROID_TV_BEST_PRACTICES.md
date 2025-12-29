# Android TV Development Best Practices Reference Guide

> Comprehensive research compiled December 2025 for building high-quality Android TV applications.

---

## Table of Contents

1. [D-Pad Focus Navigation](#1-d-pad-focus-navigation)
2. [Leanback Library](#2-leanback-library)
3. [Open Source Reference Apps](#3-open-source-reference-apps)
4. [UI/UX Design Guidelines](#4-uiux-design-guidelines)
5. [Jetpack Compose for TV](#5-jetpack-compose-for-tv)
6. [Common Pitfalls & Solutions](#6-common-pitfalls--solutions)
7. [Testing & Quality](#7-testing--quality)
8. [Project-Specific Gotchas](#8-project-specific-gotchas-readwise-quotes-app)

---

## 1. D-Pad Focus Navigation

### Core Principles

Android TV relies entirely on focus-based navigation. Users interact via D-pad (up, down, left, right) and select button - no touch.

**The Three Focus States:**
- **Default**: Unfocused, normal appearance
- **Focused**: Element ready for interaction (must be visually obvious)
- **Pressed**: Element being activated

### Focus Traversal Algorithm

Android uses `FocusFinder` to determine next focusable view:
1. Finds currently focused view via `findFocus()`
2. Uses `focusSearch()` based on proximity
3. Calculates geometrically closest focusable view in requested direction
4. Calls `requestFocus()` on found view

```kotlin
// Manual focus search example
val currentFocused = findFocus()
val nextFocused = FocusFinder.getInstance().findNextFocus(
    viewGroup,
    currentFocused,
    View.FOCUS_DOWN
)
nextFocused?.requestFocus()
```

### XML Focus Attributes

```xml
<Button
    android:id="@+id/myButton"
    android:focusable="true"
    android:nextFocusUp="@id/aboveButton"
    android:nextFocusDown="@id/belowButton"
    android:nextFocusLeft="@id/leftButton"
    android:nextFocusRight="@id/rightButton" />
```

**When to use explicit focus attributes:**
- Default proximity-based focus doesn't work well
- Specific navigation pattern required
- Elements not aligned in clear grid
- Creating focus loops

**When NOT to use:**
- Simple vertical/horizontal layouts (let framework handle)
- Views that can become `GONE` (causes RuntimeException)
- Before testing default behavior

### Handling Hidden/GONE Views

**CRITICAL**: When `nextFocus*` references a `GONE` view, you get a RuntimeException.

**Solution - Dynamic Focus Chain Updates:**

```kotlin
private fun updateTagSelectionVisibility(filter: QuoteFilter) {
    val isVisible = filter == QuoteFilter.BY_TAG
    tagSelectionContainer.visibility = if (isVisible) View.VISIBLE else View.GONE

    // CRITICAL: Disable focusability on hidden elements
    tagModeToggle.isFocusable = isVisible
    createGroupButton.isFocusable = isVisible

    // Update focus chain dynamically
    if (isVisible) {
        filterSpinner.nextFocusDownId = R.id.tagModeToggle
        styleSpinner.nextFocusUpId = R.id.createGroupButton
    } else {
        filterSpinner.nextFocusDownId = R.id.styleSpinner
        styleSpinner.nextFocusUpId = R.id.filterSpinner
    }
}
```

### ScrollView Focus Handling

```xml
<ScrollView
    android:descendantFocusability="afterDescendants"
    android:focusable="false"
    android:focusableInTouchMode="false">
    <!-- Content -->
</ScrollView>
```

**descendantFocusability options:**
- `beforeDescendants`: ViewGroup gets focus before children
- `afterDescendants`: ViewGroup gets focus only if no children want it (RECOMMENDED)
- `blocksDescendants`: ViewGroup blocks children from getting focus

### Initial Focus Management

Always define starting focus point:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

    // Ensure initial focus
    findViewById<View>(R.id.firstFocusableElement).post {
        it.requestFocus()
    }
}

override fun onResume() {
    super.onResume()
    // Ensure something has focus when returning
    if (currentFocus == null) {
        findViewById<View>(R.id.defaultFocusView)?.requestFocus()
    }
}
```

### Focus Indicator Requirements

Focus must be visually obvious from 10 feet away:

```xml
<!-- drawable/focusable_background.xml -->
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#2A4A6B"/>
            <stroke android:width="2dp" android:color="#4A7AAB"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#1A3A5B"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#333333"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
</selector>
```

**Visual feedback methods:**
- **Scale**: 1.025x, 1.05x, or 1.1x enlargement
- **Color**: Background or border color change
- **Border/Glow**: Outline or glow effect
- **Shadow**: Elevation with drop shadows
- **Animation**: 300ms transition duration

---

## 2. Leanback Library

### Current Status (2025)

**DEPRECATED** - Google officially recommends Jetpack Compose for TV for new projects.

However, Leanback remains functional and widely used in existing applications.

### Core Components

| Component | Purpose | Best For |
|-----------|---------|----------|
| `BrowseFragment` | Main browsing interface with rows | Media browsing apps |
| `DetailsFragment` | Detailed content information | Content detail screens |
| `GuidedStepFragment` | Step-by-step wizards | Setup flows, complex settings |
| `LeanbackSettingsFragment` | Preference-based settings | Traditional app settings |
| `SearchFragment` | Search interface with voice | Content search |

### GuidedStepFragment Example

```kotlin
class SetupWizardStep : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        return Guidance(
            "Welcome",
            "Let's set up your app",
            "Step 1 of 3",
            ResourcesCompat.getDrawable(resources, R.drawable.ic_logo, null)
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?
    ) {
        actions.add(
            GuidedAction.Builder(context)
                .id(ACTION_CONTINUE)
                .title("Continue")
                .build()
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == ACTION_CONTINUE) {
            add(fragmentManager, NextStep())
        }
    }
}
```

### LeanbackSettingsFragment Example

```kotlin
class SettingsFragment : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment())
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader,
            pref.fragment!!
        )
        startPreferenceFragment(fragment)
        return true
    }
}

class PrefsFragment : LeanbackPreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
```

### Dependencies

```kotlin
dependencies {
    val leanbackVersion = "1.2.0"
    implementation("androidx.leanback:leanback:$leanbackVersion")
    implementation("androidx.leanback:leanback-preference:$leanbackVersion")
}
```

### When to Use Leanback vs Custom

**Use Leanback when:**
- Building standard media browsing apps
- Rapid prototyping or MVPs
- Limited customization needs
- Maintaining existing Leanback apps

**Use Custom layouts when:**
- Unique UI requirements
- Complex focus management needs
- Building long-term projects
- Need complete design control

---

## 3. Open Source Reference Apps

### Production Apps to Study

| App | GitHub | Key Learnings |
|-----|--------|---------------|
| **SmartTube** | [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube) | Production settings, custom Leanback, multi-module |
| **Jellyfin Android TV** | [jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv) | Modular preferences, Kotlin patterns |
| **VLC Android** | [videolan/vlc-android](https://github.com/videolan/vlc-android) | Comprehensive settings, SharedPreferences |
| **AerialViews** | [theothernt/AerialViews](https://github.com/theothernt/AerialViews) | Screensaver patterns, Media3, settings |

### Official Google Samples

| Sample | Purpose |
|--------|---------|
| **JetStreamCompose** | Full Compose for TV media app |
| **LeanbackShowcase** | Leanback component demonstrations |
| **TvMaterialCatalog** | Material Design component catalog |
| **ReferenceAppKotlin** | Modern Kotlin patterns |

Repository: [android/tv-samples](https://github.com/android/tv-samples)

### Focus-Specific Libraries

| Library | Purpose |
|---------|---------|
| **CustomTVRecyclerView** | Netflix-like navigation, fixes focus issues |
| **TvRecyclerView** | FocusHighlightHelper with zoom |
| **Android-TV-Focus** | Focus problem solutions |
| **Sofa** | Leanback extensions |

---

## 4. UI/UX Design Guidelines

### The 10-Foot Experience

Users interact from ~10 feet away. **When font is 2x farther, it needs to be 2x bigger.**

### Layout Specifications

**Grid System:**
- 12 columns at 52dp width
- 20dp gutters between columns
- 58dp margins on left/right

**Safe Area Margins (Critical):**
- **5% margin** minimum for overscan
- **48dp** from left/right edges
- **27dp** from top/bottom edges

**Design Resolution:**
- Design at **960 × 540px** (MDPI)
- Scales to HD (1280×720) and 4K (1920×1080)

### Typography

**Minimum Sizes:**
- Absolute minimum: 12sp (avoid)
- Default text: 18sp
- Recommended minimum: 24sp
- Body text baseline: 24px

**Typeface:**
- **Roboto** - official Android TV typeface
- Sans-serif for body text
- Large counters for legibility
- Avoid thin/light typefaces

### Color System

**Dark Theme Preferred:**
- Enhances cinematic experience
- Saves power on TV displays
- Easier on eyes in dark rooms

**Recommendations:**
- Use colors 2-3 levels darker than mobile
- Light gray (#EEEEEE) for text on dark backgrounds
- Avoid pure black (#000000) and pure white (#FFFFFF)
- Use desaturated colors (avoid bright reds)

**Contrast Requirements (WCAG):**
- Small text: 4.5:1 minimum
- Large text: 3:1 minimum
- UI components: 3:1 against adjacent colors

### Focus Indicator Specifications

**Scale Values:**
- Small elements: 1.1x
- Medium elements: 1.05x
- Large elements: 1.025x

**Animation:**
- Default duration: 300ms
- Smooth transitions between states

### Form Input Best Practices

**Preferred Input Methods (in order):**
1. **Voice input** - Most natural for TV
2. **Second screen** - Phone/tablet for complex input
3. **Selection-based** - Spinners, toggles, radio buttons
4. **On-screen keyboard** - Last resort

**Spinners:**
- Use for 3+ choices
- Avoid very long lists
- Consider radio buttons for 2-3 choices

**Toggles:**
- Use Switch (preferred) or ToggleButton
- Clear binary states
- Immediate visual feedback

### Navigation Rules

**D-Pad Design:**
- Give each direction a specific function
- Horizontal: categories
- Vertical: items within category
- Ensure clear directional paths

**Back Button:**
- Use physical remote back button
- Do NOT show virtual back button on screen
- Exception: Cancel button for destructive actions

---

## 5. Jetpack Compose for TV

### Current Status (2025)

**Production-ready** with `androidx.tv:tv-material:1.0.0` stable release.

### Dependencies

```kotlin
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

android {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}
```

### Focus Management APIs

```kotlin
// Basic focusable
Modifier.focusable()

// Focus state detection
Modifier.onFocusChanged { focusState ->
    when {
        focusState.isFocused -> // Current element focused
        focusState.hasFocus -> // Element or child focused
    }
}

// Custom focus requester
val focusRequester = remember { FocusRequester() }
Modifier.focusRequester(focusRequester)
// Later: focusRequester.requestFocus()

// Custom focus properties
Modifier.focusProperties {
    next = customNextFocusRequester
    previous = customPreviousFocusRequester
    left = customLeftFocusRequester
    right = customRightFocusRequester
    canFocus = conditionalBoolean
}

// Focus groups
Modifier.focusGroup()

// Focus restoration
Modifier.focusRestorer()
```

### Basic Compose for TV Example

```kotlin
@Composable
fun SettingsScreen() {
    var showTagSelection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(modifier = Modifier.padding(bottom = 48.dp)) {
            Button(
                onClick = { /* navigate back */ },
                modifier = Modifier.focusable()
            ) {
                Text("Back")
            }
            Text("Settings", fontSize = 28.sp)
        }

        // Filter selection
        Spinner(
            items = filters,
            onItemSelected = { showTagSelection = (it == ByTag) },
            modifier = Modifier.focusable()
        )

        // Conditional section - focus automatically handled!
        if (showTagSelection) {
            Column(modifier = Modifier.focusGroup()) {
                ToggleButton(modifier = Modifier.focusable())
                TagGroupsList()
                Button(
                    onClick = { /* create group */ },
                    modifier = Modifier.focusable()
                ) {
                    Text("+ Create Group")
                }
            }
        }

        // More settings...
    }
}
```

### Focus with Visual Indicator

```kotlin
@Composable
fun FocusableCard(
    title: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 4.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent
            )
            .scale(if (isFocused) 1.05f else 1.0f)
    ) {
        Text(title, modifier = Modifier.padding(16.dp))
    }
}
```

### Pros vs Cons

**Pros:**
- Declarative syntax, less boilerplate (~40% less code)
- All-Kotlin, no XML
- Smart recomposition (better performance)
- Conditional visibility automatically handles focus
- Modern architecture (Hilt, ViewModel, Navigation)
- Official Google support

**Cons:**
- Focus is not part of state (requires workarounds)
- Smaller community than standard Compose
- TV libraries lag ~6 months behind standard
- Some focus modifiers experimental
- Higher memory usage than XML

### Migration Strategy

1. **Start with new features** - Build in Compose
2. **Migrate settings first** - Isolated, good test case
3. **Hybrid approach** - Keep XML main screen initially
4. **Gradual migration** - Screen by screen
5. **Remove Leanback** - When migration complete

---

## 6. Common Pitfalls & Solutions

### Pitfall 1: No Clear Focus Indicator

**Problem:** Users can't tell what's focused

**Solution:** Always provide visual feedback
```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape>
            <solid android:color="@color/focused_background"/>
            <stroke android:width="2dp" android:color="@color/accent"/>
        </shape>
    </item>
    <item>
        <shape>
            <solid android:color="@color/default_background"/>
        </shape>
    </item>
</selector>
```

### Pitfall 2: No Initial Focus

**Problem:** Screen loads with nothing focused

**Solution:** Request focus on default element
```kotlin
override fun onResume() {
    super.onResume()
    if (currentFocus == null) {
        defaultView.requestFocus()
    }
}
```

### Pitfall 3: Focus on GONE Views

**Problem:** `nextFocus*` references invisible view causing RuntimeException

**Solution:** Update focus chain when visibility changes
```kotlin
private fun updateVisibility(visible: Boolean) {
    container.visibility = if (visible) View.VISIBLE else View.GONE

    // Also update focus chain
    previousView.nextFocusDownId = if (visible) R.id.containerFirstItem else R.id.nextSection
}
```

### Pitfall 4: Virtual Back Button

**Problem:** Showing on-screen back button (wasted space)

**Solution:** Remove it - TV remotes have physical back buttons

### Pitfall 5: Breaking Focus with Dialogs

**Problem:** Dialog appears but focus stays on background

**Solution:** Request focus inside dialog
```kotlin
fun showDialog() {
    val dialog = Dialog(context)
    dialog.setContentView(R.layout.my_dialog)
    dialog.show()

    // Critical: Request focus on dialog content
    dialog.findViewById<View>(R.id.firstDialogButton)?.requestFocus()
}
```

### Pitfall 6: Focus Lost During Scroll

**Problem:** RecyclerView loses focus during fast scroll

**Solution:** Use Leanback's `VerticalGridView`/`HorizontalGridView` or custom focus handling
```kotlin
recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
```

### Pitfall 7: Touch-Based Gestures

**Problem:** Implementing swipe, pinch, drag

**Solution:** Don't - they don't exist on TV. Design for D-pad only.

### Pitfall 8: Too Small Text/Elements

**Problem:** Elements unreadable from 10 feet

**Solution:** Follow 10-foot UI guidelines
- Minimum 24sp text
- Large touch targets (48dp minimum)
- High contrast colors

---

## 7. Testing & Quality

### Required Testing Hardware

- **ADT-3**: Official Android TV development device
- **Consumer TV**: Sony, Xiaomi, Nvidia Shield
- **Emulator**: For initial development only

**Why real hardware?** Emulators cannot simulate:
- Actual remote input feel
- Real display characteristics
- Network conditions
- Hardware performance

### Testing Checklist

- [ ] All visible controls navigable with D-pad
- [ ] Focus indicator always visible
- [ ] Scrolling lists work with D-pad up/down
- [ ] Select button selects (doesn't scroll)
- [ ] Navigation paths logical and efficient
- [ ] Back button works correctly
- [ ] Initial focus set on screen load
- [ ] Focus restored when returning to screen
- [ ] Hidden views don't capture focus
- [ ] Text readable from 10 feet
- [ ] Safe areas respected (5% margins)

### Debug Tools

**Enable "Show layout bounds"** in Developer Options:
- Shows current focus with blue X
- Essential for diagnosing focus issues

### ADB Testing Commands

```bash
# D-Pad navigation
adb shell input keyevent KEYCODE_DPAD_UP
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_LEFT
adb shell input keyevent KEYCODE_DPAD_RIGHT

# Select/Enter
adb shell input keyevent KEYCODE_DPAD_CENTER

# Back
adb shell input keyevent KEYCODE_BACK

# Take screenshot
adb exec-out screencap -p > screenshot.png

# View logs
adb logcat -s "YourTag"
```

### Google Play TV Requirements

**Manifest:**
1. Main activity with `CATEGORY_LEANBACK_LAUNCHER` filter
2. Home screen banner (320 × 180 px) in `drawables/xhdpi`
3. No unsupported hardware requirements

**Quality:**
- Follow Android TV design guidelines
- Support 10-foot UI principles
- Proper focus navigation
- Landscape orientation only
- Clear focus indicators

---

## Quick Reference

### Focus Chain Update Pattern

```kotlin
private fun updateSectionVisibility(visible: Boolean) {
    // 1. Update visibility
    section.visibility = if (visible) View.VISIBLE else View.GONE

    // 2. Update focusability of children
    sectionChild1.isFocusable = visible
    sectionChild2.isFocusable = visible

    // 3. Update focus chain
    if (visible) {
        previousElement.nextFocusDownId = R.id.sectionChild1
        nextElement.nextFocusUpId = R.id.sectionChild2
    } else {
        previousElement.nextFocusDownId = R.id.nextElement
        nextElement.nextFocusUpId = R.id.previousElement
    }
}
```

### Minimum Viable Focus Drawable

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#2A4A6B"/>
            <stroke android:width="2dp" android:color="#4A7AAB"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#333333"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
</selector>
```

### Essential Imports

```kotlin
// Focus management
import android.view.View
import android.view.ViewGroup
import android.view.FocusFinder

// Compose for TV
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
```

---

## 8. Project-Specific Gotchas (Readwise Quotes App)

Lessons learned during development of this app:

### Gotcha 1: Spinner.hasFocus() Returns False When Focused

**Problem:** `spinner.hasFocus()` returns `false` even when the Spinner is visually focused, because Spinners have nested child views that actually hold focus.

**Solution:** Use a helper to check if focus is within the Spinner's view hierarchy:

```kotlin
private fun isDescendantOfView(child: View?, parent: View): Boolean {
    if (child == null) return false
    if (child == parent) return true
    var current: ViewParent? = child.parent
    while (current != null) {
        if (current == parent) return true
        current = current.parent
    }
    return false
}

// Usage in dispatchKeyEvent
val filterHasFocus = isDescendantOfView(currentFocus, filterSpinner)
```

### Gotcha 2: CheckBox Consumes LEFT/RIGHT Before nextFocusLeftId

**Problem:** Setting `nextFocusLeftId` and `nextFocusRightId` on CheckBox doesn't work - the CheckBox consumes the key events before focus navigation is consulted.

**Solution:** Intercept at the dialog/activity level with a key listener:

```kotlin
dialog.setOnKeyListener { _, keyCode, event ->
    if (event.action == KeyEvent.ACTION_DOWN) {
        val isCheckboxFocused = checkboxes.contains(dialog.currentFocus)
        if (isCheckboxFocused) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    cancelButton.requestFocus()
                    return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    saveButton.requestFocus()
                    return@setOnKeyListener true
                }
            }
        }
    }
    false
}
```

### Gotcha 3: Dynamic View IDs Need View.generateViewId()

**Problem:** Programmatically created views (like tag group rows) need unique IDs for focus chain to work.

**Solution:** Always generate IDs for dynamic views:

```kotlin
val groupSwitch = itemView.findViewById<CheckBox>(R.id.groupSwitch)
groupSwitch.id = View.generateViewId()  // Critical for focus chain

val editButton = itemView.findViewById<Button>(R.id.editButton)
editButton.id = View.generateViewId()

// Now focus chain works
groupSwitch.nextFocusRightId = editButton.id
```

### Gotcha 4: FocusAwareScrollView for Hidden View Skipping

**Problem:** Standard focus chain (`nextFocusDownId`) still tries to focus hidden views, even with dynamic updates.

**Solution:** Custom ScrollView that overrides `focusSearch()` to skip hidden views:

```kotlin
class FocusAwareScrollView : ScrollView {
    override fun focusSearch(focused: View?, direction: Int): View? {
        val result = findNextVisibleFocusable(focused, direction, 0)
        return result ?: super.focusSearch(focused, direction)
    }

    private fun findNextVisibleFocusable(fromView: View?, direction: Int, depth: Int): View? {
        if (depth > 10 || fromView == null) return null
        val nextFocusId = when (direction) {
            View.FOCUS_UP -> fromView.nextFocusUpId
            View.FOCUS_DOWN -> fromView.nextFocusDownId
            else -> View.NO_ID
        }
        if (nextFocusId == View.NO_ID) return null
        val nextView = findViewById<View>(nextFocusId) ?: return null
        // Key: use isShown() not visibility - checks entire hierarchy
        if (nextView.isShown && nextView.isFocusable) return nextView
        return findNextVisibleFocusable(nextView, direction, depth + 1)
    }
}
```

### Reference: SmartTube Focus Patterns

We studied [SmartTube](https://github.com/yuliskov/SmartTube) for focus handling inspiration:
- Overrides `focusSearch()` to skip hidden views
- Uses XML-based focus chains that persist regardless of visibility
- Checks `getVisibility() == View.VISIBLE` before returning focus candidates

---

## Sources & Further Reading

### Official Documentation
- [Android TV Design](https://developer.android.com/design/ui/tv)
- [TV Navigation](https://developer.android.com/training/tv/get-started/navigation)
- [Focus System](https://developer.android.com/design/ui/tv/guides/styles/focus-system)
- [Compose for TV](https://developer.android.com/training/tv/playback/compose)
- [Leanback Library](https://developer.android.com/training/tv/playback/leanback)
- [TV App Quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality)

### Community Resources
- [Focus Management in Android TV](https://medium.com/@sahar.asadian90/focus-management-in-android-tv-dabbe88482e4)
- [RecyclerView Focus Issues](https://georgimirchev.com/2022/07/07/recyclerview-loses-focus-when-scrolling-fast-or-how-to-use-it-on-android-tv/)
- [Focus as State Pattern](https://alexzaitsev.substack.com/p/focus-as-a-state-new-effective-tv)
- [Migrating Leanback to Compose](https://www.tothenew.com/blog/migrating-from-leanback-to-jetpack-compose-in-android-tv/)

### Sample Repositories
- [android/tv-samples](https://github.com/android/tv-samples)
- [SmartTube](https://github.com/yuliskov/SmartTube)
- [Jellyfin Android TV](https://github.com/jellyfin/jellyfin-androidtv)
- [AerialViews](https://github.com/theothernt/AerialViews)

---

*Last updated: December 2025*
