package com.aquaday.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.aquaday.app.R
import com.aquaday.app.data.HydrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 小组件与仓库同源：启动、定时、尺寸变化时都重新读一次最新快照。 */
abstract class BaseAquaWidgetProvider(private val layoutRes: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        refresh(context, manager, intArrayOf(id))
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val metrics = HydrationRepository(context).currentMetrics()
                AquaWidgets.render(context, manager, ids, metrics, layoutRes)
            } catch (failure: Throwable) {
                Log.w(TAG, "refresh: 小组件刷新失败", failure)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AquaWidget"
    }
}

class AquaWidgetSmallProvider : BaseAquaWidgetProvider(R.layout.widget_aqua_small)

class AquaWidgetWideProvider : BaseAquaWidgetProvider(R.layout.widget_aqua_wide)
