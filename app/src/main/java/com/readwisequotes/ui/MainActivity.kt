// app/src/main/java/com/readwisequotes/ui/MainActivity.kt
package com.readwisequotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.readwisequotes.R
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.data.SyncResult
import com.readwisequotes.settings.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    private lateinit var quoteDisplayView: QuoteDisplayView
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var openSettingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quoteDisplayView = findViewById(R.id.quoteDisplayView)
        emptyState = findViewById(R.id.emptyState)
        emptyStateText = findViewById(R.id.emptyStateText)
        openSettingsButton = findViewById(R.id.openSettingsButton)

        openSettingsButton.setOnClickListener {
            openSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        initializeDisplay()
    }

    override fun onPause() {
        super.onPause()
        quoteDisplayView.stop()
    }

    private fun initializeDisplay() {
        if (!settingsManager.isSetupComplete()) {
            showEmptyState(getString(R.string.setup_required))
            return
        }

        // Apply settings
        quoteDisplayView.setVisualStyle(settingsManager.getVisualStyle())
        quoteDisplayView.setQuoteDuration(settingsManager.getQuoteDuration())

        // Check if sync needed
        lifecycleScope.launch {
            if (settingsManager.shouldSync()) {
                quoteRepository.sync()
            }

            // Observe quotes
            quoteRepository.getQuotes().collectLatest { quotes ->
                if (quotes.isEmpty()) {
                    showEmptyState(getString(R.string.no_quotes))
                } else {
                    hideEmptyState()
                    quoteDisplayView.setQuotes(quotes)
                    quoteDisplayView.start()
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        emptyStateText.text = message
        emptyState.visibility = View.VISIBLE
        quoteDisplayView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        quoteDisplayView.visibility = View.VISIBLE
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Open settings on OK/Select button press
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_MENU) {
            openSettings()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
