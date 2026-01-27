# Library Theme (U4) - Book Cover Art Display

## Overview

A new "Library" visual style that displays book covers prominently with a dynamic background color extracted from each cover.

## Reference

Split-screen layout inspired by Readwise's quote display: quote text on left (~60%), large book cover on right (~40%), with background color derived from the cover.

## Layout Structure

```
┌────────────────────────────────────┬──────────────────────┐
│                                    │                      │
│  "Quote text here..."              │      ┌─────────┐     │
│  —Quote Author                     │      │  BOOK   │     │
│                                    │      │  COVER  │─────│
│  Book Title                        │      │  IMAGE  │ crop
│  Book Author                       │      └─────────┘     │
│                                    │                      │
│  [tags, notes, QR if enabled]      │                      │
└────────────────────────────────────┴──────────────────────┘
        ~60% width                         ~40% width
```

## Design Details

### Cover Image Display

- **Size:** ~70% of screen height, vertically centered
- **Position:** Right side, partially cropped off-screen by ~15% of cover width
- **Aspect ratio:** Preserved (typical book covers are ~2:3)
- **Corner radius:** 8dp on visible corners
- **Loading:** Fade-in transition when loaded

### Fallback (No Cover)

- Hide cover area
- Expand text panel to full width
- Use neutral dark background (#1A1A1A)

### Dynamic Background Color

Uses Android Palette API to extract dominant color from book cover:

1. Load cover image
2. Generate palette from bitmap
3. Extract dominant or vibrant swatch
4. Apply swatch color to left panel background
5. Use swatch's `titleTextColor` / `bodyTextColor` for text contrast

Fallback: Dark neutral (#1A1A1A) with light text if extraction fails.

### Text Layout (Left Panel)

**Quote text:**
- Curly quotes with em-dash + author inline: `"Quote..." —Author`
- Margins: ~60dp from edges
- Font size: Respects text size setting (Small/Medium/Large)
- Color: Palette's titleTextColor

**Book info (below quote, 24dp gap):**
- Book title: Medium weight, slightly smaller than quote
- Book author: Regular weight, 80% opacity

**Optional elements:**
- Tags: Bottom-left, subtle styling
- Notes: Below book author, italic, muted
- QR code: Bottom-right of left panel

**Text shadow:** 2dp blur for edge case readability

## Technical Implementation

### Dependencies

- Android Palette API (AndroidX)
- Coil/Glide for image loading (existing)

### Files to Modify

| File | Changes |
|------|---------|
| `VisualStyle.kt` | Add `LIBRARY("Library")` enum value |
| `QuoteDisplayView.kt` | Add `applyLibraryTheme()` with split layout |
| `activity_main.xml` | Add ImageView for cover display |

### Color Extraction Flow

```kotlin
// Pseudocode
fun applyLibraryTheme(quote: Quote) {
    if (quote.bookCover != null) {
        loadImage(quote.bookCover) { bitmap ->
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.dominantSwatch ?: palette.vibrantSwatch
            if (swatch != null) {
                leftPanel.setBackgroundColor(swatch.rgb)
                quoteText.setTextColor(swatch.titleTextColor)
                bookInfo.setTextColor(swatch.bodyTextColor)
            } else {
                applyFallbackColors()
            }
        }
    } else {
        applyFallbackColors()
        hideCoverImage()
    }
}
```

### Performance

- Cache extracted colors per book ID to avoid re-processing
- Preload next quote's cover during current quote display

## Acceptance Criteria

- [ ] Library theme appears in Visual Style dropdown
- [ ] Cover displays on right, cropped off edge
- [ ] Background color extracted from cover
- [ ] Text colors auto-adjust for contrast
- [ ] Graceful fallback when no cover available
- [ ] Respects existing settings (text size, show tags, show notes, show QR)
