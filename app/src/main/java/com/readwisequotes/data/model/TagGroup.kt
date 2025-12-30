// app/src/main/java/com/readwisequotes/data/model/TagGroup.kt
package com.readwisequotes.data.model

import com.readwisequotes.settings.TagFilterMode
import java.util.UUID

data class TagGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tags: Set<String>,
    val isEnabled: Boolean = false,
    val matchMode: TagFilterMode = TagFilterMode.ANY
)
