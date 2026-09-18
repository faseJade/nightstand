package io.github.christianphilip.nightstand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.christianphilip.nightstand.notifications.NotificationHub
import io.github.christianphilip.nightstand.notifications.NotificationItem
import io.github.christianphilip.nightstand.system.rememberNow

/**
 * The main landscape screen, laid out for an 800 × 360 dp window
 * (Galaxy A12 and similar 720 × 1600 phones).
 */
@Composable
fun NightstandScreen(
    popupsOff: Boolean,
    onOpen: (NotificationItem) -> Unit,
    onExit: () -> Unit,
) {
    val notifications by NotificationHub.items.collectAsStateWithLifecycle()
    val seenApps by NotificationHub.seenApps.collectAsStateWithLifecycle()
    val hiddenPackages by NotificationHub.hiddenPackages.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    val now by rememberNow()
    val nowMillis = remember(now.minute, notifications) { System.currentTimeMillis() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Ground)
            // The A12's camera notch sits on the left in this orientation.
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ClockPanel(
            now = now,
            onExit = onExit,
            onOpenSettings = { showSettings = true },
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
        )
        NotificationPanel(
            notifications = notifications,
            nowMillis = nowMillis,
            popupsOff = popupsOff,
            onOpen = onOpen,
            onDismiss = NotificationHub::dismiss,
            onClearAll = NotificationHub::clearAll,
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
        )
    }

    if (showSettings) {
        AppSettingsDialog(
            seenApps = seenApps,
            hiddenPackages = hiddenPackages,
            onToggleApp = { packageName, hidden ->
                NotificationHub.setAppHidden(packageName, hidden)
            },
            onDismiss = { showSettings = false },
        )
    }
}

/** First-run screen: asks for the two permissions Nightstand needs. */
@Composable
fun SetupScreen(
    listenerEnabled: Boolean,
    silencingAllowed: Boolean,
    onGrantListener: () -> Unit,
    onGrantSilencing: () -> Unit,
    onSkipSilencing: () -> Unit,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Ground)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(0.42f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Nightstand", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = Palette.Accent)
            Text(
                "A bedside clock that keeps your notifications in one quiet list.",
                fontSize = 16.sp,
                color = Palette.Muted,
            )
            PillButton(text = "Close", onClick = onExit)
        }
        Column(
            modifier = Modifier.weight(0.58f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionCard(
                title = "Notification access",
                body = "Needed to show your notifications on the right-hand side.",
                done = listenerEnabled,
                action = "Turn on",
                onAction = onGrantListener,
            )
            PermissionCard(
                title = "Do Not Disturb access",
                body = "Stops pop-ups and sounds while Nightstand is open. Alarms and calls still come through.",
                done = silencingAllowed,
                action = "Turn on",
                onAction = onGrantSilencing,
                secondaryAction = if (silencingAllowed) null else "Skip",
                onSecondaryAction = onSkipSilencing,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    done: Boolean,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .background(Palette.Panel, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(body, fontSize = 13.sp, color = Palette.Body)
        }
        if (done) {
            Text("On", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Accent, textAlign = TextAlign.End)
        } else {
            if (secondaryAction != null) {
                PillButton(text = secondaryAction, onClick = onSecondaryAction)
            }
            PillButton(text = action, onClick = onAction)
        }
    }
}
