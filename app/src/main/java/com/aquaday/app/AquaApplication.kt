package com.aquaday.app

import android.app.Application
import com.aquaday.app.reminder.AquaChannels
import com.aquaday.app.reminder.ReminderRefreshWorker
import com.itos.coui.material3.CouiMaterial3Style
import com.itos.coui.theme.Coui

class AquaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // COUI 组件在消费处读取宿主 MaterialTheme，进程内只需装配一次。
        Coui.initialize(CouiMaterial3Style())
        AquaChannels.ensure(this)
        // 进程重启后闹钟可能已随系统失效，用周期任务兜底重建。
        ReminderRefreshWorker.enqueue(this)
    }
}
