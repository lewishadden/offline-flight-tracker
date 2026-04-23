package com.lewishadden.flighttracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
// Color is used by BrandChip's `color: Color = ...` parameter.
import androidx.compose.ui.unit.dp
/**
 * Full-bleed gradient background used as the root of branded screens.
 *
 * Sources its colors from the active MaterialTheme so it switches cleanly
 * between dark and light brand palettes (see Theme.kt).
 */
@Composable
fun BrandBackground(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to cs.background,
                    0.55f to cs.surface,
                    1f to cs.surfaceContainerLow,
                )
            )
    ) {
        content()
    }
}

/** Elevated card with a subtle gradient, used throughout the detail screen. */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    0f to cs.surfaceContainer,
                    1f to cs.surfaceContainerLow,
                )
            )
            .border(1.dp, cs.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        // Force the theme's onSurface as the default content color inside the
        // card so no nested Text falls back to a dimmer LocalContentColor from
        // an outer Surface override.
        CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
            content()
        }
    }
}

/** Small uppercase section header used above groups of fields. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** Key-value pair rendered on a single line. */
@Composable
fun KvRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

/** Pill-shaped status chip. */
@Composable
fun BrandChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(100))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * Brand-styled switch with a clearly visible "off" state.
 *
 * Material 3's default unchecked Switch uses `outline` for the track and
 * `outlineVariant` for the thumb — both very low-contrast on dark surfaces, so
 * an off-but-active toggle reads identically to a disabled one. This styling
 * gives "off" a luminous outlined thumb on a dim filled track that visually
 * registers as a real toggle that just isn't currently on.
 */
@Composable
fun BrandSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/** Empty-state placeholder used across list screens. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
