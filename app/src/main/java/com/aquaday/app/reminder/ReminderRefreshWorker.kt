package com.aquaday.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aquaday.app.data.HydrationRepository
import java.util.concurrent.TimeUnit

/** 周期性兜底：即使闹钟被系统回收，也能重建提醒并刷新实时活动。 */
class ReminderRefreshWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        HydrationRepository(applicationContext).restore()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "aqua_reminder_refresh"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderRefreshWorker>(4, TimeUnit.HOURS).build(),
            )
        }
    }
}
