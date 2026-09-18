package io.github.christianphilip.nightstand.focus

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Turns on a Do Not Disturb mode while Nightstand is on screen, and puts the phone's own
 * Do Not Disturb settings back exactly as they were when Nightstand closes.
 *
 * While active: no pop-ups (heads-up), sounds or vibration from notifications.
 * Still allowed through: alarms, media (music keeps playing) and phone calls.
 * Silenced notifications still reach [NightstandListenerService], so they show in the app.
 */
class FocusController(context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val hasAccess: Boolean
        get() = nm?.isNotificationPolicyAccessGranted == true

    /** True while Nightstand's silencing is switched on. Survives crashes via prefs. */
    val isActive: Boolean
        get() = prefs.getBoolean(KEY_ACTIVE, false)

    fun enable() {
        val nm = nm ?: return
        if (!hasAccess || isActive) return

        val previous = nm.notificationPolicy
        val editor = prefs.edit()
            .putInt(KEY_FILTER, nm.currentInterruptionFilter)
            .putInt(KEY_CATEGORIES, previous.priorityCategories)
            .putInt(KEY_CALL_SENDERS, previous.priorityCallSenders)
            .putInt(KEY_MESSAGE_SENDERS, previous.priorityMessageSenders)
            .putInt(KEY_EFFECTS, previous.suppressedVisualEffects)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            editor.putInt(KEY_CONVERSATION_SENDERS, previous.priorityConversationSenders)
        }
        // Write the backup synchronously before touching anything.
        editor.putBoolean(KEY_ACTIVE, true).commit()

        val categories = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
            NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
            NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
            NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS

        val effects = NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS or
            NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT

        try {
            nm.notificationPolicy = NotificationManager.Policy(
                categories,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                effects,
            )
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } catch (e: RuntimeException) {
            restore()
        }
    }

    /** Puts Do Not Disturb back how the user had it. Safe to call any time. */
    fun restore() {
        val nm = nm ?: return
        if (!isActive) return
        if (!hasAccess) {
            // Access was revoked; Android already dropped our changes.
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
            return
        }
        try {
            val categories = prefs.getInt(KEY_CATEGORIES, 0)
            val calls = prefs.getInt(KEY_CALL_SENDERS, NotificationManager.Policy.PRIORITY_SENDERS_ANY)
            val messages = prefs.getInt(KEY_MESSAGE_SENDERS, NotificationManager.Policy.PRIORITY_SENDERS_ANY)
            val effects = prefs.getInt(KEY_EFFECTS, 0)
            nm.notificationPolicy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                NotificationManager.Policy(
                    categories, calls, messages, effects,
                    prefs.getInt(
                        KEY_CONVERSATION_SENDERS,
                        NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT
                    ),
                )
            } else {
                NotificationManager.Policy(categories, calls, messages, effects)
            }
            val filter = prefs.getInt(KEY_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
            nm.setInterruptionFilter(
                if (filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    filter
                }
            )
        } catch (e: RuntimeException) {
            // Nothing more we can do.
        } finally {
            prefs.edit().putBoolean(KEY_ACTIVE, false).commit()
        }
    }

    companion object {
        /** Set by MainActivity while it is on screen. */
        @Volatile
        var sessionVisible: Boolean = false

        private const val PREFS = "focus"
        private const val KEY_ACTIVE = "active"
        private const val KEY_FILTER = "filter"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_CALL_SENDERS = "call_senders"
        private const val KEY_MESSAGE_SENDERS = "message_senders"
        private const val KEY_CONVERSATION_SENDERS = "conversation_senders"
        private const val KEY_EFFECTS = "effects"
    }
}
