// app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt
package com.readwisequotes.ui

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.readwisequotes.R
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.data.SyncResult
import com.readwisequotes.settings.QrLinkType
import com.readwisequotes.settings.QuoteFilter
import com.readwisequotes.settings.SettingsManager
import com.readwisequotes.settings.TagFilterMode
import com.readwisequotes.settings.TextSize
import com.readwisequotes.settings.VisualStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : FragmentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    // Rail items
    private lateinit var backButton: Button
    private lateinit var railAccount: TextView
    private lateinit var railFilters: TextView
    private lateinit var railDisplay: TextView
    private lateinit var railSync: TextView
    private lateinit var contentArea: FrameLayout

    // Current content views (dynamically inflated)
    private var currentContentView: View? = null
    private var currentCategory: Category = Category.ACCOUNT

    // Shared state
    private var availableTags: List<String> = emptyList()
    private var isTokenVisible = false
    private var tokenEntryServer: TokenEntryServer? = null
    private var setupDialog: AlertDialog? = null

    enum class Category {
        ACCOUNT, FILTERS, DISPLAY, SYNC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_v2)

        bindRailViews()
        setupRailNavigation()
        loadAvailableTags()

        // Show Account content by default
        selectCategory(Category.ACCOUNT)

        // Initial focus on Account rail item
        railAccount.post {
            railAccount.requestFocus()
        }
    }

    private fun bindRailViews() {
        backButton = findViewById(R.id.backButton)
        railAccount = findViewById(R.id.railAccount)
        railFilters = findViewById(R.id.railFilters)
        railDisplay = findViewById(R.id.railDisplay)
        railSync = findViewById(R.id.railSync)
        contentArea = findViewById(R.id.contentArea)
    }

    private fun setupRailNavigation() {
        backButton.setOnClickListener { finish() }

        // Rail item click listeners
        railAccount.setOnClickListener { selectCategory(Category.ACCOUNT) }
        railFilters.setOnClickListener { selectCategory(Category.FILTERS) }
        railDisplay.setOnClickListener { selectCategory(Category.DISPLAY) }
        railSync.setOnClickListener { selectCategory(Category.SYNC) }

        // Set up focus change listeners to update selected state
        val railItems = listOf(railAccount, railFilters, railDisplay, railSync)
        railItems.forEach { item ->
            item.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    val category = when (view) {
                        railAccount -> Category.ACCOUNT
                        railFilters -> Category.FILTERS
                        railDisplay -> Category.DISPLAY
                        railSync -> Category.SYNC
                        else -> return@setOnFocusChangeListener
                    }
                    selectCategory(category)
                }
                updateRailSelection()
            }
        }
    }

    private fun updateRailSelection() {
        val railItems = listOf(
            railAccount to Category.ACCOUNT,
            railFilters to Category.FILTERS,
            railDisplay to Category.DISPLAY,
            railSync to Category.SYNC
        )

        railItems.forEach { (item, category) ->
            item.isSelected = (category == currentCategory && !item.isFocused)
        }
    }

    private fun selectCategory(category: Category) {
        if (category == currentCategory && currentContentView != null) {
            return
        }

        currentCategory = category
        updateRailSelection()

        // Inflate and show the appropriate content
        contentArea.removeAllViews()

        val layoutRes = when (category) {
            Category.ACCOUNT -> R.layout.settings_content_account
            Category.FILTERS -> R.layout.settings_content_filters
            Category.DISPLAY -> R.layout.settings_content_display
            Category.SYNC -> R.layout.settings_content_sync
        }

        currentContentView = LayoutInflater.from(this).inflate(layoutRes, contentArea, false)
        contentArea.addView(currentContentView)

        // Setup content based on category
        when (category) {
            Category.ACCOUNT -> setupAccountContent()
            Category.FILTERS -> setupFiltersContent()
            Category.DISPLAY -> setupDisplayContent()
            Category.SYNC -> setupSyncContent()
        }

        // Set up focus navigation from rail to content
        setupRailToContentFocus()
    }

    private fun setupRailToContentFocus() {
        val firstFocusable = findFirstFocusableInContent()

        // Rail items should move focus right into content
        railAccount.nextFocusRightId = firstFocusable?.id ?: View.NO_ID
        railFilters.nextFocusRightId = firstFocusable?.id ?: View.NO_ID
        railDisplay.nextFocusRightId = firstFocusable?.id ?: View.NO_ID
        railSync.nextFocusRightId = firstFocusable?.id ?: View.NO_ID
    }

    private fun findFirstFocusableInContent(): View? {
        return currentContentView?.let { findFirstFocusable(it) }
    }

    private fun findFirstFocusable(view: View): View? {
        if (view.isFocusable && view.visibility == View.VISIBLE) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val focusable = findFirstFocusable(view.getChildAt(i))
                if (focusable != null) return focusable
            }
        }
        return null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            // If focus is in content area, move back to rail
            val focusedView = currentFocus
            if (focusedView != null && isDescendantOfView(focusedView, contentArea)) {
                val railItem = when (currentCategory) {
                    Category.ACCOUNT -> railAccount
                    Category.FILTERS -> railFilters
                    Category.DISPLAY -> railDisplay
                    Category.SYNC -> railSync
                }
                railItem.requestFocus()
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

    // ==================== Account Content ====================

    private fun setupAccountContent() {
        val view = currentContentView ?: return

        val apiTokenInput = view.findViewById<EditText>(R.id.apiTokenInput)
        val toggleTokenVisibility = view.findViewById<Button>(R.id.toggleTokenVisibility)
        val tokenHelperText = view.findViewById<TextView>(R.id.tokenHelperText)
        val setupViaPhoneButton = view.findViewById<Button>(R.id.setupViaPhoneButton)
        val syncButton = view.findViewById<Button>(R.id.syncButton)
        val fullSyncButton = view.findViewById<Button>(R.id.fullSyncButton)
        val syncStatus = view.findViewById<TextView>(R.id.syncStatus)

        // Load current token
        val token = settingsManager.getApiToken()
        if (token.isNotEmpty()) {
            apiTokenInput.setText(token)
            tokenHelperText.visibility = View.GONE
            setupViaPhoneButton.visibility = View.GONE
        } else {
            tokenHelperText.visibility = View.VISIBLE
            setupViaPhoneButton.visibility = View.VISIBLE
        }

        // Toggle visibility
        toggleTokenVisibility.setOnClickListener {
            isTokenVisible = !isTokenVisible
            if (isTokenVisible) {
                apiTokenInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleTokenVisibility.text = "🙈"
            } else {
                apiTokenInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleTokenVisibility.text = "👁"
            }
            apiTokenInput.setSelection(apiTokenInput.text.length)
        }

        // Token input action
        apiTokenInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val inputToken = apiTokenInput.text.toString()
                if (inputToken.isNotEmpty()) {
                    verifyAndSaveToken(inputToken, syncStatus, syncButton)
                }
                true
            } else false
        }

        // Setup via phone
        setupViaPhoneButton.setOnClickListener {
            startTokenEntryServer(apiTokenInput, tokenHelperText, setupViaPhoneButton, syncStatus, syncButton)
        }

        // Sync buttons
        syncButton.setOnClickListener {
            val inputToken = apiTokenInput.text.toString()
            val savedToken = settingsManager.getApiToken()
            if (inputToken.isNotEmpty() && inputToken != savedToken) {
                verifyAndSaveToken(inputToken, syncStatus, syncButton)
            } else {
                performSync(syncStatus, syncButton)
            }
        }

        fullSyncButton.setOnClickListener {
            settingsManager.clearLastSyncTime()
            Toast.makeText(this, "Re-downloading all quotes...", Toast.LENGTH_SHORT).show()
            performSync(syncStatus, syncButton)
        }

        // Update sync status
        updateSyncStatus(syncStatus)
    }

    private fun verifyAndSaveToken(token: String, syncStatus: TextView, syncButton: Button) {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncStatus.text = "Verifying token..."

            val isValid = quoteRepository.verifyToken(token)
            if (isValid) {
                settingsManager.setApiToken(token)
                syncStatus.text = "Token verified!"
                performSync(syncStatus, syncButton)
            } else {
                syncStatus.text = "Invalid token"
                Toast.makeText(this@SettingsActivity, "Invalid API token", Toast.LENGTH_SHORT).show()
            }
            syncButton.isEnabled = true
        }
    }

    private fun performSync(syncStatus: TextView, syncButton: Button) {
        lifecycleScope.launch {
            syncButton.isEnabled = false
            syncStatus.text = getString(R.string.syncing)

            when (val result = quoteRepository.sync()) {
                is SyncResult.Success -> {
                    Toast.makeText(this@SettingsActivity, "Synced ${result.count} quotes", Toast.LENGTH_SHORT).show()
                    loadAvailableTags()
                }
                is SyncResult.Error -> {
                    Toast.makeText(this@SettingsActivity, "Sync failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }

            updateSyncStatus(syncStatus)
            syncButton.isEnabled = true
        }
    }

    private fun updateSyncStatus(syncStatus: TextView) {
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

    // ==================== Filters Content ====================

    private fun setupFiltersContent() {
        val view = currentContentView ?: return

        val filterSpinner = view.findViewById<Spinner>(R.id.filterSpinner)
        val tagSelectionContainer = view.findViewById<LinearLayout>(R.id.tagSelectionContainer)
        val tagGroupsContainer = view.findViewById<LinearLayout>(R.id.tagGroupsContainer)
        val createGroupButton = view.findViewById<Button>(R.id.createGroupButton)
        val selectedTagsText = view.findViewById<TextView>(R.id.selectedTagsText)

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
        filterSpinner.setSelection(settingsManager.getQuoteFilter().ordinal)

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val filter = QuoteFilter.entries[position]
                settingsManager.setQuoteFilter(filter)
                tagSelectionContainer.visibility = if (filter == QuoteFilter.BY_TAG) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Initial visibility
        val currentFilter = settingsManager.getQuoteFilter()
        tagSelectionContainer.visibility = if (currentFilter == QuoteFilter.BY_TAG) View.VISIBLE else View.GONE

        // Create group button
        createGroupButton.setOnClickListener {
            showCreateGroupDialog(tagGroupsContainer, selectedTagsText)
        }

        // Render existing groups
        renderTagGroups(tagGroupsContainer, selectedTagsText, filterSpinner, createGroupButton)
        updateSelectedTagsDisplay(selectedTagsText)
    }

    private fun renderTagGroups(
        container: LinearLayout,
        selectedTagsText: TextView,
        filterSpinner: Spinner,
        createGroupButton: Button
    ) {
        container.removeAllViews()
        val groups = settingsManager.getTagGroups()

        var firstGroupCheckbox: CheckBox? = null
        var lastGroupDeleteButton: Button? = null
        var previousDeleteButton: Button? = null

        groups.forEachIndexed { index, group ->
            val itemView = layoutInflater.inflate(R.layout.item_tag_group, container, false)

            val groupSwitch = itemView.findViewById<CheckBox>(R.id.groupSwitch)
            val groupName = itemView.findViewById<TextView>(R.id.groupName)
            val groupTags = itemView.findViewById<TextView>(R.id.groupTags)
            val matchModeToggle = itemView.findViewById<ToggleButton>(R.id.matchModeToggle)
            val editButton = itemView.findViewById<Button>(R.id.editButton)
            val deleteButton = itemView.findViewById<Button>(R.id.deleteButton)

            groupSwitch.id = View.generateViewId()
            matchModeToggle.id = View.generateViewId()
            editButton.id = View.generateViewId()
            deleteButton.id = View.generateViewId()

            groupName.text = group.name
            groupTags.text = group.tags.joinToString(", ")
            groupSwitch.isChecked = group.isEnabled
            matchModeToggle.isChecked = group.matchMode == TagFilterMode.ALL

            // Horizontal focus
            groupSwitch.nextFocusRightId = matchModeToggle.id
            matchModeToggle.nextFocusLeftId = groupSwitch.id
            matchModeToggle.nextFocusRightId = editButton.id
            editButton.nextFocusLeftId = matchModeToggle.id
            editButton.nextFocusRightId = deleteButton.id
            deleteButton.nextFocusLeftId = editButton.id

            if (previousDeleteButton != null) {
                previousDeleteButton!!.nextFocusDownId = groupSwitch.id
                groupSwitch.nextFocusUpId = firstGroupCheckbox?.id ?: filterSpinner.id
            }

            groupSwitch.setOnCheckedChangeListener { _, _ ->
                settingsManager.toggleTagGroup(group.id)
                updateSelectedTagsDisplay(selectedTagsText)
            }

            matchModeToggle.setOnCheckedChangeListener { _, isChecked ->
                val newMode = if (isChecked) TagFilterMode.ALL else TagFilterMode.ANY
                settingsManager.updateTagGroupMatchMode(group.id, newMode)
            }

            editButton.setOnClickListener {
                showEditGroupDialog(group, container, selectedTagsText, filterSpinner, createGroupButton)
            }

            deleteButton.setOnClickListener {
                showDeleteGroupConfirmation(group, container, selectedTagsText, filterSpinner, createGroupButton)
            }

            container.addView(itemView)

            if (index == 0) firstGroupCheckbox = groupSwitch
            lastGroupDeleteButton = deleteButton
            previousDeleteButton = deleteButton
        }

        // Focus chain connections
        firstGroupCheckbox?.let { first ->
            filterSpinner.nextFocusDownId = first.id
            first.nextFocusUpId = filterSpinner.id
        }
        lastGroupDeleteButton?.let { last ->
            last.nextFocusDownId = createGroupButton.id
            createGroupButton.nextFocusUpId = last.id
        }
        if (groups.isEmpty()) {
            filterSpinner.nextFocusDownId = createGroupButton.id
            createGroupButton.nextFocusUpId = filterSpinner.id
        }
    }

    private fun updateSelectedTagsDisplay(selectedTagsText: TextView) {
        val enabledGroups = settingsManager.getEnabledTagGroups()
        val allTags = settingsManager.getAllEnabledTags()

        selectedTagsText.text = when {
            enabledGroups.isEmpty() -> "No groups enabled"
            allTags.size <= 5 -> "${enabledGroups.size} group(s): ${allTags.joinToString(", ")}"
            else -> "${enabledGroups.size} group(s): ${allTags.take(5).joinToString(", ")} +${allTags.size - 5} more"
        }
    }

    // ==================== Display Content ====================

    private fun setupDisplayContent() {
        val view = currentContentView ?: return

        val styleSpinner = view.findViewById<Spinner>(R.id.styleSpinner)
        val textSizeSpinner = view.findViewById<Spinner>(R.id.textSizeSpinner)
        val showTagsSwitch = view.findViewById<Switch>(R.id.showTagsSwitch)
        val showNotesSwitch = view.findViewById<Switch>(R.id.showNotesSwitch)
        val showQrCodeSwitch = view.findViewById<Switch>(R.id.showQrCodeSwitch)
        val leftAlignTextSwitch = view.findViewById<Switch>(R.id.leftAlignTextSwitch)
        val qrLinkTypeContainer = view.findViewById<LinearLayout>(R.id.qrLinkTypeContainer)
        val qrLinkTypeSpinner = view.findViewById<Spinner>(R.id.qrLinkTypeSpinner)
        val durationSeekBar = view.findViewById<SeekBar>(R.id.durationSeekBar)
        val durationValue = view.findViewById<TextView>(R.id.durationValue)

        // Style spinner
        val styleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.style_pretty),
                getString(R.string.style_minimal),
                getString(R.string.style_ambient),
                getString(R.string.style_editorial),
                getString(R.string.style_stoic)
            )
        )
        styleSpinner.adapter = styleAdapter
        styleSpinner.setSelection(settingsManager.getVisualStyle().ordinal)
        styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                settingsManager.setVisualStyle(VisualStyle.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Text size spinner
        val textSizeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.text_size_small),
                getString(R.string.text_size_medium),
                getString(R.string.text_size_large)
            )
        )
        textSizeSpinner.adapter = textSizeAdapter
        textSizeSpinner.setSelection(settingsManager.getTextSize().ordinal)
        textSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                settingsManager.setTextSize(TextSize.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Switches
        showTagsSwitch.isChecked = settingsManager.getShowTags()
        showTagsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowTags(isChecked)
        }

        showNotesSwitch.isChecked = settingsManager.getShowNotes()
        showNotesSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowNotes(isChecked)
        }

        showQrCodeSwitch.isChecked = settingsManager.getShowQrCode()
        showQrCodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setShowQrCode(isChecked)
            qrLinkTypeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        leftAlignTextSwitch.isChecked = settingsManager.getLeftAlignText()
        leftAlignTextSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setLeftAlignText(isChecked)
        }

        // QR link type
        qrLinkTypeContainer.visibility = if (settingsManager.getShowQrCode()) View.VISIBLE else View.GONE
        val qrLinkTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.qr_link_readwise), getString(R.string.qr_link_source))
        )
        qrLinkTypeSpinner.adapter = qrLinkTypeAdapter
        qrLinkTypeSpinner.setSelection(settingsManager.getQrLinkType().ordinal)
        qrLinkTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                settingsManager.setQrLinkType(QrLinkType.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Duration
        val duration = settingsManager.getQuoteDuration()
        durationSeekBar.progress = duration
        durationValue.text = getString(R.string.duration_format, duration)
        durationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                durationValue.text = getString(R.string.duration_format, progress)
                if (fromUser) settingsManager.setQuoteDuration(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ==================== Sync Content ====================

    private fun setupSyncContent() {
        val view = currentContentView ?: return

        val syncIntervalSpinner = view.findViewById<Spinner>(R.id.syncIntervalSpinner)
        val lastSyncedText = view.findViewById<TextView>(R.id.lastSyncedText)
        val quoteCountText = view.findViewById<TextView>(R.id.quoteCountText)

        // Sync interval spinner
        val intervalAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("1 hour", "6 hours", "24 hours", "Manual only")
        )
        syncIntervalSpinner.adapter = intervalAdapter

        val intervalPosition = when (settingsManager.getSyncIntervalHours()) {
            1 -> 0
            6 -> 1
            24 -> 2
            else -> 3
        }
        syncIntervalSpinner.setSelection(intervalPosition)

        syncIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
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

        // Status info
        lifecycleScope.launch {
            val lastSync = settingsManager.getLastSyncTime()
            val count = quoteRepository.getQuoteCount()

            val timeText = if (lastSync != null) {
                try {
                    val instant = Instant.parse(lastSync)
                    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                        .withZone(ZoneId.systemDefault())
                    "Last synced: ${formatter.format(instant)}"
                } catch (e: Exception) {
                    "Last synced: Never"
                }
            } else {
                "Last synced: Never"
            }

            lastSyncedText.text = timeText
            quoteCountText.text = "$count quotes"
        }
    }

    // ==================== Tag Group Dialogs ====================

    private fun loadAvailableTags() {
        lifecycleScope.launch {
            availableTags = quoteRepository.getAllTags()
        }
    }

    private fun showCreateGroupDialog(container: LinearLayout, selectedTagsText: TextView) {
        if (availableTags.isEmpty()) {
            Toast.makeText(this, "No tags available. Sync your quotes first.", Toast.LENGTH_SHORT).show()
            return
        }

        val existingGroups = settingsManager.getTagGroups()
        val defaultName = "Group ${existingGroups.size + 1}"

        showGroupNameDialog("Create Tag Group", defaultName) { name ->
            showSelectTagsForGroupDialog(name, emptySet(), null, container, selectedTagsText)
        }
    }

    private fun showEditGroupDialog(
        group: com.readwisequotes.data.model.TagGroup,
        container: LinearLayout,
        selectedTagsText: TextView,
        filterSpinner: Spinner,
        createGroupButton: Button
    ) {
        showGroupNameDialog("Edit Group", group.name) { name ->
            if (name.isNotEmpty()) {
                showSelectTagsForGroupDialog(name, group.tags, group.id, container, selectedTagsText)
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteGroupConfirmation(
        group: com.readwisequotes.data.model.TagGroup,
        container: LinearLayout,
        selectedTagsText: TextView,
        filterSpinner: Spinner,
        createGroupButton: Button
    ) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Delete \"${group.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                settingsManager.deleteTagGroup(group.id)
                renderTagGroups(container, selectedTagsText, filterSpinner, createGroupButton)
                updateSelectedTagsDisplay(selectedTagsText)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        nameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                nextButton.performClick()
                true
            } else false
        }

        dialog.show()
        nameInput.requestFocus()
    }

    private fun showSelectTagsForGroupDialog(
        groupName: String,
        existingTags: Set<String>,
        groupId: String?,
        container: LinearLayout,
        selectedTagsText: TextView
    ) {
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

        val checkboxes = mutableListOf<CheckBox>()
        availableTags.forEachIndexed { index, tag ->
            val checkBox = layoutInflater.inflate(R.layout.item_tag_checkbox, tagsContainer, false) as CheckBox
            checkBox.text = tag
            checkBox.isChecked = selectedTags.contains(tag)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTags.add(tag) else selectedTags.remove(tag)
            }

            checkBox.nextFocusLeftId = cancelButton.id
            checkBox.nextFocusRightId = saveButton.id

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

            // Re-render the filters content
            setupFiltersContent()
        }

        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val focused = dialog.currentFocus
                val isCheckboxFocused = checkboxes.contains(focused)

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

        dialog.show()
        if (checkboxes.isNotEmpty()) {
            checkboxes[0].requestFocus()
        }
    }

    // ==================== Token Entry Server ====================

    private fun startTokenEntryServer(
        apiTokenInput: EditText,
        tokenHelperText: TextView,
        setupViaPhoneButton: Button,
        syncStatus: TextView,
        syncButton: Button
    ) {
        val ipAddress = getDeviceIpAddress()
        if (ipAddress == null) {
            Toast.makeText(this, "Could not get IP address. Make sure you're connected to WiFi.", Toast.LENGTH_LONG).show()
            return
        }

        val port = 9876
        tokenEntryServer = TokenEntryServer(port) { token ->
            runOnUiThread {
                handleTokenReceived(token, apiTokenInput, tokenHelperText, setupViaPhoneButton, syncStatus, syncButton)
            }
        }

        try {
            tokenEntryServer?.start()
            showSetupDialog(ipAddress, port)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start server: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSetupDialog(ipAddress: String, port: Int) {
        val url = "http://$ipAddress:$port"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            val pad = dpToPx(24)
            setPadding(pad, pad, pad, pad)
        }

        val qrSize = dpToPx(200)
        val qrBitmap = generateQrCodeBitmap(url, qrSize)
        if (qrBitmap != null) {
            val qrImage = ImageView(this).apply {
                setImageBitmap(qrBitmap)
                layoutParams = LinearLayout.LayoutParams(qrSize, qrSize).apply {
                    bottomMargin = dpToPx(16)
                }
            }
            layout.addView(qrImage)
        }

        val instructions = TextView(this).apply {
            text = "Scan the QR code, or open a browser and go to:\n\n$url\n\nThen paste your Readwise API token."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(instructions)

        setupDialog = AlertDialog.Builder(this)
            .setTitle("Setup via Phone")
            .setView(layout)
            .setNegativeButton("Cancel") { _, _ ->
                stopTokenEntryServer()
            }
            .setOnCancelListener {
                stopTokenEntryServer()
            }
            .create()

        setupDialog?.show()
    }

    private fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun handleTokenReceived(
        token: String,
        apiTokenInput: EditText,
        tokenHelperText: TextView,
        setupViaPhoneButton: Button,
        syncStatus: TextView,
        syncButton: Button
    ) {
        stopTokenEntryServer()
        setupDialog?.dismiss()

        settingsManager.setApiToken(token)
        apiTokenInput.setText(token)

        tokenHelperText.visibility = View.GONE
        setupViaPhoneButton.visibility = View.GONE

        Toast.makeText(this, "Token received! Syncing...", Toast.LENGTH_SHORT).show()
        performSync(syncStatus, syncButton)
    }

    private fun stopTokenEntryServer() {
        tokenEntryServer?.stop()
        tokenEntryServer = null
    }

    private fun getDeviceIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTokenEntryServer()
    }
}
