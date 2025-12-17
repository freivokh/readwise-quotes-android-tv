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

    private var currentQuotes: List<Quote> = emptyList()
    private var currentIndex = 0
    private var quoteDurationMs = 20000L
    private var isRunning = false
    private var visualStyle: VisualStyle = VisualStyle.AMBIENT

    private val displayRunnable = Runnable { showNextQuote() }

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
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                val padding = dpToPx(80)
                setPadding(padding, padding, padding, padding)
            }
        }
        addView(quoteContainer)

        // Quote text
        quoteText = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(32)
            }
        }
        quoteContainer.addView(quoteText)

        // Author text
        authorText = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }
        quoteContainer.addView(authorText)

        // Source text
        sourceText = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            alpha = 0.7f
        }
        quoteContainer.addView(sourceText)
    }

    fun setVisualStyle(style: VisualStyle) {
        visualStyle = style
        when (style) {
            VisualStyle.AMBIENT -> {
                gradientBackground.visibility = View.VISIBLE
                gradientBackground.startAnimation()
            }
            VisualStyle.MINIMAL -> {
                gradientBackground.visibility = View.GONE
                gradientBackground.stopAnimation()
                setBackgroundColor(Color.BLACK)
            }
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

    private fun displayCurrentQuote() {
        if (currentQuotes.isEmpty()) return

        val quote = currentQuotes[currentIndex]

        // Adjust text size based on quote length
        val textSize = when {
            quote.text.length > 500 -> 20f
            quote.text.length > 300 -> 24f
            quote.text.length > 150 -> 28f
            else -> 32f
        }
        quoteText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

        // Fade out current content
        fadeOut {
            // Update content
            quoteText.text = "\"${quote.text}\""
            authorText.text = quote.author?.let { "— $it" } ?: ""
            sourceText.text = quote.title ?: ""

            authorText.visibility = if (quote.author.isNullOrEmpty()) View.GONE else View.VISIBLE
            sourceText.visibility = if (quote.title.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Fade in new content
            fadeIn {
                // Schedule next quote
                handler?.postDelayed(displayRunnable, quoteDurationMs)
            }
        }
    }

    private fun showNextQuote() {
        if (!isRunning) return
        currentIndex = (currentIndex + 1) % currentQuotes.size
        displayCurrentQuote()
    }

    private fun fadeOut(onComplete: () -> Unit) {
        ObjectAnimator.ofFloat(quoteContainer, "alpha", 1f, 0f).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    private fun fadeIn(onComplete: () -> Unit) {
        ObjectAnimator.ofFloat(quoteContainer, "alpha", 0f, 1f).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
            start()
        }
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
