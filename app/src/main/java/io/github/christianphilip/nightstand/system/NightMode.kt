package io.github.christianphilip.nightstand.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/** How the red night tint is decided. */
enum class NightMode {
    /** Follow the room's light level, or the time of day when there is no light sensor. */
    AUTO,

    /** Always tinted. */
    ON,

    /** Never tinted. */
    OFF;

    fun next(): NightMode = when (this) {
        AUTO -> ON
        ON -> OFF
        OFF -> AUTO
    }

    val label: String
        get() = when (this) {
            AUTO -> "Night: auto"
            ON -> "Night: on"
            OFF -> "Night: off"
        }
}

/** The colour of the night tint: a deep warm red, multiplied over the screen. */
private val NightTint = Color(0xFFFF4A10)

/** Below this many lux the room counts as dark; above [LuxLight] it counts as lit. */
private const val LuxDark = 8f
private const val LuxLight = 60f

/**
 * How tinted the room says we should be (0..1), or null when the phone has no light
 * sensor. Smoothed and rounded to tenths so the value only changes when the light
 * really changes — a raw sensor reading would restart the fade several times a second.
 */
@Composable
private fun rememberAmbientNightTarget(enabled: Boolean): State<Float?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(context, enabled) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = if (enabled) manager?.getDefaultSensor(Sensor.TYPE_LIGHT) else null
        if (sensor == null || manager == null) {
            onDispose { }
        } else {
            var smoothed = -1f
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val lux = event?.values?.firstOrNull() ?: return
                    smoothed = if (smoothed < 0f) lux else smoothed + 0.25f * (lux - smoothed)
                    val raw = ((LuxLight - smoothed) / (LuxLight - LuxDark)).coerceIn(0f, 1f)
                    val stepped = (raw * 10f).roundToInt() / 10f
                    if (state.value != stepped) state.value = stepped
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { manager.unregisterListener(listener) }
        }
    }
    return state
}

/** 0 = normal colours, 1 = fully tinted. Fades over two seconds when it changes. */
@Composable
fun rememberNightFactor(mode: NightMode, hour: Int): State<Float> {
    val ambient = rememberAmbientNightTarget(enabled = mode == NightMode.AUTO)
    val target = when (mode) {
        NightMode.ON -> 1f
        NightMode.OFF -> 0f
        // No light sensor: tinted from 20:00 to 06:00.
        NightMode.AUTO -> ambient.value ?: if (hour >= 20 || hour < 6) 1f else 0f
    }
    return animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 2000),
        label = "nightFactor",
    )
}

/**
 * Multiplies a red wash over everything drawn beneath it. The factor is read inside
 * the draw lambda so a fade repaints without recomposing the screen.
 */
fun Modifier.nightTint(factor: () -> Float): Modifier = this.drawWithContent {
    drawContent()
    val f = factor()
    if (f > 0.01f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawRect(color = NightTint, alpha = f, blendMode = BlendMode.Multiply)
        } else {
            // Android 9 blends multiply through the alpha channel too; plain overlay instead.
            drawRect(color = NightTint.copy(alpha = f * 0.55f))
        }
    }
}

/**
 * Dims the screen as the tint comes in, and hands brightness back to the system
 * when night mode is off or the screen goes away.
 */
@Composable
fun ApplyScreenBrightness(factor: State<Float>) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        onDispose { activity?.setWindowBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) }
    }

    LaunchedEffect(activity) {
        val window = activity ?: return@LaunchedEffect
        snapshotFlow { factor.value }
            .map { f ->
                if (f <= 0.01f) {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                } else {
                    // Step in 2% increments so we only touch the window on real changes.
                    ((0.55f - 0.48f * f) * 50f).roundToInt() / 50f
                }
            }
            .distinctUntilChanged()
            .collect { brightness -> window.setWindowBrightness(brightness) }
    }
}

private fun Activity.setWindowBrightness(value: Float) {
    val params = window.attributes
    params.screenBrightness = if (value == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
        value
    } else {
        value.coerceIn(0.02f, 1f)
    }
    window.attributes = params
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
