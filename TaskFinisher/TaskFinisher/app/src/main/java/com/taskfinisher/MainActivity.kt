package com.taskfinisher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.taskfinisher.notifications.ReminderWorker
import com.taskfinisher.ui.navigation.AppNavGraph
import com.taskfinisher.ui.theme.TaskFinisherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule a default daily reminder at 9:00 AM on first run
        // In a production app, expose this time in Settings
        ReminderWorker.scheduleDailyReminder(this, hourOfDay = 9, minute = 0)

        setContent {
            TaskFinisherTheme {
                AppNavGraph()
            }
        }
    }
}
