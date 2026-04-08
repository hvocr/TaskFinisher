package com.taskfinisher.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.taskfinisher.R
import com.taskfinisher.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        private const val DAILY_NOTIF_ID = 8001

        fun scheduleDailyReminder(context: Context, hourOfDay: Int, minute: Int) {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= now) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val delayMs = calendar.timeInMillis - now
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_HOUR to hourOfDay, KEY_MINUTE to minute))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }

        fun cancelDailyReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val tasks = repository.getTodayTasks().first()
            val pending = tasks.filter { !it.isCompleted }
            if (pending.isNotEmpty()) showDailySummaryNotification(pending.size)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showDailySummaryNotification(count: Int) {
        val text = if (count == 1) "You have 1 task today" else "You have $count tasks today"
        val notification = NotificationCompat.Builder(
            applicationContext, NotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Reminder")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(DAILY_NOTIF_ID, notification)
    }
}
