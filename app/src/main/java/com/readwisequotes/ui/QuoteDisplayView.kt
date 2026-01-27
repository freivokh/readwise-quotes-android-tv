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
    private val libraryContainer: FrameLayout
    private val libraryBackgroundPanel: FrameLayout
    private val libraryAccentBar: View
    private val libraryQuoteContainer: LinearLayout
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

        // Library theme: FrameLayout with full-screen gradient and floating cover
        libraryContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE
            clipChildren = false
            clipToPadding = false
        }
        addView(libraryContainer)

        // Full-screen background panel (holds the gradient)
        libraryBackgroundPanel = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        libraryContainer.addView(libraryBackgroundPanel)

        // Library quote container with accent bar integrated
        // Using a horizontal layout: [accent bar] [quote content]
        // Screen is 960dp wide - text gets ~68%, cover gets ~30% visible
        val quoteWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(48)
                marginEnd = dpToPx(300) // Text ends at 660dp, before cover at 680dp
            }
        }
        libraryContainer.addView(quoteWrapper)

        // Light accent bar - white semi-transparent to match Readwise style
        libraryAccentBar = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(4),
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                marginEnd = dpToPx(24)
            }
            setBackgroundColor(Color.parseColor("#50FFFFFF"))
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

        // Cover image - LEFT edge anchored at fixed position from screen left
        // Screen is 960dp wide at 320dpi (1920x1080)
        // Cover left edge at ~70%, extends past right edge (50-60% hidden)
        coverImageView = ImageView(context).apply {
            layoutParams = LayoutParams(dpToPx(400), dpToPx(500)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                marginStart = dpToPx(680) // Left edge at ~70% of 960dp screen
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            rotation = 5f
            // Pivot from left edge - left side stays anchored while right swings
            post {
                pivotX = 0f
                pivotY = height / 2f
            }
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
        libraryBackgroundPanel.setBackgroundColor(Color.parseColor("#1A1A1A"))

        // Base text sizes for Library theme
        baseQuoteSize = 24f
        baseAuthorSize = 16f
        baseSourceSize = 14f
        baseNoteSize = 12f

        // Bring QR code and tags to front (they were added before libraryContainer)
        qrCodeImage.bringToFront()
        tagsText.bringToFront()

        // Move QR code to bottom-left with proper margins
        qrCodeImage.layoutParams = LayoutParams(dpToPx(44), dpToPx(44)).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            marginStart = dpToPx(60)
            marginEnd = 0
            bottomMargin = dpToPx(40)
            topMargin = 0
        }

        // Move tags to bottom-left above QR code
        tagsText.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            marginStart = dpToPx(60)
            marginEnd = 0
            bottomMargin = dpToPx(100)
            topMargin = 0
        }
    }

    private fun hideLibraryTheme() {
        libraryContainer.visibility = View.GONE
        quoteContainer.visibility = View.VISIBLE

        // Reset QR code position to bottom-right corner
        qrCodeImage.layoutParams = LayoutParams(dpToPx(44), dpToPx(44)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            marginStart = 0
            marginEnd = dpToPx(40)
            bottomMargin = dpToPx(40)
            topMargin = 0
        }

        // Reset tags position to bottom-center
        tagsText.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            marginStart = 0
            marginEnd = 0
            bottomMargin = dpToPx(40)
            topMargin = 0
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

        // Clean the text first to get accurate length
        val cleanText = cleanQuoteText(quote.text)

        // Scale text more aggressively for longer quotes to always fit screen
        val lengthMultiplier = when {
            cleanText.length > 1500 -> 0.35f
            cleanText.length > 1200 -> 0.4f
            cleanText.length > 1000 -> 0.45f
            cleanText.length > 800 -> 0.5f
            cleanText.length > 600 -> 0.6f
            cleanText.length > 400 -> 0.7f
            cleanText.length > 250 -> 0.8f
            cleanText.length > 150 -> 0.9f
            else -> 1f
        }

        // Also reduce line spacing for very long quotes
        val lineSpacingExtra = when {
            cleanText.length > 1000 -> dpToPx(2).toFloat()
            cleanText.length > 600 -> dpToPx(4).toFloat()
            else -> dpToPx(6).toFloat()
        }

        // Quote text - italic serif, no curly quotes (matches Readwise style)
        val quoteWithAuthor = TextView(context).apply {
            text = cleanText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, baseQuoteSize * lengthMultiplier * textSizeScale)
            setTextColor(Color.parseColor("#1A1A1A"))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            setLineSpacing(lineSpacingExtra, 1.05f)
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

        // Get the quote wrapper to adjust margins
        val quoteWrapper = libraryContainer.getChildAt(1) as? LinearLayout

        if (coverUrl.isNullOrEmpty()) {
            // No cover - use fallback colors and hide cover
            applyLibraryFallbackColors(quoteTextView, bookTitleView, bookAuthorView)
            coverImageView.visibility = View.GONE
            // Expand text to use full width
            quoteWrapper?.let {
                (it.layoutParams as LayoutParams).marginEnd = dpToPx(60)
                it.requestLayout()
            }
            return
        }

        // Show cover and restore margin for text (cover at far right, ~30% visible)
        coverImageView.visibility = View.VISIBLE
        quoteWrapper?.let {
            (it.layoutParams as LayoutParams).marginEnd = dpToPx(300)
            it.requestLayout()
        }

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
                // Check aspect ratio to adjust cover size
                val imgWidth = drawable.intrinsicWidth
                val imgHeight = drawable.intrinsicHeight
                val aspectRatio = if (imgHeight > 0) imgWidth.toFloat() / imgHeight else 1f

                coverImageView.setImageDrawable(drawable)
                coverImageView.visibility = View.VISIBLE

                // Adjust cover size based on aspect ratio
                val quoteWrapperView = libraryContainer.getChildAt(1) as? LinearLayout
                val coverParams = coverImageView.layoutParams as LayoutParams

                when {
                    aspectRatio > 1.2f -> {
                        // Very wide/landscape - make much smaller
                        coverParams.width = dpToPx(200)
                        coverParams.height = dpToPx(200)
                        coverParams.marginStart = dpToPx(780) // Push further right
                        quoteWrapperView?.let {
                            (it.layoutParams as LayoutParams).marginEnd = dpToPx(200)
                            it.requestLayout()
                        }
                    }
                    aspectRatio > 0.9f -> {
                        // Square or slightly wide - shrink moderately
                        coverParams.width = dpToPx(280)
                        coverParams.height = dpToPx(320)
                        coverParams.marginStart = dpToPx(720) // Push a bit right
                        quoteWrapperView?.let {
                            (it.layoutParams as LayoutParams).marginEnd = dpToPx(260)
                            it.requestLayout()
                        }
                    }
                    else -> {
                        // Portrait (normal book cover) - standard size
                        coverParams.width = dpToPx(400)
                        coverParams.height = dpToPx(500)
                        coverParams.marginStart = dpToPx(680)
                        quoteWrapperView?.let {
                            (it.layoutParams as LayoutParams).marginEnd = dpToPx(300)
                            it.requestLayout()
                        }
                    }
                }
                coverImageView.layoutParams = coverParams

                // Reset pivot point for rotation
                coverImageView.post {
                    coverImageView.pivotX = 0f
                    coverImageView.pivotY = coverImageView.height / 2f
                }

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

        // Create subtle gradient: slightly darker on left, base color on right (full screen)
        val darkerLeft = Color.argb(
            255,
            maxOf(0, (Color.red(bgColor) * 0.85f).toInt()),
            maxOf(0, (Color.green(bgColor) * 0.85f).toInt()),
            maxOf(0, (Color.blue(bgColor) * 0.85f).toInt())
        )
        val panelGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(darkerLeft, bgColor)
        )
        libraryBackgroundPanel.background = panelGradient

        // Calculate luminance to determine if background is light or dark
        val luminance = calculateLuminance(bgColor)
        val isLightBackground = luminance > 0.45

        // Use high-contrast text colors based on background luminance
        // Accent bar is always light/white (semi-transparent) to match Readwise style
        val titleColor: Int
        val bodyColor: Int
        val accentBarColor: Int

        if (isLightBackground) {
            // Dark text on light background
            titleColor = Color.parseColor("#1A1A1A")
            bodyColor = Color.parseColor("#404040")
            accentBarColor = Color.parseColor("#40FFFFFF") // White accent bar
        } else {
            // Light text on dark background
            titleColor = Color.parseColor("#F5F5F5")
            bodyColor = Color.parseColor("#CCCCCC")
            accentBarColor = Color.parseColor("#50FFFFFF") // White accent bar
        }

        libraryAccentBar.setBackgroundColor(accentBarColor)
        quoteTextView.setTextColor(titleColor)
        bookTitleView.setTextColor(titleColor)
        bookAuthorView.setTextColor(bodyColor)
        tagsText.setTextColor(bodyColor)
    }

    /** Calculate relative luminance using sRGB formula */
    private fun calculateLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0

        val rLinear = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    private fun applyLibraryFallbackColors(
        quoteTextView: TextView,
        bookTitleView: TextView,
        bookAuthorView: TextView
    ) {
        val darkerLeft = Color.parseColor("#101010")
        val bgColor = Color.parseColor("#1E1E1E")

        // Subtle gradient: darker on left (full screen)
        val panelGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(darkerLeft, bgColor)
        )
        libraryBackgroundPanel.background = panelGradient

        // White accent bar (semi-transparent)
        libraryAccentBar.setBackgroundColor(Color.parseColor("#50FFFFFF"))

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
            // Remove markdown links: [text](url) -> text
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            // Remove markdown bold/italic: **text**, *text*, __text__, _text_
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("_(.+?)_"), "$1")
            // Remove blockquote markers
            .replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
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
