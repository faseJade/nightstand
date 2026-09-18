package io.github.christianphilip.nightstand.system

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BatteryInfo(val percent: Int, val charging: Boolean)

/** The current time, updated on every second tick. */
@Composable
fun rememberNow(): State<LocalDateTime> {
    val state = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            state.value = LocalDateTime.now()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    return state
}

@Composable
fun rememberBatteryInfo(): State<BatteryInfo?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<BatteryInfo?>(null) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let { state.value = parseBattery(it) }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { state.value = parseBattery(it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

private fun parseBattery(intent: Intent): BatteryInfo? {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(percent = level * 100 / scale, charging = charging)
}

/** The next alarm set in the phone's clock app, formatted for display, or null. */
@Composable
fun rememberNextAlarm(): State<String?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(readNextAlarm(context)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                state.value = readNextAlarm(context)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

private fun readNextAlarm(context: Context): String? {
    val am = context.getSystemService(AlarmManager::class.java) ?: return null
    val info = am.nextAlarmClock ?: return null
    val time = Instant.ofEpochMilli(info.triggerTime).atZone(ZoneId.systemDefault())
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val withinADay = info.triggerTime - System.currentTimeMillis() < 24L * 60 * 60 * 1000
    val pattern = if (withinADay) timePattern else "EEE $timePattern"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
