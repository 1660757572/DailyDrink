package com.aquaday.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aquaday.app.data.HydrationRepository
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 通知按钮与闹钟统一入口：确认喝水、稍后提醒、到点触发。 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        AquaChannels.ensure(appContext)
        val repository = HydrationRepository(appContext)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                Log.i(TAG, "onReceive: ${intent.action}")
                when (intent.action) {
                    ACTION_FIRE -> repository.onReminderDue()
                    ACTION_CONFIRM -> repository.confirmDrink(
                        amountMl = intent.getIntExtra(EXTRA_AMOUNT, DEFAULT_AMOUNT_ML),
                        typeId = intent.getStringExtra(EXTRA_TYPE_ID) ?: "water",
                    )

                    ACTION_SNOOZE -> repository.snoozeReminder(intent.getIntExtra(EXTRA_MINUTES, DEFAULT_SNOOZE_MIN))
                    else -> Log.w(TAG, "onReceive: 未识别的动作 ${intent.action}")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "onReceive 处理失败: ${intent.action} - ${error.message}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderActionReceiver"
        const val ACTION_FIRE = "com.aquaday.app.action.FIRE_REMINDER"
        const val ACTION_CONFIRM = "com.aquaday.app.action.CONFIRM_DRINK"
        const val ACTION_SNOOZE = "com.aquaday.app.action.SNOOZE"
        const val EXTRA_AMOUNT = "extra_amount_ml"
        const val EXTRA_TYPE_ID = "extra_drink_type_id"
        const val EXTRA_MINUTES = "extra_minutes"
        const val DEFAULT_AMOUNT_ML = 200
        const val DEFAULT_SNOOZE_MIN = 10
    }
}
