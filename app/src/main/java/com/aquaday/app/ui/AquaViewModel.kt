package com.aquaday.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aquaday.app.data.AchievementProgress
import com.aquaday.app.data.Achievements
import com.aquaday.app.data.AquaMetrics
import com.aquaday.app.data.AquaPermissions
import com.aquaday.app.data.CalendarCell
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.data.DrinkEvent
import com.aquaday.app.data.DrinkType
import com.aquaday.app.data.HydrationMetrics
import com.aquaday.app.data.HydrationRepository
import com.aquaday.app.data.ReminderSettings
import com.aquaday.app.data.ThemeMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AquaUiState(
    val metrics: AquaMetrics? = null,
    val achievements: List<AchievementProgress> = emptyList(),
    val calendar: List<CalendarCell> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val permissions: AquaPermissions = AquaPermissions(notifications = false, exactAlarms = false),
    val nextReminderAt: LocalDateTime? = null,
    /** 由心跳推进的当前时间，让"距上次活动"和下次提醒随时间走动。 */
    val now: LocalDateTime = LocalDateTime.now(),
    val message: String? = null,
)

/** 界面唯一状态源：所有写入都回到同一份快照，再由仓库同步成就、实时活动与闹钟。 */
class AquaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HydrationRepository(application)
    private val _state = MutableStateFlow(AquaUiState())
    val state: StateFlow<AquaUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.metrics.collect { metrics ->
                val fresh = repository.synchronize(metrics)
                _state.update { previous ->
                    previous.copy(
                        metrics = metrics,
                        achievements = Achievements.evaluate(metrics),
                        calendar = monthCells(metrics, previous.month),
                        permissions = repository.permissions(),
                        nextReminderAt = repository.nextReminderAt(metrics),
                        message = fresh.lastOrNull()?.let { "解锁成就 · ${it.achievement.title}" } ?: previous.message,
                    )
                }
            }
        }
        // 下一次提醒只在数据变化时算一次会停在旧时间，心跳保证分钟级刷新。
        viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val now = LocalDateTime.now()
                _state.update { previous ->
                    previous.copy(
                        now = now,
                        nextReminderAt = previous.metrics?.let { repository.nextReminderAt(it) },
                    )
                }
            }
        }
    }

    fun addDrink(
        amountMl: Int,
        type: DrinkType = DrinkType.WATER,
        at: LocalDateTime = LocalDateTime.now(),
    ) = viewModelScope.launch {
        repository.logDrink(amountMl, type, at)
    }

    fun addDrinkType(label: String, onAdded: (DrinkType) -> Unit = {}) = viewModelScope.launch {
        repository.addDrinkType(label)?.let(onAdded)
    }

    fun deleteEvent(id: Long) = viewModelScope.launch { repository.deleteEvent(id) }

    fun setGoal(goalMl: Int) = viewModelScope.launch { repository.setGoal(goalMl) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setColorStyle(style: ColorStyle) = viewModelScope.launch { repository.setColorStyle(style) }

    fun setCustomAccent(argb: Int) = viewModelScope.launch { repository.setCustomAccent(argb) }

    fun setBackgroundImage(uri: android.net.Uri?) = viewModelScope.launch { repository.setBackgroundImage(uri) }

    fun setBackgroundScrim(percent: Int) = viewModelScope.launch { repository.setBackgroundScrim(percent) }

    fun setBackgroundBlur(dp: Int) = viewModelScope.launch { repository.setBackgroundBlur(dp) }

    fun setAvatarImage(uri: android.net.Uri?) = viewModelScope.launch { repository.setAvatarImage(uri) }

    fun setNickname(nickname: String) = viewModelScope.launch { repository.setNickname(nickname) }

    fun completeOnboarding() = viewModelScope.launch { repository.completeOnboarding() }

    fun updateSettings(transform: (ReminderSettings) -> ReminderSettings) = viewModelScope.launch {
        repository.updateSettings(transform)
    }

    fun resetAll() = viewModelScope.launch {
        repository.resetAll()
        _state.update { it.copy(month = YearMonth.now(), selectedDate = LocalDate.now(), message = "已清空全部记录") }
    }

    fun showMonth(month: YearMonth) = viewModelScope.launch {
        _state.update { it.copy(month = month, calendar = monthCells(it.metrics, month)) }
    }

    fun shiftMonth(deltaMonths: Long) {
        val current = _state.value.month
        showMonth(current.plusMonths(deltaMonths))
    }

    fun selectDate(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    fun refreshPermissions() = _state.update { it.copy(permissions = repository.permissions()) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun monthCells(metrics: AquaMetrics?, month: YearMonth): List<CalendarCell> {
        val snapshot = metrics?.snapshot ?: return emptyList()
        val byDay = HydrationMetrics.dayRecords(snapshot.events, snapshot.goalMl)
        return HydrationMetrics.monthGrid(month, byDay, LocalDate.now())
    }

    fun eventsOn(date: LocalDate): List<DrinkEvent> =
        _state.value.metrics?.snapshot?.events.orEmpty().filter { it.at.toLocalDate() == date }

    private companion object {
        const val TICK_MS = 15_000L
    }
}
