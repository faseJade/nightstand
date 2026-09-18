package io.github.christianphilip.nightstand.ui

import android.graphics.Typeface
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Warm near-black palette, tuned for a dark bedroom. Matches the canvas mockup. */
object Palette {
    val Ground = Color(0xFF0B0A09)
    val Panel = Color(0xFF181614)
    val Card = Color(0xFF24211E)
    val Chip = Color(0xFF2A2622)
    val ChipPressed = Color(0xFF36312C)
    val Border = Color(0xFF3A3530)
    val HoldFill = Color(0xFF3A2E20)
    val Text = Color(0xFFF4EFE7)
    val Muted = Color(0xFFB3ABA0)
    val Subtle = Color(0xFFA8A198)
    val Body = Color(0xFFC9C2B8)
    val Accent = Color(0xFFF0A94B)
}

/** Android's built-in condensed sans at semi-bold weight, for the big clock digits. */
val ClockFontFamily: FontFamily = FontFamily(
    Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL), 600, false)
)

@Composable
fun NightstandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            onPrimary = Palette.Ground,
            background = Palette.Ground,
            onBackground = Palette.Text,
            surface = Palette.Panel,
            onSurface = Palette.Text,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides Palette.Text) {
            content()
        }
    }
}

/** Simple stroke icons (16-unit grid) so the app needs no icon library. */
object NightstandIcons {
    val Exit: ImageVector = strokeIcon("exit", "M6 3H3v10h3M10 5l3 3-3 3M13 8H6")
    val Close: ImageVector = strokeIcon("close", "M4 4l8 8M12 4l-8 8", width = 1.8f)
    val Alarm: ImageVector = strokeIcon(
        "alarm",
        "M3 9a5 5 0 1 0 10 0a5 5 0 1 0 -10 0M8 6.5V9l1.5 1.5M2.5 3.5l2-1.5M13.5 3.5l-2-1.5",
    )
    val Battery: ImageVector = strokeIcon(
        "battery",
        "M3.5 4h9a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-9a2 2 0 0 1 -2 -2v-4a2 2 0 0 1 2 -2zM16.5 7v2",
        viewport = 18f,
    )
    val Charging: ImageVector = strokeIcon(
        "charging",
        "M3.5 4h9a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-9a2 2 0 0 1 -2 -2v-4a2 2 0 0 1 2 -2zM16.5 7v2M8.5 5.5L6.5 8.5h3l-2 3",
        viewport = 18f,
    )
    val PopupsOff: ImageVector = strokeIcon(
        "popups_off",
        "M4 11V7a4 4 0 0 1 6.5-3.1M12 7v4l1 1H5M6.5 14h3M2 2l12 12",
    )

    private fun strokeIcon(
        name: String,
        path: String,
        width: Float = 1.5f,
        viewport: Float = 16f,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = (24f * viewport / 16f).dp,
        defaultHeight = 24.dp,
        viewportWidth = viewport,
        viewportHeight = 16f,
    ).addPath(
        pathData = addPathNodes(path),
        stroke = SolidColor(Color.White),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}
