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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lewishadden.flighttracker.ui.theme.Brand

/** Full-bleed gradient background used as the root of branded screens. */
@Composable
fun BrandBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Brand.IndigoDeep,
                    0.55f to Brand.Surface,
                    1f to Brand.SurfaceLo,
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    0f to Color(0xFF1A2247),
                    1f to Color(0xFF111732),
                )
            )
            .border(1.dp, Brand.Outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        // Force a light default content color inside the card so no nested Text
        // falls back to a dark "on-surface" value from a parent Surface.
        CompositionLocalProvider(LocalContentColor provides Brand.OnSurface) {
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
