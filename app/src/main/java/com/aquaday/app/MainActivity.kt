package com.aquaday.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aquaday.app.ui.AquaApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val quickLog = MutableStateFlow<QuickLogRequest?>(null)
    private var requestId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeQuickLog(intent)
        // 通知权限由引导页在「权限」这一步申请，避免系统弹窗抢在用户了解用途之前出现。
        setContent { AquaApp(quickLog = quickLog) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeQuickLog(intent)
    }

    /** 快速记录走「启动/唤醒界面 + 一次性事件」，记录本身仍由仓库统一落盘。 */
    private fun consumeQuickLog(intent: Intent?) {
        if (intent?.action != QuickActions.ACTION_QUICK_LOG) return
        val amountMl = intent.getIntExtra(QuickActions.EXTRA_QUICK_ML, QuickActions.DEFAULT_CUP)
        // 动作消费掉，避免同一次启动因重建被重复记录。
        intent.action = null
        quickLog.value = QuickLogRequest(id = ++requestId, amountMl = amountMl)
    }
}
