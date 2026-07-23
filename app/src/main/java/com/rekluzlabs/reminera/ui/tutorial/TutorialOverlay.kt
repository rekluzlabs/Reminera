package com.rekluzlabs.reminera.ui.tutorial

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TutorialOverlay(
    coordinator: TutorialCoordinator,
    screenKey: String,
    repository: TutorialRepository,
    onDismiss: () -> Unit = {},
) {
    val step = coordinator.currentStep ?: return
    if (!coordinator.isActive) return

    if (step.dismissOnRealAction) {
        DismissOnActionOverlay(
            step = step,
            coordinator = coordinator,
            screenKey = screenKey,
            repository = repository,
            onDismiss = onDismiss
        )
    } else {
        FullScreenOverlayPopup(
            step = step,
            coordinator = coordinator,
            screenKey = screenKey,
            repository = repository,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun FullScreenOverlayPopup(
    step: TutorialStep,
    coordinator: TutorialCoordinator,
    screenKey: String,
    repository: TutorialRepository,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { (config.screenHeightDp.dp + 48.dp).toPx() }

    val holePaddingPx = with(density) { 12.dp.toPx() }
    val roundedCornerPx = with(density) { 16.dp.toPx() }

    val targetBounds = step.targetKey?.let { coordinator.targets[it] }

    val cardWidthDp = 280.dp
    val cardPaddingDp = 16.dp
    val totalCardWidthDp = cardWidthDp + cardPaddingDp * 2

    val cardWidthPx = with(density) { totalCardWidthDp.toPx() }
    val cardHeightPx = with(density) { 200.dp.toPx() }

    val halfScreenPx = screenHeightPx / 2f
    val isAbove = targetBounds != null && targetBounds.center.y > halfScreenPx

    val tooltipXPx = if (targetBounds != null) {
        (targetBounds.center.x - cardWidthPx / 2f)
            .coerceIn(with(density) { 8.dp.toPx() }, screenWidthPx - cardWidthPx - with(density) { 8.dp.toPx() })
    } else {
        (screenWidthPx - cardWidthPx) / 2f
    }

    val tooltipYPx = if (targetBounds != null) {
        if (isAbove) {
            targetBounds.top - holePaddingPx - cardHeightPx - with(density) { 8.dp.toPx() }
        } else {
            targetBounds.bottom + holePaddingPx + with(density) { 8.dp.toPx() }
        }
    } else {
        (screenHeightPx - cardHeightPx) / 2f
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = 0.7f))
                if (targetBounds != null) {
                    val holeRect = Rect(
                        left = max(0f, targetBounds.left - holePaddingPx),
                        top = max(0f, targetBounds.top - holePaddingPx),
                        right = min(size.width, targetBounds.right + holePaddingPx),
                        bottom = min(size.height, targetBounds.bottom + holePaddingPx)
                    )
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(holeRect.left, holeRect.top),
                        size = Size(holeRect.width, holeRect.height),
                        cornerRadius = CornerRadius(roundedCornerPx),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(holeRect.left, holeRect.top),
                        size = Size(holeRect.width, holeRect.height),
                        cornerRadius = CornerRadius(roundedCornerPx),
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(tooltipXPx.roundToInt(), tooltipYPx.roundToInt()) }
                    .width(totalCardWidthDp)
            ) {
                TutorialTooltipCard(
                    step = step,
                    currentStepIndex = coordinator.currentStepIndex,
                    totalSteps = coordinator.steps.size,
                    showNextButton = true,
                    isLastStep = coordinator.currentStepIndex == coordinator.steps.size - 1,
                    onNext = { coordinator.advance() },
                    onSkip = {
                        coordinator.skip()
                        repository.markSeen(screenKey)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun DismissOnActionOverlay(
    step: TutorialStep,
    coordinator: TutorialCoordinator,
    screenKey: String,
    repository: TutorialRepository,
    onDismiss: () -> Unit,
) {
    val targetBounds = step.targetKey?.let { coordinator.targets[it] } ?: return

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { (config.screenHeightDp.dp + 48.dp).toPx() }

    val tooltipWidthDp = 260.dp
    val tooltipWidthPx = with(density) { tooltipWidthDp.toPx() }

    val halfScreenPx = screenHeightPx / 2f
    val isAbove = targetBounds.center.y > halfScreenPx

    val tooltipXPx = (targetBounds.center.x - tooltipWidthPx / 2f)
        .coerceIn(with(density) { 8.dp.toPx() }, screenWidthPx - tooltipWidthPx - with(density) { 8.dp.toPx() })

    val tooltipHeightPx = with(density) { 180.dp.toPx() }
    val tooltipYPx = if (isAbove) {
        targetBounds.top - with(density) { 12.dp.toPx() } - tooltipHeightPx
    } else {
        targetBounds.bottom + with(density) { 12.dp.toPx() }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(tooltipXPx.roundToInt(), tooltipYPx.roundToInt()) }
            .width(tooltipWidthDp)
    ) {
        TutorialTooltipCard(
            step = step,
            currentStepIndex = coordinator.currentStepIndex,
            totalSteps = coordinator.steps.size,
            showNextButton = false,
            isLastStep = false,
            onNext = {},
            onSkip = {
                coordinator.skip()
                repository.markSeen(screenKey)
                onDismiss()
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val targetSizeDp = with(density) { targetBounds.width.toDp() }
    val haloRadiusPx = with(density) { 6.dp.toPx() }
    val pulseColor = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)

    val offsetX = targetBounds.left.roundToInt()
    val offsetY = targetBounds.top.roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .then(Modifier.requiredSize(targetSizeDp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = pulseColor,
                radius = size.minDimension / 2f + haloRadiusPx
            )
        }
    }
}

@Composable
private fun TutorialTooltipCard(
    step: TutorialStep,
    currentStepIndex: Int,
    totalSteps: Int,
    showNextButton: Boolean,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip tutorial",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TutorialDots(
                        currentIndex = currentStepIndex,
                        totalCount = totalSteps
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (showNextButton) {
                        TextButton(onClick = onNext) {
                            Text(
                                text = if (isLastStep) "Got it" else "Next",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialDots(
    currentIndex: Int,
    totalCount: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalCount) { index ->
            val dotSize = if (index == currentIndex) 10.dp else 6.dp
            val dotColor = if (index == currentIndex)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            Box(
                modifier = Modifier
                    .then(Modifier.requiredSize(dotSize))
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
