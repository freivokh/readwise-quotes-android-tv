// app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt
package com.readwisequotes.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.readwisequotes.R
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.data.SyncResult
import com.readwisequotes.settings.QuoteFilter
import com.readwisequotes.settings.SettingsManager
import com.readwisequotes.settings.TagFilterMode
import com.readwisequotes.settings.VisualStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : FragmentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    private lateinit var backButton: Button
    private lateinit var apiTokenInput: EditText
    private lateinit var syncButton: Button
    private lateinit var syncStatus: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var tagSelectionContainer: LinearLayout
    private lateinit var tagGroupsContainer: LinearLayout
    private lateinit var createGroupButton: Button
    private lateinit var selectedTagsText: TextView
    private lateinit var styleSpinner: Spinner
    private lateinit var durationSeekBar: SeekBar
    private lateinit var durationValue: TextView
    private lateinit var syncIntervalSpinner: Spinner
    private lateinit var toggleTokenVisibility: Button

    private var availableTags: List<String> = emptyList()
    private var isTokenVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        setupListeners()
        loadCurrentSettings()

        // Ensure initial focus for D-pad navigation
        backButton.post {
            backButton.requestFocus()
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val currentFilter = settingsManager.getQuoteFilter()
            val tagSectionHidden = currentFilter != QuoteFilter.BY_TAG

            // Check if focus is within the spinner by walking up the view hierarchy
            val filterHasFocus = isDescendantOfView(currentFocus, filterSpinner)
            val styleHasFocus = isDescendantOfView(currentFocus, styleSpinner)

            // Handle D-pad DOWN from filter spinner when tag section is hidden
            if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN &&
                filterHasFocus && tagSectionHidden) {
                styleSpinner.requestFocus()
                return true
            }

            // Handle D-pad UP from style spinner when tag section is hidden
            if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP &&
                styleHasFocus && tagSectionHidden) {
                filterSpinner.requestFocus()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isDescendantOfView(child: View?, parent: View): Boolean {
        if (child == null) return false
        if (child == parent) return true
        var current: android.view.ViewParent? = child.parent
        while (current != null) {
            if (current == parent) return true
            current = current.parent
        }
        return false
    }

    private fun bindViews() {
        backButton = findViewById(R.id.backButton)
        apiTokenInput = findViewById(R.id.apiTokenInput)
        syncButton = findViewById(R.id.syncButton)
        syncStatus = findViewById(R.id.syncStatus)
        filterSpinner = findViewById(R.id.filterSpinner)
        tagSelectionContainer = findViewById(R.id.tagSelectionContainer)
        tagGroupsContainer = findViewById(R.id.tagGroupsContainer)
        createGroupButton = findViewById(R.id.createGroupButton)
        selectedTagsText = findViewById(R.id.selectedTagsText)
        styleSpinner = findViewById(R.id.styleSpinner)
        durationSeekBar = findViewById(R.id.durationSeekBar)
        durationValue = findViewById(R.id.durationValue)
        syncIntervalSpinner = findViewById(R.id.syncIntervalSpinner)
        toggleTokenVisibility = findViewById(R.id.toggleTokenVisibility)
    }

    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Toggle token visibility
        toggleTokenVisibility.setOnClickListener {
            isTokenVisible = !isTokenVisible
            if (isTokenVisible) {
                apiTokenInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleTokenVisibility.text = "🙈"
            } else {
                apiTokenInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleTokenVisibility.text = "👁"
            }
            // Move cursor to end after changing input type
            apiTokenInput.setSelection(apiTokenInput.text.length)
        }

        // API Token - verify on Enter key (not on focus change for TV navigation)
        apiTokenInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val token = apiTokenInput.text.toString()
                if (token.isNotEmpty()) {
                    verifyAndSaveToken(token)
                }
                true
            } else {
                false
            }
        }

        // Sync button
        syncButton.setOnClickListener {
            val inputToken = apiTokenInput.text.toString()
            val savedToken = settingsManager.getApiToken()
            if (inputToken.isNotEmpty() && inputToken != savedToken) {
                // Token was entered but not saved yet - verify and save first
                verifyAndSaveToken(inputToken)
            } else {
                performSync()
            }
        }

        // Filter spinner
        val filterAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.filter_all),
                getString(R.string.filter_favorites),
                getString(R.string.filter_tags),
                getString(R.string.filter_recent)
            )
        )
        filterSpinner.adapter = filterAdapter
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filter = QuoteFilter.entries[position]
                settingsManager.setQuoteFilter(filter)
                updateTagSelectionVisibility(filter)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Create group button
        createGroupButton.setOnClickListener {
            showCreateGroupDialog()
        }

        // Style spinner
        val styleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.style_ambient), getString(R.string.style_minimal))
        )
        styleSpinner.adapter = styleAdapter
        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val style = VisualStyle.entries[position]
                settingsManager.setVisualStyle(style)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Duration seek bar
        durationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                durationValue.text = getString(R.string.duration_format, progress)
                if (fromUser) {
                    settingsManager.setQuoteDuration(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Sync interval spinner
        val intervalAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("1 hour", "6 hours", "24 hours", "Manual only")
        )
        syncIntervalSpinner.adapter = intervalAdapter
        syncIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val hours = when (position) {
                    0 -> 1
                    1 -> 6
                    2 -> 24
                    else -> Int.MAX_VALUE
                }
                settingsManager.setSyncIntervalHours(hours)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCurrentSettings() {
        // Token (show masked)
        val token = settingsManager.getApiToken()
        if (token.isNotEmpty()) {
            apiTokenInput.setText(token)
        }

        // Sync status
        updateSyncStatus()

        // Filter
        val currentFilter = settingsManager.getQuoteFilter()
        filterSpinner.setSelection(currentFilter.ordinal)
        updateTagSelectionVisibility(currentFilter)

        // Tag settings
        renderTagGroups()
        updateSelectedTagsDisplay()
        loadAvailableTags()

        // Style
        styleSpinner.setSelection(settingsManager.getVisualStyle().ordinal)

        // Duration
        val duration = settingsManager.getQuoteDuration()
        durationSeekBar.progress = duration
        durationValue.text = getString(R.string.duration_format, duration)

        // Sync interval
        val intervalPosition = when (settingsManager.getSyncIntervalHours()) {
            1 -> 0
            6 -> 1
            24 -> 2
            else -> 3
        }
        syncIntervalSpinner.setSelection(intervalPosition)
    }

    private fun updateSyncStatus() {
        lifecycleScope.launch {
            val lastSync = settingsManager.getLastSyncTime()
            val count = quoteRepository.getQuoteCount()

            val timeText = if (lastSync != null) {
                try {
                    val instant = Instant.parse(lastSync)
                    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                } catch (e: Exception) {
                    getString(R.string.never_synced)
                }
            } else {
                getString(R.string.never_synced)
            }

            syncStatus.text = "$timeText ($count quotes)"
        }
    }

    private fun verifyAndSaveToken(token: String) {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncStatus.text = "Verifying token..."

            val isValid = quoteRepository.verifyToken(token)
            if (isValid) {
                settingsManager.setApiToken(token)
                syncStatus.text = "Token verified!"
                performSync()
            } else {
                syncStatus.text = "Invalid token"
                Toast.makeText(this@SettingsActivity, "Invalid API token", Toast.LENGTH_SHORT).show()
            }
            syncButton.isEnabled = true
        }
    }

    private fun performSync() {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncStatus.text = getString(R.string.syncing)

            when (val result = quoteRepository.sync()) {
                is SyncResult.Success -> {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Synced ${result.count} quotes",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAvailableTags()
                }
                is SyncResult.Error -> {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Sync failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            updateSyncStatus()
            syncButton.isEnabled = true
        }
    }

    private fun loadAvailableTags() {
        lifecycleScope.launch {
            availableTags = quoteRepository.getAllTags()
        }
    }

    private fun updateTagSelectionVisibility(filter: QuoteFilter) {
        val isTagFilter = filter == QuoteFilter.BY_TAG

        // 1. Update visibility
        tagSelectionContainer.visibility = if (isTagFilter) View.VISIBLE else View.GONE

        // 2. Update focus chain AFTER layout completes (critical for D-pad navigation)
        filterSpinner.post {
            if (isTagFilter) {
                // Tag section visible: filter → first group or createGroupButton → style
                // (renderTagGroups() will set filterSpinner.nextFocusDownId to first group or createGroupButton)
                styleSpinner.nextFocusUpId = R.id.createGroupButton
            } else {
                // Tag section hidden: filter → style directly
                filterSpinner.nextFocusDownId = R.id.styleSpinner
                styleSpinner.nextFocusUpId = R.id.filterSpinner
            }
        }

        // Load tags if needed
        if (isTagFilter && availableTags.isEmpty()) {
            loadAvailableTags()
        }
    }

    private fun renderTagGroups() {
        tagGroupsContainer.removeAllViews()
        val groups = settingsManager.getTagGroups()

        // Track first and last focusable elements for focus chain
        var firstGroupCheckbox: CheckBox? = null
        var lastGroupDeleteButton: Button? = null
        var previousDeleteButton: Button? = null

        groups.forEachIndexed { index, group ->
            val itemView = layoutInflater.inflate(R.layout.item_tag_group, tagGroupsContainer, false)

            val groupSwitch = itemView.findViewById<CheckBox>(R.id.groupSwitch)
            val groupName = itemView.findViewById<TextView>(R.id.groupName)
            val groupTags = itemView.findViewById<TextView>(R.id.groupTags)
            val matchModeToggle = itemView.findViewById<ToggleButton>(R.id.matchModeToggle)
            val editButton = itemView.findViewById<Button>(R.id.editButton)
            val deleteButton = itemView.findViewById<Button>(R.id.deleteButton)

            // Generate unique IDs for each view to fix focus navigation
            groupSwitch.id = View.generateViewId()
            matchModeToggle.id = View.generateViewId()
            editButton.id = View.generateViewId()
            deleteButton.id = View.generateViewId()

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

            // Set vertical focus navigation between rows
            if (previousDeleteButton != null) {
                // Connect previous row's delete button DOWN to this row's checkbox
                previousDeleteButton!!.nextFocusDownId = groupSwitch.id
                // Connect this row's checkbox UP to previous row's checkbox
                groupSwitch.nextFocusUpId = firstGroupCheckbox?.id ?: R.id.filterSpinner
            }

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

            // Track first/last elements
            if (index == 0) {
                firstGroupCheckbox = groupSwitch
            }
            lastGroupDeleteButton = deleteButton
            previousDeleteButton = deleteButton
        }

        // Connect focus chain: filterSpinner → first group, last group → createGroupButton
        firstGroupCheckbox?.let { first ->
            filterSpinner.post {
                filterSpinner.nextFocusDownId = first.id
                first.nextFocusUpId = R.id.filterSpinner
            }
        }
        lastGroupDeleteButton?.let { last ->
            last.nextFocusDownId = createGroupButton.id
            createGroupButton.nextFocusUpId = last.id
        }

        // If no groups, connect filterSpinner directly to createGroupButton
        if (groups.isEmpty()) {
            filterSpinner.post {
                filterSpinner.nextFocusDownId = R.id.createGroupButton
                createGroupButton.nextFocusUpId = R.id.filterSpinner
            }
        }
    }

    private fun showCreateGroupDialog() {
        if (availableTags.isEmpty()) {
            Toast.makeText(this, "No tags available. Sync your quotes first.", Toast.LENGTH_SHORT).show()
            return
        }

        val existingGroups = settingsManager.getTagGroups()
        val defaultName = "Group ${existingGroups.size + 1}"

        showGroupNameDialog("Create Tag Group", defaultName) { name ->
            showSelectTagsForGroupDialog(name, emptySet())
        }
    }

    private fun showGroupNameDialog(title: String, initialName: String, onNext: (String) -> Unit) {
        val dialog = Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        val view = layoutInflater.inflate(R.layout.dialog_group_name, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val titleView = view.findViewById<TextView>(R.id.dialogTitle)
        val nameInput = view.findViewById<EditText>(R.id.groupNameInput)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val nextButton = view.findViewById<Button>(R.id.nextButton)

        titleView.text = title
        nameInput.setText(initialName)
        nameInput.selectAll()

        cancelButton.setOnClickListener { dialog.dismiss() }
        nextButton.setOnClickListener {
            val name = nameInput.text.toString().trim().ifEmpty { initialName }
            dialog.dismiss()
            onNext(name)
        }

        // Handle Enter key on EditText
        nameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                nextButton.performClick()
                true
            } else false
        }

        dialog.show()
        nameInput.requestFocus()
    }

    private fun showSelectTagsForGroupDialog(groupName: String, existingTags: Set<String>, groupId: String? = null) {
        val selectedTags = existingTags.toMutableSet()

        val dialog = Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        val view = layoutInflater.inflate(R.layout.dialog_tag_selection, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)

        val titleView = view.findViewById<TextView>(R.id.dialogTitle)
        val tagsContainer = view.findViewById<LinearLayout>(R.id.tagsContainer)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val saveButton = view.findViewById<Button>(R.id.saveButton)

        titleView.text = "Select tags for \"$groupName\""

        // Create checkboxes for each tag
        val checkboxes = mutableListOf<CheckBox>()
        availableTags.forEachIndexed { index, tag ->
            val checkBox = layoutInflater.inflate(R.layout.item_tag_checkbox, tagsContainer, false) as CheckBox
            checkBox.text = tag
            checkBox.isChecked = selectedTags.contains(tag)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTags.add(tag) else selectedTags.remove(tag)
            }

            // LEFT from any tag → Cancel, RIGHT from any tag → Save
            checkBox.nextFocusLeftId = cancelButton.id
            checkBox.nextFocusRightId = saveButton.id

            // Vertical navigation at list boundaries
            if (index == 0) {
                saveButton.nextFocusDownId = checkBox.id
                cancelButton.nextFocusDownId = checkBox.id
            }
            if (index == availableTags.size - 1) {
                checkBox.nextFocusDownId = cancelButton.id
                cancelButton.nextFocusUpId = checkBox.id
                saveButton.nextFocusUpId = checkBox.id
            }

            checkboxes.add(checkBox)
            tagsContainer.addView(checkBox)
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        saveButton.setOnClickListener {
            if (selectedTags.isEmpty()) {
                Toast.makeText(this, "Please select at least one tag", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveButton.isEnabled = false

            if (groupId != null) {
                val existingGroup = settingsManager.getTagGroups().find { it.id == groupId }
                if (existingGroup != null) {
                    settingsManager.updateTagGroup(existingGroup.copy(name = groupName, tags = selectedTags))
                }
            } else {
                val newGroup = com.readwisequotes.data.model.TagGroup(
                    name = groupName,
                    tags = selectedTags
                )
                settingsManager.addTagGroup(newGroup)
            }

            dialog.dismiss()
            Toast.makeText(this@SettingsActivity, "Group \"$groupName\" saved!", Toast.LENGTH_SHORT).show()
            renderTagGroups()
            updateSelectedTagsDisplay()
        }

        // Intercept LEFT/RIGHT from checkboxes to navigate to buttons
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val focused = dialog.currentFocus
                val isCheckboxFocused = checkboxes.contains(focused)

                if (isCheckboxFocused) {
                    when (keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            cancelButton.requestFocus()
                            return@setOnKeyListener true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            saveButton.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                }
            }
            false
        }

        dialog.show()

        // Focus first checkbox if available
        if (checkboxes.isNotEmpty()) {
            checkboxes[0].requestFocus()
        }
    }

    private fun showEditGroupDialog(group: com.readwisequotes.data.model.TagGroup) {
        showGroupNameDialog("Edit Group", group.name) { name ->
            if (name.isNotEmpty()) {
                showSelectTagsForGroupDialog(name, group.tags, group.id)
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteGroupConfirmation(group: com.readwisequotes.data.model.TagGroup) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Delete \"${group.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                settingsManager.deleteTagGroup(group.id)
                renderTagGroups()
                updateSelectedTagsDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSelectedTagsDisplay() {
        val enabledGroups = settingsManager.getEnabledTagGroups()
        val allTags = settingsManager.getAllEnabledTags()

        selectedTagsText.text = when {
            enabledGroups.isEmpty() -> "No groups enabled"
            allTags.size <= 5 -> "${enabledGroups.size} group(s): ${allTags.joinToString(", ")}"
            else -> "${enabledGroups.size} group(s): ${allTags.take(5).joinToString(", ")} +${allTags.size - 5} more"
        }
    }
}
