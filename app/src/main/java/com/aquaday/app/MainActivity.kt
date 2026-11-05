package com.aquaday.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aquaday.app.ui.AquaApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 通知权限由引导页在「权限」这一步申请，避免系统弹窗抢在用户了解用途之前出现。
        setContent { AquaApp() }
    }
}
