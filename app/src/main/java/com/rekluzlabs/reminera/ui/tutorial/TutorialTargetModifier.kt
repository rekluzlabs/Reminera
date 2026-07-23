package com.rekluzlabs.reminera.ui.tutorial

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.tutorialTarget(
    key: String,
    coordinator: TutorialCoordinator
): Modifier = this.onGloballyPositioned { layoutCoordinates ->
    val bounds = layoutCoordinates.boundsInWindow()
    coordinator.registerTarget(key, bounds)
}
