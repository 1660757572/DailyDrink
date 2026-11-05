package com.aquaday.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquaday.app.R

/** 头像为空时用主题渐变底 + 昵称首字兜底，保证任何阶段都有可识别的形象。 */
@Composable
fun AquaAvatar(
    path: String?,
    nickname: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val bitmap = path?.let { rememberBackgroundBitmap(it) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (bitmap == null) {
                    Modifier.background(
                        Brush.linearGradient(listOf(Color(0xFF7FDCFF), Color(0xFF2E86FF))),
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.profile_avatar_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (nickname.isNotBlank()) {
            Text(
                text = nickname.trim().take(1),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (size / 2.2f).value.sp,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(R.string.profile_avatar_description),
                modifier = Modifier.size(size * 0.52f),
                tint = Color.White,
            )
        }
    }
}

/** 未设置昵称时给出的占位名，与「水杯」品牌保持一致。 */
@Composable
fun profileDisplayName(nickname: String): String =
    nickname.ifBlank { stringResource(R.string.profile_default_name) }
