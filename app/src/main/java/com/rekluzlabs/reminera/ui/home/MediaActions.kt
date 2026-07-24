package com.rekluzlabs.reminera.ui.home

import com.rekluzlabs.reminera.data.FamilyMemberEntity

sealed interface MediaAction {
    data class Rename(val entryId: String, val newTitle: String) : MediaAction
    data class Move(val entryId: String, val targetMemberId: Long, val targetMemberName: String) : MediaAction
    data class Download(val entryId: String) : MediaAction
    data class Delete(val entryId: String) : MediaAction
}

sealed interface MediaActionResult {
    data object Success : MediaActionResult
    data class Error(val message: String) : MediaActionResult
}

data class MediaMenuState(
    val entryId: String,
    val entryTitle: String,
    val entryType: String,
    val currentMemberName: String?,
    val members: List<FamilyMemberEntity>
)
