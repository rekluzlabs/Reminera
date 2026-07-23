package com.rekluzlabs.reminera.ui.tutorial

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect

class TutorialCoordinator {
    private val _targets = mutableStateMapOf<String, Rect>()
    val targets: Map<String, Rect> get() = _targets

    private val _isActive = mutableStateOf(false)
    var isActive: Boolean
        get() = _isActive.value
        internal set(value) { _isActive.value = value }

    private val _currentStepIndex = mutableIntStateOf(0)
    var currentStepIndex: Int
        get() = _currentStepIndex.intValue
        internal set(value) { _currentStepIndex.intValue = value }

    var steps: List<TutorialStep> = emptyList()
        private set

    val currentStep: TutorialStep?
        get() = steps.getOrNull(currentStepIndex)

    private var onComplete: (() -> Unit)? = null

    fun start(
        steps: List<TutorialStep>,
        onComplete: (() -> Unit)? = null
    ) {
        this.steps = steps
        this.currentStepIndex = 0
        this.isActive = true
        this.onComplete = onComplete
    }

    fun advance() {
        if (!isActive) return
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            finish()
        }
    }

    fun skip() {
        if (!isActive) return
        finish()
    }

    private fun finish() {
        isActive = false
        currentStepIndex = 0
        steps = emptyList()
        onComplete?.invoke()
        onComplete = null
    }

    fun registerTarget(key: String, bounds: Rect) {
        val existing = _targets[key]
        if (existing != bounds) {
            _targets[key] = bounds
        }
    }

    fun unregisterTarget(key: String) {
        _targets.remove(key)
    }
}
