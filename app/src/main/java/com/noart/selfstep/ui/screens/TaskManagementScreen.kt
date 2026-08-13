package com.noart.selfstep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.noart.selfstep.model.DisciplineTask
import com.noart.selfstep.model.TaskType

@Composable
fun TaskManagementScreen(
    tasks: List<DisciplineTask>,
    contentPadding: PaddingValues,
    onAddTask: (String, TaskType) -> Unit,
    onUpdateTask: (String, String, TaskType) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DisciplineTask?>(null) }
    var deletingTask by remember { mutableStateOf<DisciplineTask?>(null) }
    val mustDo = tasks.filter { it.type == TaskType.MUST_DO }.sortedBy { it.createdAt }
    val avoid = tasks.filter { it.type == TaskType.AVOID }.sortedBy { it.createdAt }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("任务管理", style = MaterialTheme.typography.headlineMedium)
            Text(
                "这里的任务会自动加入每天的清单。修改和删除不会改动过去的历史记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(
                onClick = {
                    editingTask = null
                    showEditor = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋  添加新任务")
            }
        }

        item { TaskGroupTitle("必须完成", mustDo.size) }
        if (mustDo.isEmpty()) {
            item { EmptyGroupCard("例如：学习、运动、阅读") }
        } else {
            items(mustDo, key = { it.id }) { task ->
                TaskDefinitionCard(
                    task = task,
                    onEdit = {
                        editingTask = task
                        showEditor = true
                    },
                    onDelete = { deletingTask = task }
                )
            }
        }

        item { TaskGroupTitle("禁止事项", avoid.size, Modifier.padding(top = 8.dp)) }
        if (avoid.isEmpty()) {
            item { EmptyGroupCard("例如：玩游戏、刷短视频") }
        } else {
            items(avoid, key = { it.id }) { task ->
                TaskDefinitionCard(
                    task = task,
                    onEdit = {
                        editingTask = task
                        showEditor = true
                    },
                    onDelete = { deletingTask = task }
                )
            }
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            task = editingTask,
            onDismiss = { showEditor = false },
            onSave = { title, type ->
                editingTask?.let { onUpdateTask(it.id, title, type) }
                    ?: onAddTask(title, type)
                showEditor = false
            }
        )
    }

    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            title = { Text("删除任务？") },
            text = { Text("“${task.title}”将不再出现在今天和之后的清单中，过去的记录会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTask(task.id)
                    deletingTask = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTask = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TaskGroupTitle(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text("$count 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TaskDefinitionCard(task: DisciplineTask, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAvoid = task.type == TaskType.AVOID
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvoid) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 17.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (isAvoid) "每天确认今天已守住" else "每天完成后打勾",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onEdit) { Text("修改") }
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EmptyGroupCard(hint: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            hint,
            modifier = Modifier.padding(17.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskEditorDialog(
    task: DisciplineTask?,
    onDismiss: () -> Unit,
    onSave: (String, TaskType) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var type by remember(task?.id) { mutableStateOf(task?.type ?: TaskType.MUST_DO) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "添加任务" else "修改任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("任务名称") },
                    placeholder = { Text("例如：阅读 30 分钟") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canSave) {
                            focusManager.clearFocus()
                            onSave(title, type)
                        }
                    }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                Text("任务类型", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = type == TaskType.MUST_DO,
                        onClick = { type = TaskType.MUST_DO },
                        label = { Text("必须完成") }
                    )
                    FilterChip(
                        selected = type == TaskType.AVOID,
                        onClick = { type = TaskType.AVOID },
                        label = { Text("禁止事项") }
                    )
                }
                Text(
                    if (type == TaskType.AVOID) {
                        "当天没有做这件事时打勾，表示今天守住了。"
                    } else {
                        "当天完成这件事后打勾。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onSave(title, type) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
