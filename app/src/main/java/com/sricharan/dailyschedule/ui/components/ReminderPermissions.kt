package com.sricharan.dailyschedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sricharan.dailyschedule.notifications.Reminders

/**
 * What the system is currently letting this app do about reminders.
 *
 * Both answers can change while the app is in the background — granting them
 * means walking off to system settings and coming back — so this re-reads them
 * on every resume rather than trusting a value captured once.
 */
data class ReminderPermissions(
    val canNotify: Boolean,
    val canBeExact: Boolean
) {
    val allGood: Boolean get() = canNotify && canBeExact
}

@Composable
fun rememberReminderPermissions(): ReminderPermissions {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeCount by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(resumeCount) {
        ReminderPermissions(
            canNotify = Reminders.canPostNotifications(context),
            canBeExact = Reminders.canScheduleExactAlarms(context)
        )
    }
}
