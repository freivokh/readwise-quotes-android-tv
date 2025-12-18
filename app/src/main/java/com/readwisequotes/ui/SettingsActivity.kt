// app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt
package com.readwisequotes.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.ImageButton
import android.widget.Switch
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
    private lateinit var tagModeToggle: ToggleButton
    private lateinit var selectedTagsText: TextView
    private lateinit var styleSpinner: Spinner
    private lateinit var durationSeekBar: SeekBar
    private lateinit var durationValue: TextView
    private lateinit var syncIntervalSpinner: Spinner

    private var availableTags: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        setupListeners()
        loadCurrentSettings()
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
        tagModeToggle = findViewById(R.id.tagModeToggle)
        selectedTagsText = findViewById(R.id.selectedTagsText)
        styleSpinner = findViewById(R.id.styleSpinner)
        durationSeekBar = findViewById(R.id.durationSeekBar)
        durationValue = findViewById(R.id.durationValue)
        syncIntervalSpinner = findViewById(R.id.syncIntervalSpinner)
    }

    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // API Token - save on focus lost
        apiTokenInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val token = apiTokenInput.text.toString()
                if (token.isNotEmpty()) {
                    verifyAndSaveToken(token)
                }
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

        // Tag mode toggle (ANY/ALL)
        tagModeToggle.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) TagFilterMode.ALL else TagFilterMode.ANY
            settingsManager.setTagFilterMode(mode)
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
        tagModeToggle.isChecked = settingsManager.getTagFilterMode() == TagFilterMode.ALL
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
        tagSelectionContainer.visibility = if (filter == QuoteFilter.BY_TAG) View.VISIBLE else View.GONE
        if (filter == QuoteFilter.BY_TAG && availableTags.isEmpty()) {
            loadAvailableTags()
        }
    }

    private fun renderTagGroups() {
        tagGroupsContainer.removeAllViews()
        val groups = settingsManager.getTagGroups()

        for (group in groups) {
            val itemView = layoutInflater.inflate(R.layout.item_tag_group, tagGroupsContainer, false)

            val groupSwitch = itemView.findViewById<Switch>(R.id.groupSwitch)
            val groupName = itemView.findViewById<TextView>(R.id.groupName)
            val groupTags = itemView.findViewById<TextView>(R.id.groupTags)
            val editButton = itemView.findViewById<ImageButton>(R.id.editButton)
            val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteButton)

            groupName.text = group.name
            groupTags.text = group.tags.joinToString(", ")
            groupSwitch.isChecked = group.isEnabled

            groupSwitch.setOnCheckedChangeListener { _, _ ->
                settingsManager.toggleTagGroup(group.id)
                updateSelectedTagsDisplay()
            }

            editButton.setOnClickListener {
                showEditGroupDialog(group)
            }

            deleteButton.setOnClickListener {
                showDeleteGroupConfirmation(group)
            }

            tagGroupsContainer.addView(itemView)
        }
    }

    private fun showCreateGroupDialog() {
        if (availableTags.isEmpty()) {
            Toast.makeText(this, "No tags available. Sync your quotes first.", Toast.LENGTH_SHORT).show()
            return
        }

        val nameInput = EditText(this).apply {
            hint = "Group name"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Create Tag Group")
            .setView(nameInput)
            .setPositiveButton("Next") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    showSelectTagsForGroupDialog(name, emptySet())
                } else {
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSelectTagsForGroupDialog(groupName: String, existingTags: Set<String>, groupId: String? = null) {
        val selectedTags = existingTags.toMutableSet()
        val checkedItems = availableTags.map { selectedTags.contains(it) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Select tags for \"$groupName\"")
            .setMultiChoiceItems(availableTags.toTypedArray(), checkedItems) { _, which, isChecked ->
                val tag = availableTags[which]
                if (isChecked) {
                    selectedTags.add(tag)
                } else {
                    selectedTags.remove(tag)
                }
            }
            .setPositiveButton("Save") { _, _ ->
                if (selectedTags.isEmpty()) {
                    Toast.makeText(this, "Please select at least one tag", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (groupId != null) {
                    // Editing existing group
                    val existingGroup = settingsManager.getTagGroups().find { it.id == groupId }
                    if (existingGroup != null) {
                        settingsManager.updateTagGroup(existingGroup.copy(name = groupName, tags = selectedTags))
                    }
                } else {
                    // Creating new group
                    val newGroup = com.readwisequotes.data.model.TagGroup(
                        name = groupName,
                        tags = selectedTags
                    )
                    settingsManager.addTagGroup(newGroup)
                }

                renderTagGroups()
                updateSelectedTagsDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditGroupDialog(group: com.readwisequotes.data.model.TagGroup) {
        val nameInput = EditText(this).apply {
            setText(group.name)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Group")
            .setView(nameInput)
            .setPositiveButton("Next") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    showSelectTagsForGroupDialog(name, group.tags, group.id)
                } else {
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
