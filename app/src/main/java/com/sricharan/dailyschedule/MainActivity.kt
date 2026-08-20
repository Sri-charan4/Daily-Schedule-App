package com.sricharan.dailyschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.sricharan.dailyschedule.notifications.ReminderSync
import com.sricharan.dailyschedule.notifications.Reminders
import com.sricharan.dailyschedule.ui.AppNavigation
import com.sricharan.dailyschedule.ui.theme.DailyScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Reminders.ensureChannel(this)

        // A safety net rather than the main mechanism. Alarms are normally kept
        // current as items are edited and rebuilt by BootReceiver, but a
        // force-stop or a killed background process can lose them silently and
        // nothing else would notice.
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { ReminderSync.syncAll(applicationContext) }
        }

        setContent {
            DailyScheduleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}
