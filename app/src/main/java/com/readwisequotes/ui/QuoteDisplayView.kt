// app/src/main/java/com/readwisequotes/ui/QuoteDisplayView.kt
package com.readwisequotes.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.readwisequotes.R
import com.readwisequotes.data.model.Quote
import com.readwisequotes.settings.VisualStyle

class QuoteDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val gradientBackground: GradientBackgroundView
    private val quoteContainer: LinearLayout
    private val quoteText: TextView
    private val authorText: TextView
    private val sourceText: TextView
    private val noteText: TextView
    private val tagsText: TextView

    private var currentQuotes: List<Quote> = emptyList()
    private var currentIndex = 0
    private var quoteDurationMs = 20000L
    private var isRunning = false
    private var visualStyle: VisualStyle = VisualStyle.AMBIENT
    private var textSizeScale: Float = 1.0f
    private var showTags: Boolean = true
    private var showNotes: Boolean = true

    private val autoFadeDuration = 600L
    private val manualFadeDuration = 350L

    private val displayRunnable = Runnable { showNextQuoteAuto() }

    // Base text sizes (will be adjusted based on quote length)
    private var baseQuoteSize = 32f
    private var baseAuthorSize = 20f
    private var baseSourceSize = 16f
    private var baseNoteSize = 14f

    init {
        // Create gradient background
        gradientBackground = GradientBackgroundView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(gradientBackground)

        // Create quote container
        quoteContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(quoteContainer)

        // Quote text
        quoteText = TextView(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        quoteContainer.addView(quoteText)

        // Author text
        authorText = TextView(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        quoteContainer.addView(authorText)

        // Source text
        sourceText = TextView(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        quoteContainer.addView(sourceText)

        // Note text (user's personal annotation)
        noteText = TextView(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16)
            }
        }
        quoteContainer.addView(noteText)

        // Tags text - positioned at bottom center (outside quoteContainer)
        tagsText = TextView(context).apply {
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(40)
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            alpha = 0.4f
        }
        addView(tagsText)

        // Apply default style
        applyTheme(VisualStyle.AMBIENT)
    }

    fun setVisualStyle(style: VisualStyle) {
        visualStyle = style
        applyTheme(style)
    }

    fun setTextSizeScale(scale: Float) {
        textSizeScale = scale
    }

    fun setShowTags(show: Boolean) {
        showTags = show
    }

    fun setShowNotes(show: Boolean) {
        showNotes = show
    }

    private fun applyTheme(style: VisualStyle) {
        when (style) {
            VisualStyle.MINIMAL -> applyMinimalTheme()
            VisualStyle.AMBIENT -> applyAmbientTheme()
            VisualStyle.EDITORIAL -> applyEditorialTheme()
            VisualStyle.STOIC -> applyStoicTheme()
        }
    }

    private fun applyMinimalTheme() {
        // Background
        gradientBackground.visibility = View.GONE
        gradientBackground.stopAnimation()
        setBackgroundColor(Color.BLACK)

        // Padding - wider horizontal margins for elegant text blocks
        val horizontalPadding = dpToPx(60)
        val verticalPadding = dpToPx(60)
        quoteContainer.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Quote text - clean white on black
        quoteText.apply {
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setLineSpacing(dpToPx(3).toFloat(), 1f)
            letterSpacing = 0f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(24)
        }
        baseQuoteSize = 22f

        // Author text
        authorText.apply {
            setTextColor(Color.parseColor("#B0B0B0"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0f
            isAllCaps = false
            alpha = 1f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(6)
        }
        baseAuthorSize = 16f

        // Source text
        sourceText.apply {
            setTextColor(Color.parseColor("#808080"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0f
            isAllCaps = false
            alpha = 0.8f
        }
        baseSourceSize = 12f

        // Note text
        noteText.apply {
            setTextColor(Color.parseColor("#909090"))
            typeface = Typeface.create("sans-serif-light", Typeface.ITALIC)
            alpha = 0.7f
        }
        baseNoteSize = 12f

        // Tags text
        tagsText.apply {
            setTextColor(Color.parseColor("#606060"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun applyAmbientTheme() {
        // Background - animated gradient
        gradientBackground.visibility = View.VISIBLE
        if (isRunning) gradientBackground.startAnimation()

        // Padding - wider horizontal margins for elegant text blocks
        val horizontalPadding = dpToPx(60)
        val verticalPadding = dpToPx(60)
        quoteContainer.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Quote text - elegant serif italic
        quoteText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            setLineSpacing(dpToPx(4).toFloat(), 1f)
            letterSpacing = 0f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(24)
        }
        baseQuoteSize = 24f

        // Author text
        authorText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0f
            isAllCaps = false
            alpha = 1f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(6)
        }
        baseAuthorSize = 17f

        // Source text
        sourceText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0f
            isAllCaps = false
            alpha = 0.7f
        }
        baseSourceSize = 13f

        // Note text
        noteText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            typeface = Typeface.create("sans-serif-light", Typeface.ITALIC)
            alpha = 0.6f
        }
        baseNoteSize = 12f

        // Tags text
        tagsText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun applyEditorialTheme() {
        // Background - warm dark paper tone
        gradientBackground.visibility = View.GONE
        gradientBackground.stopAnimation()
        setBackgroundColor(ContextCompat.getColor(context, R.color.editorial_background))

        // Wider horizontal margins for elegant text blocks
        val horizontalPadding = dpToPx(80)
        val verticalPadding = dpToPx(60)
        quoteContainer.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Quote text - elegant serif, cream colored
        quoteText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.editorial_text_primary))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            setLineSpacing(dpToPx(5).toFloat(), 1.05f)
            letterSpacing = 0.01f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(28)
        }
        baseQuoteSize = 24f

        // Author text - refined, small caps feel
        authorText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.editorial_accent))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            isAllCaps = true
            alpha = 1f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(8)
        }
        baseAuthorSize = 14f

        // Source text - subtle, understated
        sourceText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.editorial_text_secondary))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            letterSpacing = 0.02f
            isAllCaps = false
            alpha = 0.8f
        }
        baseSourceSize = 12f

        // Note text
        noteText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.editorial_text_secondary))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            alpha = 0.6f
        }
        baseNoteSize = 11f

        // Tags text
        tagsText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.editorial_text_secondary))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun applyStoicTheme() {
        // Background - deep charcoal
        gradientBackground.visibility = View.GONE
        gradientBackground.stopAnimation()
        setBackgroundColor(ContextCompat.getColor(context, R.color.stoic_background))

        // Wider horizontal margins for elegant text blocks
        val horizontalPadding = dpToPx(80)
        val verticalPadding = dpToPx(60)
        quoteContainer.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Quote text - warm off-white, classical serif
        quoteText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.stoic_text_primary))
            typeface = Typeface.create("serif", Typeface.NORMAL)
            setLineSpacing(dpToPx(5).toFloat(), 1.05f)
            letterSpacing = 0.01f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(28)
        }
        baseQuoteSize = 24f

        // Author text - warm gold accent
        authorText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.stoic_accent))
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.05f
            isAllCaps = false
            alpha = 1f
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dpToPx(6)
        }
        baseAuthorSize = 16f

        // Source text - muted, philosophical
        sourceText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.stoic_text_secondary))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            letterSpacing = 0.01f
            isAllCaps = false
            alpha = 0.7f
        }
        baseSourceSize = 12f

        // Note text
        noteText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.stoic_text_secondary))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            alpha = 0.6f
        }
        baseNoteSize = 11f

        // Tags text
        tagsText.apply {
            setTextColor(ContextCompat.getColor(context, R.color.stoic_text_secondary))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    fun setQuoteDuration(durationSeconds: Int) {
        quoteDurationMs = durationSeconds * 1000L
    }

    fun setQuotes(quotes: List<Quote>) {
        currentQuotes = quotes.shuffled()
        currentIndex = 0
        if (isRunning && quotes.isNotEmpty()) {
            displayCurrentQuote()
        }
    }

    fun start() {
        isRunning = true
        if (visualStyle == VisualStyle.AMBIENT) {
            gradientBackground.startAnimation()
        }
        if (currentQuotes.isNotEmpty()) {
            displayCurrentQuote()
        }
    }

    fun stop() {
        isRunning = false
        gradientBackground.stopAnimation()
        handler?.removeCallbacks(displayRunnable)
    }

    private fun displayCurrentQuote(fadeDuration: Long = autoFadeDuration) {
        if (currentQuotes.isEmpty()) return

        val quote = currentQuotes[currentIndex]

        // Fade out current content first
        fadeOut(fadeDuration) {
            // Adjust text size based on quote length (while invisible)
            val lengthMultiplier = when {
                quote.text.length > 500 -> 0.625f
                quote.text.length > 300 -> 0.75f
                quote.text.length > 150 -> 0.875f
                else -> 1f
            }
            // Apply both length-based and user preference scaling
            quoteText.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseQuoteSize * lengthMultiplier * textSizeScale)
            authorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseAuthorSize * textSizeScale)
            sourceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSourceSize * textSizeScale)
            noteText.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseNoteSize * textSizeScale)

            // Update content - clean markdown and add curly quotes
            val cleanText = cleanQuoteText(quote.text)
            quoteText.text = "\u201C${cleanText}\u201D"
            authorText.text = quote.author?.let { "\u2014 $it" } ?: ""
            sourceText.text = quote.title ?: ""

            authorText.visibility = if (quote.author.isNullOrEmpty()) View.GONE else View.VISIBLE
            sourceText.visibility = if (quote.title.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Show note (if enabled and exists)
            if (showNotes && !quote.note.isNullOrEmpty()) {
                noteText.text = quote.note
                noteText.visibility = View.VISIBLE
            } else {
                noteText.visibility = View.GONE
            }

            // Show tags at bottom center (if enabled)
            if (showTags && quote.tags.isNotEmpty()) {
                tagsText.text = quote.tags.joinToString(", ")
                tagsText.visibility = View.VISIBLE
            } else {
                tagsText.visibility = View.GONE
            }

            // Fade in new content
            fadeIn(fadeDuration) {
                // Schedule next quote
                handler?.postDelayed(displayRunnable, quoteDurationMs)
            }
        }
    }

    private fun showNextQuoteAuto() {
        if (!isRunning) return
        currentIndex = (currentIndex + 1) % currentQuotes.size
        displayCurrentQuote(autoFadeDuration)
    }

    /** Navigate to next quote manually (faster animation, resets timer) */
    fun showNextQuote() {
        if (currentQuotes.isEmpty()) return
        handler?.removeCallbacks(displayRunnable)
        currentIndex = (currentIndex + 1) % currentQuotes.size
        displayCurrentQuote(manualFadeDuration)
    }

    /** Navigate to previous quote manually (faster animation, resets timer) */
    fun showPreviousQuote() {
        if (currentQuotes.isEmpty()) return
        handler?.removeCallbacks(displayRunnable)
        currentIndex = if (currentIndex > 0) currentIndex - 1 else currentQuotes.size - 1
        displayCurrentQuote(manualFadeDuration)
    }

    private fun fadeOut(fadeDuration: Long, onComplete: () -> Unit) {
        ObjectAnimator.ofFloat(quoteContainer, "alpha", 1f, 0f).apply {
            duration = fadeDuration
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    private fun fadeIn(fadeDuration: Long, onComplete: () -> Unit) {
        ObjectAnimator.ofFloat(quoteContainer, "alpha", 0f, 1f).apply {
            duration = fadeDuration
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    private fun cleanQuoteText(text: String): String {
        return text
            // Remove markdown bold/italic: **text**, *text*, __text__, _text_
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("_(.+?)_"), "$1")
            // Remove existing quotes at start/end (we add our own curly quotes)
            .trim()
            .removeSurrounding("\"", "\"")
            .removeSurrounding("\u201C", "\u201D")  // curly quotes
            .removeSurrounding("\u201C", "\"")      // mixed quotes
            .removeSurrounding("\"", "\u201D")
            .trim()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
