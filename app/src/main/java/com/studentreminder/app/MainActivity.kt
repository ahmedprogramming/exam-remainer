package com.studentreminder.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentreminder.app.data.AppSettings
import com.studentreminder.app.reminders.NotificationHelper
import com.studentreminder.app.ui.StudentReminderRoot
import com.studentreminder.app.ui.theme.StudentReminderTheme

class MainActivity : ComponentActivity() {

    /** Route requested by a tapped notification, consumed by the nav host. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_ROUTE)

        val container = (application as StudentReminderApp).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            StudentReminderTheme(settings = settings) {
                StudentReminderRoot(
                    container = container,
                    settings = settings,
                    pendingRoute = pendingRoute,
                    onRouteConsumed = { pendingRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_OPEN_ROUTE)?.let { pendingRoute = it }
    }
}
