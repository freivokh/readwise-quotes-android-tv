# D-pad Quote Navigation

## Overview

Add LEFT/RIGHT D-pad controls to cycle through quotes manually on the main display screen.

## Behavior

- **D-pad RIGHT**: Show next quote
- **D-pad LEFT**: Show previous quote
- **Timer**: Resets after each manual navigation (full duration to read new quote)
- **Animation**: Faster fade for manual nav (~300ms) vs auto-advance (~500ms)
- **Wrap-around**: Navigation wraps at both ends of the list
- **Shuffle**: Quotes remain shuffled on load; navigation is sequential through shuffled list

## Implementation

### QuoteDisplayView.kt

1. Add `isManualNavigation` parameter to `displayCurrentQuote()` to control fade speed
2. Make `showNextQuote()` public, add manual nav flag
3. Add `showPreviousQuote()` method - decrements index with wrap-around
4. Both methods reset the auto-advance timer

### MainActivity.kt

1. Handle `KEYCODE_DPAD_LEFT` → `quoteDisplayView.showPreviousQuote()`
2. Handle `KEYCODE_DPAD_RIGHT` → `quoteDisplayView.showNextQuote()`

## Files Changed

- `app/src/main/java/com/readwisequotes/ui/QuoteDisplayView.kt`
- `app/src/main/java/com/readwisequotes/ui/MainActivity.kt`
