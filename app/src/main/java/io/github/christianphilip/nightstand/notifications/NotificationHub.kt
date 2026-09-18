package io.github.christianphilip.nightstand.notifications

import android.app.PendingIntent
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One notification as Nightstand shows it. */
data class NotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val appIcon: ImageBitmap?,
    val title: String,
    val text: String,
    val postTime: Long,
    val contentIntent: PendingIntent?,
    val clearable: Boolean,
    val autoCancel: Boolean,
)

/**
 * Shared, in-process bridge between the notification listener (which receives the phone's
 * notifications) and the screen (which shows them).
 */
object NotificationHub {
    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Volatile
    private var service: NightstandListenerService? = null

    internal fun attach(s: NightstandListenerService) {
        service = s
        _connected.value = true
    }

    internal fun detach(s: NightstandListenerService) {
        if (service === s) {
            service = null
            _connected.value = false
            _items.value = emptyList()
        }
    }

    internal fun publish(list: List<NotificationItem>) {
        _items.value = list
    }

    /** Removes the notification from Nightstand and from the phone's notification shade. */
    fun dismiss(key: String) {
        _items.value = _items.value.filterNot { it.key == key }
        runCatching { service?.cancelNotification(key) }
    }

    /** Clears every notification that the phone would let you swipe away. */
    fun clearAll() {
        _items.value = _items.value.filterNot { it.clearable }
        runCatching { service?.cancelAllNotifications() }
    }
}
