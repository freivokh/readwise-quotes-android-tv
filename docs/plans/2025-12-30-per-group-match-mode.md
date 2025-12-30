# Per-Group Match Mode Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move ANY/ALL matching from global toggle to per-group setting

**Architecture:** Add `matchMode` field to TagGroup model, add toggle button to each group row, update query builder to construct per-group conditions joined with OR

**Tech Stack:** Kotlin, Room, Android XML layouts

---

## Task 1: Update TagGroup Model

**Files:**
- Modify: `app/src/main/java/com/readwisequotes/data/model/TagGroup.kt`

**Step 1: Add matchMode field**

```kotlin
// app/src/main/java/com/readwisequotes/data/model/TagGroup.kt
package com.readwisequotes.data.model

import com.readwisequotes.settings.TagFilterMode
import java.util.UUID

data class TagGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tags: Set<String>,
    val isEnabled: Boolean = false,
    val matchMode: TagFilterMode = TagFilterMode.ANY
)
```

**Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 2: Add Toggle Button to Group Row Layout

**Files:**
- Modify: `app/src/main/res/layout/item_tag_group.xml`

**Step 1: Add ToggleButton between groupTags and editButton**

Replace the entire file with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingVertical="12dp"
    android:paddingHorizontal="4dp"
    android:focusable="false"
    android:descendantFocusability="afterDescendants">

    <CheckBox
        android:id="@+id/groupSwitch"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginEnd="12dp"
        android:focusable="true" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/groupName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/text_primary"
            android:textSize="14sp" />

        <TextView
            android:id="@+id/groupTags"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/text_secondary"
            android:textSize="11sp"
            android:maxLines="1"
            android:ellipsize="end" />

    </LinearLayout>

    <ToggleButton
        android:id="@+id/matchModeToggle"
        android:layout_width="50dp"
        android:layout_height="32dp"
        android:textOn="ALL"
        android:textOff="ANY"
        android:textSize="10sp"
        android:textColor="@color/text_primary"
        android:background="@drawable/focusable_button_background"
        android:focusable="true"
        android:layout_marginStart="8dp" />

    <Button
        android:id="@+id/editButton"
        android:layout_width="wrap_content"
        android:layout_height="36dp"
        android:text="Edit"
        android:textSize="12sp"
        android:textColor="@color/text_primary"
        android:background="@drawable/focusable_button_background"
        android:focusable="true"
        android:paddingHorizontal="12dp"
        android:layout_marginStart="8dp" />

    <Button
        android:id="@+id/deleteButton"
        android:layout_width="wrap_content"
        android:layout_height="36dp"
        android:text="Del"
        android:textSize="12sp"
        android:textColor="@color/text_primary"
        android:background="@drawable/focusable_button_background"
        android:focusable="true"
        android:paddingHorizontal="12dp"
        android:layout_marginStart="4dp" />

</LinearLayout>
```

---

## Task 3: Remove Global Match Mode Toggle from Settings Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_settings.xml`

**Step 1: Remove the Match mode LinearLayout section**

Remove lines ~145-180 (the LinearLayout containing tagModeToggle and help text).

Find and delete:
```xml
            <!-- Match mode toggle -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Match mode:"
                    android:textColor="@color/text_secondary"
                    android:textSize="12sp"
                    android:layout_marginEnd="12dp" />

                <ToggleButton
                    android:id="@+id/tagModeToggle"
                    android:layout_width="60dp"
                    android:layout_height="36dp"
                    android:textOn="ALL"
                    android:textOff="ANY"
                    android:textSize="11sp"
                    android:textColor="@color/text_primary"
                    android:background="@drawable/focusable_button_background"
                    android:focusable="true"
                    android:nextFocusUp="@id/filterSpinner" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="(ANY = or, ALL = and)"
                    android:textColor="@color/text_secondary"
                    android:textSize="10sp"
                    android:layout_marginStart="8dp" />

            </LinearLayout>
```

**Step 2: Update filterSpinner nextFocusDown**

Change `android:nextFocusDown="@id/tagModeToggle"` to point to first group or createGroupButton (will be handled programmatically in SettingsActivity).

---

## Task 4: Update SettingsActivity - Remove Global Toggle References

**Files:**
- Modify: `app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt`

**Step 1: Remove tagModeToggle field declaration**

Find and remove the line declaring `tagModeToggle` (likely in the lateinit vars section around line 50-70).

**Step 2: Remove tagModeToggle binding in onCreate**

Find and remove: `tagModeToggle = findViewById(R.id.tagModeToggle)`

**Step 3: Remove tagModeToggle listener setup**

Find and remove (around line 173-177):
```kotlin
        // Tag mode toggle (ANY/ALL)
        tagModeToggle.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) TagFilterMode.ALL else TagFilterMode.ANY
            settingsManager.setTagFilterMode(mode)
        }
```

**Step 4: Remove tagModeToggle from loadCurrentSettings**

Find and remove (around line 240):
```kotlin
        tagModeToggle.isChecked = settingsManager.getTagFilterMode() == TagFilterMode.ALL
```

**Step 5: Update renderTagGroups focus chain**

Remove all references to `tagModeToggle` in focus navigation. Update to connect `filterSpinner` directly to first group checkbox.

---

## Task 5: Update SettingsActivity - Add Per-Group Toggle Handling

**Files:**
- Modify: `app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt`

**Step 1: Update renderTagGroups to handle matchModeToggle**

In `renderTagGroups()`, add toggle handling after finding views:

```kotlin
private fun renderTagGroups() {
    tagGroupsContainer.removeAllViews()
    val groups = settingsManager.getTagGroups()

    var firstGroupCheckbox: CheckBox? = null
    var lastGroupDeleteButton: Button? = null

    groups.forEachIndexed { index, group ->
        val itemView = layoutInflater.inflate(R.layout.item_tag_group, tagGroupsContainer, false)

        val groupSwitch = itemView.findViewById<CheckBox>(R.id.groupSwitch)
        val groupName = itemView.findViewById<TextView>(R.id.groupName)
        val groupTags = itemView.findViewById<TextView>(R.id.groupTags)
        val matchModeToggle = itemView.findViewById<ToggleButton>(R.id.matchModeToggle)
        val editButton = itemView.findViewById<Button>(R.id.editButton)
        val deleteButton = itemView.findViewById<Button>(R.id.deleteButton)

        groupName.text = group.name
        groupTags.text = group.tags.joinToString(", ")
        groupSwitch.isChecked = group.isEnabled
        matchModeToggle.isChecked = group.matchMode == TagFilterMode.ALL

        // Set horizontal focus navigation within the row
        groupSwitch.nextFocusRightId = matchModeToggle.id
        matchModeToggle.nextFocusLeftId = groupSwitch.id
        matchModeToggle.nextFocusRightId = editButton.id
        editButton.nextFocusLeftId = matchModeToggle.id
        editButton.nextFocusRightId = deleteButton.id
        deleteButton.nextFocusLeftId = editButton.id

        groupSwitch.setOnCheckedChangeListener { _, _ ->
            settingsManager.toggleTagGroup(group.id)
            updateSelectedTagsDisplay()
        }

        matchModeToggle.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) TagFilterMode.ALL else TagFilterMode.ANY
            settingsManager.updateTagGroupMatchMode(group.id, newMode)
        }

        editButton.setOnClickListener {
            showEditGroupDialog(group)
        }

        deleteButton.setOnClickListener {
            showDeleteGroupConfirmation(group)
        }

        tagGroupsContainer.addView(itemView)

        if (index == 0) {
            firstGroupCheckbox = groupSwitch
        }
        if (index == groups.size - 1) {
            lastGroupDeleteButton = deleteButton
        }
    }

    // Connect focus chain: filterSpinner → first group, last group → createGroupButton
    firstGroupCheckbox?.let { first ->
        filterSpinner.nextFocusDownId = first.id
        first.nextFocusUpId = filterSpinner.id
    }
    lastGroupDeleteButton?.let { last ->
        last.nextFocusDownId = createGroupButton.id
        createGroupButton.nextFocusUpId = last.id
    }

    // If no groups, connect filterSpinner directly to createGroupButton
    if (groups.isEmpty()) {
        filterSpinner.nextFocusDownId = createGroupButton.id
        createGroupButton.nextFocusUpId = filterSpinner.id
    }
}
```

---

## Task 6: Add updateTagGroupMatchMode to SettingsManager

**Files:**
- Modify: `app/src/main/java/com/readwisequotes/settings/SettingsManager.kt`

**Step 1: Add updateTagGroupMatchMode method**

Add after `updateTagGroup`:

```kotlin
fun updateTagGroupMatchMode(groupId: String, mode: TagFilterMode) {
    val groups = getTagGroups().toMutableList()
    val index = groups.indexOfFirst { it.id == groupId }
    if (index != -1) {
        groups[index] = groups[index].copy(matchMode = mode)
        saveTagGroups(groups)
    }
}
```

---

## Task 7: Update QuoteRepository Query Builder

**Files:**
- Modify: `app/src/main/java/com/readwisequotes/data/QuoteRepository.kt`

**Step 1: Replace getQuotesByTags with per-group logic**

```kotlin
private fun getQuotesByTags(): Flow<List<Quote>> {
    val enabledGroups = settingsManager.getEnabledTagGroups()

    if (enabledGroups.isEmpty()) {
        // Fall back to all quotes if no groups enabled
        return quoteDao.getAllQuotes()
    }

    return buildGroupedTagQuery(enabledGroups)
}

private fun buildGroupedTagQuery(groups: List<TagGroup>): Flow<List<Quote>> {
    // Build per-group conditions, then OR them together
    val groupConditions = groups.map { group ->
        val tagConditions = group.tags.map { "tags LIKE '%$it%'" }
        val operator = if (group.matchMode == TagFilterMode.ANY) " OR " else " AND "
        "(${tagConditions.joinToString(operator)})"
    }

    val whereClause = groupConditions.joinToString(" OR ")
    val query = SimpleSQLiteQuery("SELECT * FROM quotes WHERE $whereClause ORDER BY RANDOM()")

    return quoteDao.getQuotesByTagsRaw(query)
}
```

**Step 2: Remove old buildTagQuery method (or keep for manual tag selection fallback)**

**Step 3: Add TagGroup import**

Add at top of file:
```kotlin
import com.readwisequotes.data.model.TagGroup
```

---

## Task 8: Clean Up - Remove Unused Global TagFilterMode Methods

**Files:**
- Modify: `app/src/main/java/com/readwisequotes/settings/SettingsManager.kt`

**Step 1: Remove global tag filter mode methods (optional - can keep for backwards compat)**

Remove if not needed elsewhere:
```kotlin
fun getTagFilterMode(): TagFilterMode { ... }
fun setTagFilterMode(mode: TagFilterMode) { ... }
```

And remove `KEY_TAG_FILTER_MODE` constant.

---

## Task 9: Build and Test

**Step 1: Build the project**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Install and test**

Run: `./gradlew installDebug`

Test cases:
1. Create a group with ANY mode - verify quotes match any tag
2. Create a group with ALL mode - verify quotes match all tags
3. Create two groups with different modes - verify OR between groups
4. Toggle match mode on existing group - verify query updates

---

## Task 10: Commit

**Step 1: Stage and commit**

```bash
git add -A
git commit -m "feat: add per-group ANY/ALL match mode

- Add matchMode field to TagGroup model
- Add toggle button to each group row
- Remove global match mode toggle
- Update query builder for per-group matching
- Groups are OR'd together, tags within group use group's mode"
```

