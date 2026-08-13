package com.noart.selfstep.ui.screens

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noart.selfstep.SelfStepUiState
import com.noart.selfstep.data.BackupOperation
import com.noart.selfstep.model.DailyRecord
import com.noart.selfstep.model.MetricsCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    state: SelfStepUiState,
    contentPadding: PaddingValues,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onBackupDirectorySelected: (Uri, BackupOperation) -> Unit
) {
    val metrics = MetricsCalculator.calculate(state.data.records, state.today)
    val records = state.data.records.values.sortedByDescending { it.date }
    var pendingOperation by remember { mutableStateOf<BackupOperation?>(null) }
    var showDirectoryInstructions by remember { mutableStateOf(false) }
    var showImportConfirmation by remember { mutableStateOf(false) }
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val operation = pendingOperation
        pendingOperation = null
        if (treeUri != null && operation != null) {
            onBackupDirectorySelected(treeUri, operation)
        }
    }

    fun startOperation(operation: BackupOperation) {
        if (state.backupDirectoryReady) {
            when (operation) {
                BackupOperation.IMPORT -> onImportData()
                BackupOperation.EXPORT -> onExportData()
            }
        } else {
            pendingOperation = operation
            showDirectoryInstructions = true
        }
    }

    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { showImportConfirmation = false },
            title = { Text("导入备份？") },
            text = {
                Text("导入会使用 Documents/SelfStep/selfstep_data.json 覆盖当前的全部任务和历史记录。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirmation = false
                        startOperation(BackupOperation.IMPORT)
                    }
                ) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDirectoryInstructions) {
        AlertDialog(
            onDismissRequest = {
                showDirectoryInstructions = false
                pendingOperation = null
            },
            title = { Text("授权 Documents 文件夹") },
            text = {
                Text("首次使用需要选择手机内部存储中的 Documents 文件夹，然后点击“使用此文件夹”。SelfStep 子文件夹会自动创建。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDirectoryInstructions = false
                        directoryPicker.launch(documentsInitialUri())
                    }
                ) {
                    Text("去选择")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDirectoryInstructions = false
                        pendingOperation = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自律记录", style = MaterialTheme.typography.headlineMedium)
                if (state.backupInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(22.dp).height(22.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row {
                        TextButton(onClick = { showImportConfirmation = true }) {
                            Text("导入")
                        }
                        TextButton(onClick = { startOperation(BackupOperation.EXPORT) }) {
                            Text("导出")
                        }
                    }
                }
            }
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
                        "添加任务并完成一天后，这里会显示每日完成进度。",
                        modifier = Modifier.padding(22.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    "每日记录",
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
                verticalArrangement = Arrangement.Center
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
            }
        }
    }
}

private fun formatRecordDate(value: String): String = runCatching {
    LocalDate.parse(value).format(
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    )
}.getOrDefault(value)

private fun documentsInitialUri(): Uri = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents",
    "primary:${Environment.DIRECTORY_DOCUMENTS}"
)
