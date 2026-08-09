package com.lifespan.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifespan.app.domain.trends.ChargeTrendBucket
import com.lifespan.app.domain.trends.ChargeTrendRange
import com.lifespan.app.domain.trends.ChargeTrends
import com.lifespan.app.ui.theme.Amber
import com.lifespan.app.ui.theme.Danger
import com.lifespan.app.ui.theme.Ok
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeTrendsScreen(
    trends: ChargeTrends,
    onBack: () -> Unit,
    initialRange: ChargeTrendRange = ChargeTrendRange.DAILY,
) {
    var selectedRange by rememberSaveable { mutableStateOf(initialRange) }
    val buckets = when (selectedRange) {
        ChargeTrendRange.DAILY -> trends.daily
        ChargeTrendRange.WEEKLY -> trends.weekly
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charge trends", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRange == ChargeTrendRange.DAILY,
                        onClick = { selectedRange = ChargeTrendRange.DAILY },
                        label = { Text("Last 7 days") },
                    )
                    FilterChip(
                        selected = selectedRange == ChargeTrendRange.WEEKLY,
                        onClick = { selectedRange = ChargeTrendRange.WEEKLY },
                        label = { Text("Last 4 weeks") },
                    )
                }
            }
            item { TrendSummaryCard(buckets) }
            item {
                TrendChartCard(
                    title = "Sessions",
                    total = buckets.sumOf { it.sessionCount }.toString(),
                    values = buckets.map { it.sessionCount.toFloat() },
                    labels = bucketLabels(buckets, selectedRange),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                TrendChartCard(
                    title = "Charge gained",
                    total = "${buckets.sumOf { it.chargeLevelGained }}%",
                    values = buckets.map { it.chargeLevelGained.toFloat() },
                    labels = bucketLabels(buckets, selectedRange),
                    color = Ok,
                )
            }
            item {
                TrendChartCard(
                    title = "Energy added",
                    total = String.format(Locale.US, "%.0f mAh", buckets.sumOf { it.totalMahAdded }),
                    values = buckets.map { it.totalMahAdded.toFloat() },
                    labels = bucketLabels(buckets, selectedRange),
                    color = Amber,
                )
            }
            item {
                TrendChartCard(
                    title = "Average peak temperature",
                    total = aggregateTemperature(buckets)?.let {
                        String.format(Locale.US, "%.1f°C", it)
                    } ?: "—",
                    values = buckets.map { (it.averagePeakTemperatureCelsius ?: 0.0).toFloat() },
                    labels = bucketLabels(buckets, selectedRange),
                    color = Danger,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TrendSummaryCard(buckets: List<ChargeTrendBucket>) {
    val sessions = buckets.sumOf { it.sessionCount }
    val cycles = buckets.sumOf { it.equivalentCycles }
    val mah = buckets.sumOf { it.totalMahAdded }
    val temperature = aggregateTemperature(buckets)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Range summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TrendMetric("Sessions", sessions.toString())
                TrendMetric("Eq. cycles", String.format(Locale.US, "%.2f", cycles))
                TrendMetric("Added", String.format(Locale.US, "%.0f mAh", mah))
                TrendMetric(
                    "Avg. peak",
                    temperature?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—",
                )
            }
            if (sessions == 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "No completed charge sessions in this range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun TrendMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun TrendChartCard(
    title: String,
    total: String,
    values: List<Float>,
    labels: List<String>,
    color: Color,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    total,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(12.dp))
            BarChart(
                values = values,
                barColor = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

private fun bucketLabels(
    buckets: List<ChargeTrendBucket>,
    range: ChargeTrendRange,
): List<String> {
    val formatter = DateTimeFormatter.ofPattern(
        if (range == ChargeTrendRange.DAILY) "EEE" else "MMM d",
        Locale.US,
    )
    return buckets.map { formatter.format(it.startDate) }
}

private fun aggregateTemperature(buckets: List<ChargeTrendBucket>): Double? {
    val withTemperature = buckets.filter {
        it.sessionCount > 0 && it.averagePeakTemperatureCelsius != null
    }
    val sampleCount = withTemperature.sumOf { it.sessionCount }
    if (sampleCount == 0) return null
    return withTemperature.sumOf {
        checkNotNull(it.averagePeakTemperatureCelsius) * it.sessionCount
    } / sampleCount
}
