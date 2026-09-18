package io.github.christianphilip.nightstand.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
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

/** An app that has posted a notification since Nightstand started. */
data class SeenApp(
    val packageName: String,
    val appName: String,
    val appIcon: ImageBitmap?,
)

/**
 * Shared, in-process bridge between the notification listener (which receives the phone's
 * notifications) and the screen (which shows them).
 */
object NotificationHub {
    private const val KEY_HIDDEN_PACKAGES = "hidden_packages"

    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _seenApps = MutableStateFlow<List<SeenApp>>(emptyList())
    val seenApps: StateFlow<List<SeenApp>> = _seenApps.asStateFlow()

    private val _hiddenPackages = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    @Volatile
    private var service: NightstandListenerService? = null

    private var rawItems: List<NotificationItem> = emptyList()
    private val seenAppMap = mutableMapOf<String, SeenApp>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        synchronized(this) {
            if (prefs == null) {
                val p = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
                prefs = p
                _hiddenPackages.value = p.getStringSet(KEY_HIDDEN_PACKAGES, emptySet()) ?: emptySet()
            }
        }
    }

    internal fun attach(s: NightstandListenerService) {
        init(s)
        synchronized(this) {
            service = s
            _connected.value = true
        }
    }

    internal fun detach(s: NightstandListenerService) {
        synchronized(this) {
            if (service === s) {
                service = null
                _connected.value = false
                rawItems = emptyList()
                recomputeFilteredItems()
            }
        }
    }

    internal fun publish(list: List<NotificationItem>) {
        synchronized(this) {
            rawItems = list
            var changedApps = false
            for (item in list) {
                val existing = seenAppMap[item.packageName]
                if (existing == null) {
                    seenAppMap[item.packageName] = SeenApp(item.packageName, item.appName, item.appIcon)
                    changedApps = true
                } else if (existing.appIcon == null && item.appIcon != null) {
                    seenAppMap[item.packageName] = existing.copy(appIcon = item.appIcon)
                    changedApps = true
                }
            }
            if (changedApps) {
                _seenApps.value = seenAppMap.values.sortedBy { it.appName.lowercase() }
            }
            recomputeFilteredItems()
        }
    }

    /** Toggles whether notifications from [packageName] are hidden. */
    fun setAppHidden(packageName: String, hidden: Boolean) {
        synchronized(this) {
            val current = _hiddenPackages.value
            val newSet = if (hidden) current + packageName else current - packageName
            _hiddenPackages.value = newSet
            prefs?.edit()?.putStringSet(KEY_HIDDEN_PACKAGES, newSet)?.apply()
            recomputeFilteredItems()
        }
    }

    private fun recomputeFilteredItems() {
        val hidden = _hiddenPackages.value
        _items.value = rawItems.filter { it.packageName !in hidden }
    }

    /** Removes the notification from Nightstand and from the phone's notification shade. */
    fun dismiss(key: String) {
        synchronized(this) {
            rawItems = rawItems.filterNot { it.key == key }
            recomputeFilteredItems()
            runCatching { service?.cancelNotification(key) }
        }
    }

    /** Clears every notification that the phone would let you swipe away. */
    fun clearAll() {
        synchronized(this) {
            rawItems = rawItems.filterNot { it.clearable }
            recomputeFilteredItems()
            runCatching { service?.cancelAllNotifications() }
        }
    }
}
