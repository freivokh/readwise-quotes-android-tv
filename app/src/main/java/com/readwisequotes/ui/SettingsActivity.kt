// app/src/main/java/com/readwisequotes/ui/SettingsActivity.kt
package com.readwisequotes.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.readwisequotes.R
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.data.SyncResult
import com.readwisequotes.settings.QuoteFilter
import com.readwisequotes.settings.SettingsManager
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

    private lateinit var apiTokenInput: EditText
    private lateinit var syncButton: Button
    private lateinit var syncStatus: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var styleSpinner: Spinner
    private lateinit var durationSeekBar: SeekBar
    private lateinit var durationValue: TextView
    private lateinit var syncIntervalSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        setupListeners()
        loadCurrentSettings()
    }

    private fun bindViews() {
        apiTokenInput = findViewById(R.id.apiTokenInput)
        syncButton = findViewById(R.id.syncButton)
        syncStatus = findViewById(R.id.syncStatus)
        filterSpinner = findViewById(R.id.filterSpinner)
        styleSpinner = findViewById(R.id.styleSpinner)
        durationSeekBar = findViewById(R.id.durationSeekBar)
        durationValue = findViewById(R.id.durationValue)
        syncIntervalSpinner = findViewById(R.id.syncIntervalSpinner)
    }

    private fun setupListeners() {
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
            performSync()
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
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        filterSpinner.setSelection(settingsManager.getQuoteFilter().ordinal)

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
}
