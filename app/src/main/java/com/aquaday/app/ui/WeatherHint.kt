package com.aquaday.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aquaday.app.R
import com.aquaday.app.data.WeatherInfo

private const val DRY_HUMIDITY = 35
private const val HOT_CELSIUS = 30

/** 问候语第二行：天气状况 + 需要多补水的提示（只在干或热时出现）。 */
@Composable
internal fun weatherHintLines(info: WeatherInfo): List<String> {
    val main = stringResource(
        R.string.home_greeting_weather,
        stringResource(conditionRes(info.weatherCode)),
        info.temperatureC,
        info.humidity,
    )
    val hint = when {
        info.humidity in 1..DRY_HUMIDITY -> stringResource(R.string.home_hint_dry)
        info.temperatureC >= HOT_CELSIUS -> stringResource(R.string.home_hint_hot)
        else -> null
    }
    return if (hint == null) listOf(main) else listOf(main, hint)
}

/** 未识别的天气码归到「多云」，避免轮播里出现空文案。 */
@StringRes
private fun conditionRes(code: Int): Int = when (code) {
    0 -> R.string.weather_clear
    1, 2, 3 -> R.string.weather_cloudy
    45, 48 -> R.string.weather_fog
    51, 53, 55, 56, 57 -> R.string.weather_drizzle
    61, 63, 65, 66, 67, 80, 81, 82 -> R.string.weather_rain
    71, 73, 75, 77, 85, 86 -> R.string.weather_snow
    95, 96, 99 -> R.string.weather_thunder
    else -> R.string.weather_cloudy
}
