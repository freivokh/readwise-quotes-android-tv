// app/src/main/java/com/readwisequotes/settings/QuoteFilter.kt
package com.readwisequotes.settings

enum class QuoteFilter {
    ALL,
    FAVORITES,
    BY_TAG,
    RECENT
}

enum class TagFilterMode {
    ANY,  // OR - quotes with any of the selected tags
    ALL   // AND - quotes with all of the selected tags
}
