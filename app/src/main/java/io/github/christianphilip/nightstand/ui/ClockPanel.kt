package io.github.christianphilip.nightstand.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.christianphilip.nightstand.system.BatteryInfo
import io.github.christianphilip.nightstand.system.rememberBatteryInfo
import io.github.christianphilip.nightstand.system.rememberNextAlarm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Left half: exit button, settings button, big digital clock, weekday, date + month, alarm and battery. */
@Composable
fun ClockPanel(
    now: LocalDateTime,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val is24h = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()

    val hour = if (is24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val hh = if (is24h) "%02d".format(hour) else hour.toString()
    val mm = "%02d".format(now.minute)
    val suffix = if (is24h) null else if (now.hour < 12) "AM" else "PM"
    val weekday = now.format(DateTimeFormatter.ofPattern("EEEE", locale))
    val date = now.format(DateTimeFormatter.ofPattern("d MMMM", locale))

    val colonAlpha by animateFloatAsState(
        targetValue = if (now.second % 2 == 0) 1f else 0.25f,
        animationSpec = tween(250),
        label = "colon",
    )

    // Burn-in protection: nudge the clock a few pixels as the minutes change.
    val minute = now.minute
    val shiftX by animateDpAsState(((minute % 5 - 2) * 3).dp, tween(1500), label = "shiftX")
    val shiftY by animateDpAsState((((minute / 5) % 3 - 1) * 3).dp, tween(1500), label = "shiftY")

    val battery by rememberBatteryInfo()
    val alarm by rememberNextAlarm()

    // Clock digits are sized in dp so the phone's font-size setting can't push them off screen.
    // 12-hour times carry an AM/PM label, so the digits are a little smaller to fit.
    val clockSize = with(LocalDensity.current) { (if (is24h) 124.dp else 108.dp).toSp() }
    val clockStyle = TextStyle(
        fontFamily = ClockFontFamily,
        fontSize = clockSize,
        lineHeight = clockSize,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
        color = Palette.Text,
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HoldToExitButton(onExit = onExit)
            GhostButton(
                icon = NightstandIcons.Settings,
                contentDescription = "Notification Settings",
                onClick = onOpenSettings,
            )
        }

        Column(
            modifier = Modifier
                .offset(x = shiftX, y = shiftY)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(hh, style = clockStyle)
                Text(
                    ":",
                    style = clockStyle.copy(color = Palette.Accent),
                    modifier = Modifier
                        .alpha(colonAlpha)
                        .padding(horizontal = 2.dp),
                )
                Text(mm, style = clockStyle)
                if (suffix != null) {
                    Text(
                        suffix,
                        color = Palette.Muted,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp, bottom = 18.dp),
                    )
                }
            }
            Text(
                weekday,
                color = Palette.Accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )
            Text(
                date,
                color = Palette.Muted,
                fontSize = 19.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
            StatusLine(alarm = alarm, battery = battery)
        }

        // Balances the exit button so the clock sits in the visual middle.
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun StatusLine(alarm: String?, battery: BatteryInfo?) {
    if (alarm == null && battery == null) return
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alarm != null) {
            StatusItem(NightstandIcons.Alarm, "Alarm $alarm")
        }
        if (battery != null) {
            StatusItem(
                icon = if (battery.charging) NightstandIcons.Charging else NightstandIcons.Battery,
                text = if (battery.charging) "Charging · ${battery.percent}%" else "${battery.percent}%",
            )
        }
    }
}

@Composable
private fun StatusItem(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(16.dp))
        Text(text, color = Palette.Muted, fontSize = 14.sp)
    }
}
