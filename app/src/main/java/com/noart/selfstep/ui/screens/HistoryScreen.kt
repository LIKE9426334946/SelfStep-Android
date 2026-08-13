package com.noart.selfstep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noart.selfstep.SelfStepUiState
import com.noart.selfstep.model.DailyRecord
import com.noart.selfstep.model.MetricsCalculator
import com.noart.selfstep.model.TaskType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(state: SelfStepUiState, contentPadding: PaddingValues) {
    val metrics = MetricsCalculator.calculate(state.data.records, state.today)
    val records = state.data.records.values.sortedByDescending { it.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("自律记录", style = MaterialTheme.typography.headlineMedium)
            Text(
                "每一个完成的日子，都会留在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MetricCard("当前连续", "${metrics.currentStreak} 天", Modifier.weight(1f))
                MetricCard("最长连续", "${metrics.bestStreak} 天", Modifier.weight(1f))
                MetricCard("达标天数", "${metrics.successfulDays} 天", Modifier.weight(1f))
            }
        }

        if (records.all { it.tasks.isEmpty() }) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "添加任务并完成一天后，这里会显示详细历史记录。",
                        modifier = Modifier.padding(22.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    "每日明细",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            items(records.filter { it.tasks.isNotEmpty() }, key = { it.date }) { record ->
                HistoryRecordCard(record)
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 14.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun HistoryRecordCard(record: DailyRecord) {
    val successColor = MaterialTheme.colorScheme.primary
    val pendingColor = MaterialTheme.colorScheme.error
    val accent = if (record.isSuccessful) successColor else pendingColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .background(accent)
                    .fillMaxHeight()
                    .width(6.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(formatRecordDate(record.date), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "完成 ${record.completedCount} / ${record.tasks.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (record.isSuccessful) "已达标" else "未达标",
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                record.tasks.forEach { task ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (task.completed) successColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            if (task.completed) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.padding(start = 9.dp)) {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (task.type == TaskType.AVOID) "禁止事项" else "必须完成",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRecordDate(value: String): String = runCatching {
    LocalDate.parse(value).format(
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    )
}.getOrDefault(value)
