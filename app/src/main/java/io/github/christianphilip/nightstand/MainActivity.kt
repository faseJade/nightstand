package io.github.christianphilip.nightstand

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.christianphilip.nightstand.focus.FocusController
import io.github.christianphilip.nightstand.notifications.NightstandListenerService
import io.github.christianphilip.nightstand.notifications.NotificationHub
import io.github.christianphilip.nightstand.notifications.NotificationItem
import io.github.christianphilip.nightstand.ui.NightstandScreen
import io.github.christianphilip.nightstand.ui.NightstandTheme
import io.github.christianphilip.nightstand.ui.SetupScreen

class MainActivity : ComponentActivity() {

    private lateinit var focus: FocusController

    private var listenerEnabled by mutableStateOf(false)
    private var silencingAllowed by mutableStateOf(false)
    private var skipSilencing by mutableStateOf(false)
    private var popupsOff by mutableStateOf(false)

    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        focus = FocusController(applicationContext)
        if (savedInstanceState == null) {
            // Clean up if a previous session ended without switching silencing off.
            focus.restore()
        }
        skipSilencing = prefs.getBoolean(KEY_SKIP_SILENCING, false)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)

        // Back does nothing here; leaving needs the hold-to-exit button.
        onBackPressedDispatcher.addCallback(this) { }

        setContent {
            NightstandTheme {
                if (listenerEnabled && (silencingAllowed || skipSilencing)) {
                    NightstandScreen(
                        popupsOff = popupsOff,
                        onOpen = ::openNotification,
                        onExit = ::finish,
                    )
                } else {
                    SetupScreen(
                        listenerEnabled = listenerEnabled,
                        silencingAllowed = silencingAllowed,
                        onGrantListener = {
                            openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        },
                        onGrantSilencing = {
                            openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        },
                        onSkipSilencing = {
                            skipSilencing = true
                            prefs.edit().putBoolean(KEY_SKIP_SILENCING, true).apply()
                        },
                        onExit = ::finish,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FocusController.sessionVisible = true
        hideSystemBars()
        refreshPermissions()

        if (listenerEnabled) {
            if (!NotificationHub.connected.value) {
                // Some phones don't bind the listener straight after it is switched on.
                NotificationListenerService.requestRebind(
                    ComponentName(this, NightstandListenerService::class.java)
                )
            }
            if (silencingAllowed) focus.enable()
        }
        popupsOff = focus.isActive
    }

    override fun onPause() {
        FocusController.sessionVisible = false
        focus.restore()
        popupsOff = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun refreshPermissions() {
        listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        silencingAllowed = focus.hasAccess
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** Opens the app that posted the notification, unlocking the phone first if needed. */
    private fun openNotification(item: NotificationItem) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard != null && keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        launchNotification(item)
                    }
                },
            )
        } else {
            launchNotification(item)
        }
    }

    private fun launchNotification(item: NotificationItem) {
        val intent = item.contentIntent ?: return
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                .toBundle()
        } else {
            null
        }
        try {
            intent.send(this, 0, null, null, null, null, options)
            if (item.autoCancel) NotificationHub.dismiss(item.key)
        } catch (e: PendingIntent.CanceledException) {
            NotificationHub.dismiss(item.key)
        }
    }

    private companion object {
        const val KEY_SKIP_SILENCING = "skip_silencing"
    }
}
