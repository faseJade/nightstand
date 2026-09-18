package io.github.christianphilip.nightstand.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val HOLD_MILLIS = 1200

/**
 * Leaving Nightstand needs a 1.2-second press, so a bump or stray tap can't close it.
 * The back gesture is disabled while the app is open.
 */
@Composable
fun HoldToExitButton(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnExit by rememberUpdatedState(onExit)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(Palette.Ground)
            .drawBehind {
                drawRect(
                    color = Palette.HoldFill,
                    size = Size(size.width * progress.value, size.height),
                )
            }
            .border(1.dp, Palette.Border, shape)
            .semantics {
                contentDescription = "Hold to exit Nightstand"
                onLongClick(label = "Exit") { currentOnExit(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val fill = scope.launch {
                            progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                            currentOnExit()
                        }
                        val released = tryAwaitRelease()
                        if (fill.isActive || !released) {
                            fill.cancel()
                            scope.launch { progress.animateTo(0f, tween(150)) }
                        }
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp).height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = NightstandIcons.Exit,
                contentDescription = null,
                tint = Palette.Subtle,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (progress.value > 0f) "Keep holding…" else "Hold to exit",
                color = Palette.Subtle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
