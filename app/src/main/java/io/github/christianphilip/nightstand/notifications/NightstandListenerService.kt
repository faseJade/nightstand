package io.github.christianphilip.nightstand.notifications

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import io.github.christianphilip.nightstand.focus.FocusController

/**
 * Receives a copy of every notification on the phone once the user turns on
 * Settings → Notifications → Notification access → Nightstand.
 *
 * Notifications silenced by Nightstand's Do Not Disturb mode are still delivered here,
 * which is how they end up inside the app instead of popping up.
 */
class NightstandListenerService : NotificationListenerService() {

    private data class AppInfo(val label: String, val icon: ImageBitmap?)

    private val appCache = HashMap<String, AppInfo>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationHub.attach(this)
        // If the app crashed while Do Not Disturb was on, put the phone back to normal.
        if (!FocusController.sessionVisible) {
            FocusController(applicationContext).restore()
        }
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationHub.detach(this)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onDestroy() {
        NotificationHub.detach(this)
        super.onDestroy()
    }

    private fun refresh() {
        val active: Array<StatusBarNotification> = try {
            activeNotifications ?: emptyArray()
        } catch (e: SecurityException) {
            emptyArray()
        } catch (e: RuntimeException) {
            emptyArray()
        }

        val groupsWithChildren = active
            .filter { !isGroupSummary(it) }
            .mapNotNull { it.groupKey }
            .toSet()

        val items = active
            .asSequence()
            .filter { it.packageName != packageName }
            .filter { !it.isOngoing }
            .filter { !(isGroupSummary(it) && it.groupKey in groupsWithChildren) }
            .map { toItem(it) }
            .filter { it.title.isNotBlank() || it.text.isNotBlank() }
            .sortedByDescending { it.postTime }
            .toList()

        NotificationHub.publish(items)
    }

    private fun isGroupSummary(sbn: StatusBarNotification): Boolean =
        (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    private fun toItem(sbn: StatusBarNotification): NotificationItem {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
        val app = appInfo(sbn.packageName)
        return NotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = app.label,
            appIcon = app.icon,
            title = title.trim(),
            text = text.trim().replace('\n', ' '),
            postTime = sbn.postTime,
            contentIntent = n.contentIntent,
            clearable = sbn.isClearable,
            autoCancel = (n.flags and Notification.FLAG_AUTO_CANCEL) != 0,
        )
    }

    private fun appInfo(pkg: String): AppInfo = appCache.getOrPut(pkg) {
        try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            val label = pm.getApplicationLabel(info).toString()
            val icon = runCatching {
                pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
            AppInfo(label, icon)
        } catch (e: PackageManager.NameNotFoundException) {
            AppInfo(pkg.substringAfterLast('.'), null)
        }
    }
}
