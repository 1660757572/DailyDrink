package com.aquaday.app.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.DrinkType
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.MenuBackdropWhileOpen
import com.aquaday.app.ui.components.bleedHorizontal
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.chip.ChipTabButton
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor

/**
 * 饮品类型筛选：首项「全部」，其后每类一片，图标取自 DrinkIcons。
 * 高版本走徽章墙同款 ChipTabRow + 前置菜单按钮，低版本退回普通胶囊。
 */
@Composable
internal fun DrinkTypeFilter(
    types: List<DrinkType>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onEditType: (DrinkType) -> Unit,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    val allLabel = stringResource(R.string.medal_filter_all)
    val labels = remember(types, allLabel) { listOf(allLabel) + types.map { it.label } }
    val selectedIndex = if (selectedId == null) 0 else types.indexOfFirst { it.id == selectedId } + 1
    fun pick(index: Int) = onSelect(if (index <= 0) null else types.getOrNull(index - 1)?.id)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        TypeTabsApi34(labels, types, selectedIndex, ::pick, backdrop, dark)
    } else {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AquaChip(text = labels.first(), selected = selectedIndex == 0, onClick = { pick(0) })
            types.forEach { type ->
                AquaChip(
                    text = type.label,
                    selected = type.id == selectedId,
                    onClick = { pick(types.indexOf(type) + 1) },
                    leading = DrinkIcons.vector(type.iconKey),
                    leadingTint = type.tintArgb?.let { Color(it) },
                    onLongClick = { onEditType(type) },
                )
            }
        }
    }
}

@RequiresApi(34)
@Composable
private fun TypeTabsApi34(
    labels: List<String>,
    types: List<DrinkType>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    val items = remember(labels, types) {
        labels.mapIndexed { index, label ->
            val tint = types.getOrNull(index - 1)?.tintArgb?.let { Color(it) }
            CouiChipTabItem(
                key = index,
                label = label,
                icon = if (index == 0) null else {
                    {
                        Icon(
                            imageVector = DrinkIcons.vector(types[index - 1].iconKey),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = tint ?: Color.Unspecified,
                        )
                    }
                },
            )
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val anchor = rememberCouiMenuAnchor()
    val menuItems = remember(labels, selectedIndex) {
        labels.mapIndexed { index, label -> CouiMenuItem(index, label, selected = index == selectedIndex) }
    }
    MenuBackdropWhileOpen(menuOpen)
    Menu(
        expanded = menuOpen,
        onDismissRequest = { menuOpen = false },
        items = menuItems,
        onItemClick = { item ->
            onSelect(item.value)
            menuOpen = false
        },
        anchor = anchor,
    )
    ChipTabRow(
        modifier = Modifier.fillMaxWidth().bleedHorizontal(),
        items = items,
        selectedKey = selectedIndex,
        onSelectedKeyChange = { onSelect(it as Int) },
        backdrop = backdrop,
        dark = dark,
        itemSpacing = 6.dp,
        leadingAction = {
            ChipTabButton(
                modifier = Modifier.couiMenuAnchor(anchor),
                onClick = { anchor.open { menuOpen = true } },
                contentDescription = stringResource(R.string.drink_filter_menu),
                backdrop = backdrop,
                dark = dark,
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}
