package com.aquaday.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.NoDrinks
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 饮品图标目录：key 落盘到 DrinkType.iconKey，新增只追加不改名，旧数据按 fallback 渲染。
 * 全部来自 material-icons-extended。
 */
internal object DrinkIcons {
    val catalog: List<Pair<String, ImageVector>> = listOf(
        "water" to Icons.Filled.Water,
        "drop" to Icons.Filled.WaterDrop,
        "damage" to Icons.Filled.WaterDamage,
        "coffee" to Icons.Filled.Coffee,
        "coffeeMaker" to Icons.Filled.CoffeeMaker,
        "cafe" to Icons.Filled.LocalCafe,
        "soup" to Icons.Filled.SoupKitchen,
        "room" to Icons.Filled.RoomService,
        "drink" to Icons.Filled.LocalDrink,
        "icecream" to Icons.Filled.Icecream,
        "bakery" to Icons.Filled.BakeryDining,
        "dinner" to Icons.Filled.DinnerDining,
        "lunch" to Icons.Filled.LunchDining,
        "rice" to Icons.Filled.RiceBowl,
        "bar" to Icons.Filled.LocalBar,
        "liquor" to Icons.Filled.Liquor,
        "wine" to Icons.Filled.WineBar,
        "sports" to Icons.Filled.SportsBar,
        "pizza" to Icons.Filled.LocalPizza,
        "restaurant" to Icons.Filled.Restaurant,
        "table" to Icons.Filled.TableRestaurant,
        "grocery" to Icons.Filled.LocalGroceryStore,
        "park" to Icons.Filled.Park,
        "noDrinks" to Icons.Filled.NoDrinks,
    )

    const val FALLBACK_KEY = "drop"

    fun vector(key: String): ImageVector =
        catalog.firstOrNull { it.first == key }?.second ?: Icons.Filled.WaterDrop
}

/** 类型图标的着色：自定义色优先，否则跟随强调色。argb 为 null 时返回 accent。 */
internal fun drinkIconTint(tintArgb: Int?, accent: Color): Color = tintArgb?.let { Color(it) } ?: accent
