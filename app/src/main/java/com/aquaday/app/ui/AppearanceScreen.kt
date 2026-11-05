package com.aquaday.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AquaSegmented
import com.aquaday.app.ui.components.AquaSwitchRow
import com.aquaday.app.ui.components.AquaTileShape

@Composable
fun AppearanceScreen(state: AquaUiState, viewModel: AquaViewModel) {
    val snapshot = state.metrics?.snapshot ?: return
    val imagePath = snapshot.backgroundImage
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setBackgroundImage(uri)
    }

    AquaSectionTitle(stringResource(R.string.appearance_section_mode))
    AquaCard {
        Column {
            AquaSwitchRow(
                title = stringResource(R.string.setting_theme),
                summary = stringResource(R.string.mine_appearance_hint),
                checked = snapshot.themeMode == ThemeMode.Dark,
                icon = Icons.Filled.DarkMode,
                iconTint = TintTheme,
                onCheckedChange = { dark ->
                    viewModel.setThemeMode(
                        if (dark) ThemeMode.Dark else ThemeMode.Light,
                    )
                },
            )
            Spacer(Modifier.height(6.dp))
            val modes = listOf(
                stringResource(R.string.setting_theme_system),
                stringResource(R.string.setting_theme_light),
                stringResource(R.string.setting_theme_dark),
            )
            AquaSegmented(
                options = modes,
                selectedIndex = when (snapshot.themeMode) {
                    ThemeMode.System -> 0
                    ThemeMode.Light -> 1
                    ThemeMode.Dark -> 2
                },
                onSelect = { index ->
                    viewModel.setThemeMode(listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark)[index])
                },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.appearance_section_color))
    AquaCard {
        AquaSwitchRow(
            title = stringResource(R.string.setting_dynamic_color),
            summary = stringResource(R.string.setting_dynamic_color_summary),
            checked = snapshot.dynamicColor,
            icon = Icons.Filled.Palette,
            iconTint = TintTheme,
            onCheckedChange = { viewModel.setDynamicColor(it) },
        )
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.appearance_section_background))
    AquaCard {
        Column {
            BackgroundPreview(imagePath)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AquaChip(
                    text = stringResource(R.string.action_choose_image),
                    selected = true,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    leading = Icons.Filled.Wallpaper,
                    modifier = Modifier.weight(1f),
                )
                AquaChip(
                    text = stringResource(R.string.action_remove_image),
                    selected = false,
                    enabled = imagePath != null,
                    onClick = { viewModel.setBackgroundImage(null) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setting_background_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackgroundPreview(path: String?) {
    val image = path?.let { rememberBackgroundBitmap(it) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(AquaTileShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), AquaTileShape),
        contentAlignment = Alignment.Center,
    ) {
        if (image == null) {
            Text(
                text = stringResource(R.string.background_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
            )
        }
    }
}
