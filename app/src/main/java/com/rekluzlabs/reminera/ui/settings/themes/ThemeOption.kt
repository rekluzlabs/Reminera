package com.rekluzlabs.reminera.ui.settings.themes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.reminera.R
import com.rekluzlabs.reminera.ui.theme.CinnamonPrimary
import com.rekluzlabs.reminera.ui.theme.MutedClay
import com.rekluzlabs.reminera.ui.theme.OlivePrimary
import com.rekluzlabs.reminera.ui.theme.RosePrimary

@Composable
fun ThemeOption(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = mode.displayName
    val (description, previewColor) = when (mode) {
        ThemeMode.LIGHT -> "A fresh, open canvas" to Color(0xFFF5F5F5)
        ThemeMode.DARK -> "Subdued tones for focused evenings" to Color(0xFF1C1B1F)
        ThemeMode.AMOLED_BLACK -> "True black for OLED screens" to Color.Black
        ThemeMode.WARM_TERRACOTTA -> "Warm, hand-thrown clay tones" to MutedClay
        ThemeMode.CINNAMON_CREAM -> "Soft, time-worn paper tones" to CinnamonPrimary
        ThemeMode.DUSTY_ROSE_COPPER -> "Muted rose warmed by copper accents" to RosePrimary
        ThemeMode.OLIVE_BRASS -> "Earthy olive grounded by aged brass" to OlivePrimary
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, previewColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_theme_book),
                    contentDescription = null,
                    tint = previewColor,
                    modifier = Modifier.size(34.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
