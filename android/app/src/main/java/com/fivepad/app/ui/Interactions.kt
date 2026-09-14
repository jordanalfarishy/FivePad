package com.fivepad.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Small, interruptible press feedback; Compose respects the system animation scale. */
internal fun Modifier.playfulClick(onClick: () -> Unit): Modifier = composed {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.65f, stiffness = 650f),
        label = "press",
    )
    val haptics = LocalHapticFeedback.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactions, indication = LocalIndication.current) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}
