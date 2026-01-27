// app/src/main/java/com/readwisequotes/ui/QuoteDisplayView.kt
package com.readwisequotes.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.readwisequotes.R
import com.readwisequotes.data.model.Quote
import com.readwisequotes.settings.QrLinkType
import com.readwisequotes.settings.VisualStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val qrCodeImage: ImageView

    // Library theme specific views
    private val libraryContainer: LinearLayout
    private val libraryLeftPanel: FrameLayout
    private val libraryAccentBar: View
    private val libraryQuoteContainer: LinearLayout
    private val libraryGradientOverlay: View
    private val libraryBookTitle: TextView
    private val libraryBookAuthor: TextView
    private val coverImageView: ImageView

    // Cache for extracted colors per book
    private val colorCache = mutableMapOf<String, Palette.Swatch?>()

    private var currentQuotes: List<Quote> = emptyList()
    private var currentIndex = 0
    private var quoteDurationMs = 20000L
    private var isRunning = false
    private var visualStyle: VisualStyle = VisualStyle.AMBIENT
    private var textSizeScale: Float = 1.0f
    private var showTags: Boolean = true
    private var showNotes: Boolean = true
    private var showQrCode: Boolean = true
    private var qrLinkType: QrLinkType = QrLinkType.READWISE

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

        // QR code - positioned at bottom right corner
        qrCodeImage = ImageView(context).apply {
            layoutParams = LayoutParams(
                dpToPx(44),
                dpToPx(44)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                marginEnd = dpToPx(40)
                bottomMargin = dpToPx(40)
            }
            alpha = 0.25f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        addView(qrCodeImage)

        // Library theme: horizontal container with left panel and cover
        libraryContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        addView(libraryContainer)

        // Library left panel (colored background, ~62% width)
        libraryLeftPanel = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.62f)
        }
        libraryContainer.addView(libraryLeftPanel)

        // Library quote container with accent bar integrated
        // Using a horizontal layout: [accent bar] [quote content]
        val quoteWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(48)
                marginEnd = dpToPx(32)
            }
        }
        libraryLeftPanel.addView(quoteWrapper)

        // Dark accent bar - will stretch to match quote content height
        libraryAccentBar = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(3),
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                marginEnd = dpToPx(16)
            }
            setBackgroundColor(Color.parseColor("#40000000"))
        }
        quoteWrapper.addView(libraryAccentBar)

        // Library quote container (vertical stack of quote, title, author)
        libraryQuoteContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        quoteWrapper.addView(libraryQuoteContainer)

        // Subtle gradient overlay (now just a spacer, gradient applied to panel background)
        libraryGradientOverlay = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(8), LinearLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE // Hard edge, no gradient overlay
        }
        libraryContainer.addView(libraryGradientOverlay)

        // Library book title (below quote)
        libraryBookTitle = TextView(context).apply {
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(24)
            }
        }

        // Library book author (below title)
        libraryBookAuthor = TextView(context).apply {
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        // Cover image (right side, with subtle 2D angle rotation)
        coverImageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.38f).apply {
                marginEnd = dpToPx(-30)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            // Subtle clockwise rotation (5 degrees like reference)
            rotation = 5f
            // Scale up slightly to fill gaps
            scaleX = 1.08f
            scaleY = 1.12f
        }
        libraryContainer.addView(coverImageView)

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

    fun setShowQrCode(show: Boolean) {
        showQrCode = show
    }

    fun setQrLinkType(type: QrLinkType) {
        qrLinkType = type
    }

    private fun applyTheme(style: VisualStyle) {
        when (style) {
            VisualStyle.MINIMAL -> applyMinimalTheme()
            VisualStyle.AMBIENT -> applyAmbientTheme()
            VisualStyle.EDITORIAL -> applyEditorialTheme()
            VisualStyle.STOIC -> applyStoicTheme()
            VisualStyle.LIBRARY -> applyLibraryTheme()
        }
    }

    private fun applyMinimalTheme() {
        // Hide library layout if switching from Library theme
        hideLibraryTheme()

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
        // Hide library layout if switching from Library theme
        hideLibraryTheme()

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
        // Hide library layout if switching from Library theme
        hideLibraryTheme()

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
        // Hide library layout if switching from Library theme
        hideLibraryTheme()

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

    private fun applyLibraryTheme() {
        // Hide standard backgrounds, show library layout
        gradientBackground.visibility = View.GONE
        gradientBackground.stopAnimation()
        quoteContainer.visibility = View.GONE
        libraryContainer.visibility = View.VISIBLE
        setBackgroundColor(Color.parseColor("#1A1A1A"))

        // Default colors (will be updated per-quote based on cover)
        libraryLeftPanel.setBackgroundColor(Color.parseColor("#1A1A1A"))

        // Base text sizes for Library theme
        baseQuoteSize = 24f
        baseAuthorSize = 16f
        baseSourceSize = 14f
        baseNoteSize = 12f

        // Move QR code to bottom-right of left panel area
        (qrCodeImage.layoutParams as LayoutParams).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            marginStart = dpToPx(60)
            marginEnd = 0
            bottomMargin = dpToPx(40)
        }
        qrCodeImage.requestLayout()

        // Move tags to bottom of left panel
        (tagsText.layoutParams as LayoutParams).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            marginStart = dpToPx(60)
            bottomMargin = dpToPx(100)
        }
        tagsText.requestLayout()
    }

    private fun hideLibraryTheme() {
        libraryContainer.visibility = View.GONE
        quoteContainer.visibility = View.VISIBLE

        // Reset QR code position
        (qrCodeImage.layoutParams as LayoutParams).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            marginStart = 0
            marginEnd = dpToPx(40)
            bottomMargin = dpToPx(40)
        }
        qrCodeImage.requestLayout()

        // Reset tags position
        (tagsText.layoutParams as LayoutParams).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            marginStart = 0
            bottomMargin = dpToPx(40)
        }
        tagsText.requestLayout()
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
            if (visualStyle == VisualStyle.LIBRARY) {
                displayLibraryQuote(quote, fadeDuration)
            } else {
                displayStandardQuote(quote, fadeDuration)
            }
        }
    }

    private fun displayStandardQuote(quote: Quote, fadeDuration: Long) {
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

        // Show QR code (if enabled)
        if (showQrCode) {
            val url = when (qrLinkType) {
                QrLinkType.SOURCE -> quote.sourceUrl ?: "https://readwise.io/open/${quote.id}"
                QrLinkType.READWISE -> "https://readwise.io/open/${quote.id}"
            }
            val qrBitmap = generateQrCode(url)
            if (qrBitmap != null) {
                qrCodeImage.setImageBitmap(qrBitmap)
                qrCodeImage.visibility = View.VISIBLE
            } else {
                qrCodeImage.visibility = View.GONE
            }
        } else {
            qrCodeImage.visibility = View.GONE
        }

        // Fade in new content
        fadeIn(fadeDuration) {
            // Schedule next quote
            handler?.postDelayed(displayRunnable, quoteDurationMs)
        }
    }

    private fun displayLibraryQuote(quote: Quote, fadeDuration: Long) {
        // Clear and rebuild library quote container
        libraryQuoteContainer.removeAllViews()

        // Scale text more aggressively for longer quotes to fit the screen
        val lengthMultiplier = when {
            quote.text.length > 800 -> 0.5f
            quote.text.length > 600 -> 0.6f
            quote.text.length > 400 -> 0.7f
            quote.text.length > 250 -> 0.8f
            quote.text.length > 150 -> 0.9f
            else -> 1f
        }

        // Quote text - italic serif, no curly quotes (matches Readwise style)
        val cleanText = cleanQuoteText(quote.text)
        val quoteWithAuthor = TextView(context).apply {
            text = cleanText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, baseQuoteSize * lengthMultiplier * textSizeScale)
            setTextColor(Color.parseColor("#1A1A1A"))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            setLineSpacing(dpToPx(6).toFloat(), 1.1f)
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        libraryQuoteContainer.addView(quoteWithAuthor)

        // Book title - sans-serif, dark text
        val bookTitleView = TextView(context).apply {
            text = quote.title ?: ""
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * textSizeScale)
            setTextColor(Color.parseColor("#1A1A1A"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            visibility = if (quote.title.isNullOrEmpty()) View.GONE else View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(32)
            }
        }
        libraryQuoteContainer.addView(bookTitleView)

        // Book author - smaller, slightly muted
        val bookAuthorView = TextView(context).apply {
            text = quote.author ?: ""
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * textSizeScale)
            setTextColor(Color.parseColor("#404040"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            visibility = if (quote.author.isNullOrEmpty()) View.GONE else View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
        }
        libraryQuoteContainer.addView(bookAuthorView)

        // Show note (if enabled and exists)
        if (showNotes && !quote.note.isNullOrEmpty()) {
            val noteView = TextView(context).apply {
                text = quote.note
                setTextSize(TypedValue.COMPLEX_UNIT_SP, baseNoteSize * textSizeScale)
                setTextColor(Color.parseColor("#AAAAAA"))
                typeface = Typeface.create("sans-serif-light", Typeface.ITALIC)
                setShadowLayer(2f, 0f, 1f, Color.parseColor("#40000000"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(16)
                }
            }
            libraryQuoteContainer.addView(noteView)
        }

        // Show tags
        if (showTags && quote.tags.isNotEmpty()) {
            tagsText.text = quote.tags.joinToString(", ")
            tagsText.setTextColor(Color.parseColor("#888888"))
            tagsText.visibility = View.VISIBLE
        } else {
            tagsText.visibility = View.GONE
        }

        // Show QR code
        if (showQrCode) {
            val url = when (qrLinkType) {
                QrLinkType.SOURCE -> quote.sourceUrl ?: "https://readwise.io/open/${quote.id}"
                QrLinkType.READWISE -> "https://readwise.io/open/${quote.id}"
            }
            val qrBitmap = generateQrCode(url)
            if (qrBitmap != null) {
                qrCodeImage.setImageBitmap(qrBitmap)
                qrCodeImage.visibility = View.VISIBLE
            } else {
                qrCodeImage.visibility = View.GONE
            }
        } else {
            qrCodeImage.visibility = View.GONE
        }

        // Load cover and extract colors
        loadCoverAndApplyColors(quote, quoteWithAuthor, bookTitleView, bookAuthorView)

        // Fade in new content
        fadeIn(fadeDuration) {
            // Schedule next quote
            handler?.postDelayed(displayRunnable, quoteDurationMs)
        }
    }

    private fun loadCoverAndApplyColors(
        quote: Quote,
        quoteTextView: TextView,
        bookTitleView: TextView,
        bookAuthorView: TextView
    ) {
        val coverUrl = quote.bookCover

        if (coverUrl.isNullOrEmpty()) {
            // No cover - use fallback colors and hide cover
            applyLibraryFallbackColors(quoteTextView, bookTitleView, bookAuthorView)
            coverImageView.visibility = View.GONE
            // Expand left panel to full width
            (libraryLeftPanel.layoutParams as LinearLayout.LayoutParams).weight = 1f
            (coverImageView.layoutParams as LinearLayout.LayoutParams).weight = 0f
            libraryLeftPanel.requestLayout()
            return
        }

        // Reset weights for cover display
        (libraryLeftPanel.layoutParams as LinearLayout.LayoutParams).weight = 0.6f
        (coverImageView.layoutParams as LinearLayout.LayoutParams).weight = 0.4f
        libraryLeftPanel.requestLayout()
        coverImageView.visibility = View.VISIBLE

        // Check cache first
        val cacheKey = coverUrl
        if (colorCache.containsKey(cacheKey)) {
            val cachedSwatch = colorCache[cacheKey]
            if (cachedSwatch != null) {
                applyExtractedColors(cachedSwatch, quoteTextView, bookTitleView, bookAuthorView)
            } else {
                applyLibraryFallbackColors(quoteTextView, bookTitleView, bookAuthorView)
            }
        }

        // Load cover image with Coil (disable hardware bitmaps for Palette API)
        val request = ImageRequest.Builder(context)
            .data(coverUrl)
            .allowHardware(false)
            .target { drawable ->
                coverImageView.setImageDrawable(drawable)

                // Extract colors from bitmap
                CoroutineScope(Dispatchers.Main).launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            drawable.toBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap).generate()
                        }

                        // Prefer vibrant swatches for colorful backgrounds like the reference
                        // Priority: light vibrant > vibrant > muted > light muted > dominant
                        val candidates = listOfNotNull(
                            palette.lightVibrantSwatch,
                            palette.vibrantSwatch,
                            palette.mutedSwatch,
                            palette.lightMutedSwatch,
                            palette.dominantSwatch
                        )

                        // Pick the first swatch that isn't too dark (brightness > 0.25)
                        val swatch = candidates.firstOrNull { sw ->
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(sw.rgb, hsv)
                            hsv[2] > 0.25f // brightness threshold
                        } ?: candidates.firstOrNull() ?: palette.dominantSwatch

                        colorCache[cacheKey] = swatch

                        if (swatch != null) {
                            applyExtractedColors(swatch, quoteTextView, bookTitleView, bookAuthorView)
                        } else {
                            applyLibraryFallbackColors(quoteTextView, bookTitleView, bookAuthorView)
                        }
                    }
                }
            }
            .build()

        context.imageLoader.enqueue(request)
    }

    private fun applyExtractedColors(
        swatch: Palette.Swatch,
        quoteTextView: TextView,
        bookTitleView: TextView,
        bookAuthorView: TextView
    ) {
        val bgColor = swatch.rgb

        // Create subtle gradient: slightly darker on left, base color on right
        val darkerLeft = Color.argb(
            255,
            maxOf(0, (Color.red(bgColor) * 0.92f).toInt()),
            maxOf(0, (Color.green(bgColor) * 0.92f).toInt()),
            maxOf(0, (Color.blue(bgColor) * 0.92f).toInt())
        )
        val panelGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(darkerLeft, bgColor)
        )
        libraryLeftPanel.background = panelGradient

        // Accent bar: semi-transparent dark color
        libraryAccentBar.setBackgroundColor(Color.parseColor("#35000000"))

        // Use palette's text colors for contrast
        val titleColor = swatch.titleTextColor
        val bodyColor = swatch.bodyTextColor

        quoteTextView.setTextColor(titleColor)
        bookTitleView.setTextColor(titleColor)
        bookAuthorView.setTextColor(bodyColor)
        tagsText.setTextColor(bodyColor)
    }

    private fun applyLibraryFallbackColors(
        quoteTextView: TextView,
        bookTitleView: TextView,
        bookAuthorView: TextView
    ) {
        val darkerLeft = Color.parseColor("#151515")
        val bgColor = Color.parseColor("#1E1E1E")

        // Subtle gradient: darker on left
        val panelGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(darkerLeft, bgColor)
        )
        libraryLeftPanel.background = panelGradient

        // Accent bar
        libraryAccentBar.setBackgroundColor(Color.parseColor("#35FFFFFF"))

        // Light text
        quoteTextView.setTextColor(Color.parseColor("#F5F5F5"))
        bookTitleView.setTextColor(Color.parseColor("#F5F5F5"))
        bookAuthorView.setTextColor(Color.parseColor("#CCCCCC"))
        tagsText.setTextColor(Color.parseColor("#888888"))
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
        val targetView = if (visualStyle == VisualStyle.LIBRARY) libraryContainer else quoteContainer
        ObjectAnimator.ofFloat(targetView, "alpha", 1f, 0f).apply {
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
        val targetView = if (visualStyle == VisualStyle.LIBRARY) libraryContainer else quoteContainer
        ObjectAnimator.ofFloat(targetView, "alpha", 0f, 1f).apply {
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

    private fun generateQrCode(content: String): Bitmap? {
        return try {
            val size = dpToPx(44)
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.WHITE else Color.TRANSPARENT)
                }
            }
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
