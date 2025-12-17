// app/src/main/java/com/readwisequotes/QuoteDreamService.kt
package com.readwisequotes

import android.service.dreams.DreamService
import com.readwisequotes.data.QuoteRepository
import com.readwisequotes.settings.SettingsManager
import com.readwisequotes.ui.QuoteDisplayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class QuoteDreamService : DreamService() {

    @Inject lateinit var settingsManager: SettingsManager
    @Inject lateinit var quoteRepository: QuoteRepository

    private lateinit var quoteDisplayView: QuoteDisplayView
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Configure dream
        isInteractive = true
        isFullscreen = true
        isScreenBright = false

        // Create and set content view
        quoteDisplayView = QuoteDisplayView(this)
        setContentView(quoteDisplayView)

        // Apply settings
        quoteDisplayView.setVisualStyle(settingsManager.getVisualStyle())
        quoteDisplayView.setQuoteDuration(settingsManager.getQuoteDuration())
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()

        serviceScope.launch {
            // Sync if needed
            if (settingsManager.shouldSync()) {
                quoteRepository.sync()
            }

            // Observe and display quotes
            quoteRepository.getQuotes().collectLatest { quotes ->
                if (quotes.isNotEmpty()) {
                    quoteDisplayView.setQuotes(quotes)
                    quoteDisplayView.start()
                }
            }
        }
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        quoteDisplayView.stop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        serviceScope.cancel()
    }
}
