package com.noart.selfstep.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noart.selfstep.SelfStepUiState
import com.noart.selfstep.model.DailyTaskStatus
import com.noart.selfstep.model.MetricsCalculator
import com.noart.selfstep.model.TaskType
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    state: SelfStepUiState,
    contentPadding: PaddingValues,
    onToggleTask: (String) -> Unit,
    onOpenTaskManagement: () -> Unit
) {
    val record = state.todayRecord
    val mustDo = record.tasks.filter { it.type == TaskType.MUST_DO }
    val avoid = record.tasks.filter { it.type == TaskType.AVOID }
    val metrics = MetricsCalculator.calculate(state.data.records, state.today)

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
            Text(
                text = "今天，也向前一步",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = state.today.format(
                    DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            TodaySummaryCard(
                completed = record.completedCount,
                total = record.tasks.size,
                streak = metrics.currentStreak,
                successful = record.isSuccessful
            )
        }

        if (record.tasks.isEmpty()) {
            item { EmptyTodayCard(onOpenTaskManagement) }
        } else {
            item { SectionTitle("必须完成", "完成后打勾") }
            if (mustDo.isEmpty()) {
                item { SectionEmptyText("还没有必须完成的事情") }
            } else {
                items(mustDo, key = { it.taskId }) { task ->
                    DailyTaskCard(task = task, onToggle = { onToggleTask(task.taskId) })
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle("禁止事项", "今天守住了就打勾")
            }
            if (avoid.isEmpty()) {
                item { SectionEmptyText("还没有禁止做的事情") }
            } else {
                items(avoid, key = { it.taskId }) { task ->
                    DailyTaskCard(task = task, onToggle = { onToggleTask(task.taskId) })
                }
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(completed: Int, total: Int, streak: Int, successful: Boolean) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF176B52), Color(0xFF2A8B68))
                )
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (successful) "今日目标已达成" else "今日完成进度",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$completed / $total",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("连续", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                        Text("$streak 天", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Text(
                text = when {
                    total == 0 -> "先添加今天要坚持的目标"
                    successful -> "做得很好，今天的每一步都算数。"
                    else -> "完成全部项目后，今天会计入连续自律天数。"
                },
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyTaskCard(task: DailyTaskStatus, onToggle: () -> Unit) {
    val isAvoid = task.type == TaskType.AVOID
    val container = if (isAvoid) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    }
    val accent = if (isAvoid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        task.completed && isAvoid -> "今天已守住"
                        task.completed -> "今天已完成"
                        isAvoid -> "没有做这件事后再打勾"
                        else -> "完成后打勾"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
            }
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun EmptyTodayCard(onOpenTaskManagement: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("第一步，从一个小目标开始", style = MaterialTheme.typography.titleLarge)
            Text(
                "添加必须完成或禁止做的事情，它们会自动出现在每天的首页。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
            )
            Button(onClick = onOpenTaskManagement) { Text("添加任务") }
        }
    }
}

@Composable
private fun SectionEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
