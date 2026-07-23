package com.rekluzlabs.reminera.ui.tutorial

data class TutorialStep(
    val id: String,
    val targetKey: String?,
    val title: String,
    val description: String,
    val dismissOnRealAction: Boolean = false
)
