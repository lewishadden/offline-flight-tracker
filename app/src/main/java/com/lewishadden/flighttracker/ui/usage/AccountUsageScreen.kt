package com.lewishadden.flighttracker.ui.usage

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lewishadden.flighttracker.data.api.dto.AccountUsageDto
import com.lewishadden.flighttracker.ui.common.BrandBackground
import com.lewishadden.flighttracker.ui.common.BrandCard
import com.lewishadden.flighttracker.ui.common.KvRow
import com.lewishadden.flighttracker.ui.common.SectionHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountUsageScreen(vm: AccountUsageViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    BrandBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("API usage") },
                    actions = {
                        IconButton(onClick = vm::refresh) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
                when {
                    ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    ui.error != null -> Text(
                        ui.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                    ui.data != null -> UsageContent(ui.data!!)
                }
            }
        }
    }
}

@Composable
private fun UsageContent(data: AccountUsageDto) {
    val total = data.totalCost ?: 0.0
    val cap = data.planCap
    val pct = if (cap != null && cap > 0) (total / cap).coerceIn(0.0, 1.0) else null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(8.dp))
        BrandCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                SectionHeader("This billing period")
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "\$%.2f".format(total),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    cap?.let {
                        Text(
                            "/ \$%.2f".format(it),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
                if (pct != null) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { pct.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (pct > 0.85) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(pct * 100).toInt()}% of plan cap used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        BrandCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionHeader("Details")
                KvRow("Period start", formatPeriod(data.periodStart))
                KvRow("Period end", formatPeriod(data.periodEnd))
                KvRow(
                    "Overage",
                    if (data.planOverageEnabled == true) "Enabled" else "Disabled",
                )
                cap?.let {
                    val remaining = (it - total).coerceAtLeast(0.0)
                    KvRow("Remaining", "\$%.2f".format(remaining), highlight = true)
                }
            }
        }

        Text(
            "Per-poll cost is about \$0.005 for a subscribed flight's refresh — your polling is covered by the Personal tier's \$5/month free allowance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private fun formatPeriod(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    return DateTimeFormatter.ofPattern("d MMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
