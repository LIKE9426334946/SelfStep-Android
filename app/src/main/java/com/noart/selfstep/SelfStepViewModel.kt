package com.noart.selfstep

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noart.selfstep.data.BackupOperation
import com.noart.selfstep.data.DocumentJsonBackup
import com.noart.selfstep.data.LocalJsonRepository
import com.noart.selfstep.model.DailyRecord
import com.noart.selfstep.model.DailyTaskStatus
import com.noart.selfstep.model.DisciplineTask
import com.noart.selfstep.model.SelfStepData
import com.noart.selfstep.model.TaskType
import com.noart.selfstep.model.TaskCheckWindow
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SelfStepUiState(
    val data: SelfStepData = SelfStepData(),
    val today: LocalDate = LocalDate.now(),
    val storageMessage: String? = null,
    val backupDirectoryReady: Boolean = false,
    val backupInProgress: Boolean = false
) {
    val todayRecord: DailyRecord
        get() = data.records[today.toString()] ?: DailyRecord(today.toString(), emptyList())
}

class SelfStepViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalJsonRepository(application)
    private val documentBackup = DocumentJsonBackup(application)
    private val _uiState = mutableStateOf(
        SelfStepUiState(backupDirectoryReady = documentBackup.hasDirectoryAccess())
    )
    val uiState: State<SelfStepUiState> = _uiState

    init {
        val loaded = repository.load()
        publish(normalizeToday(loaded, LocalDate.now()), LocalDate.now())
    }

    fun refreshToday() {
        val today = LocalDate.now()
        val normalized = normalizeToday(_uiState.value.data, today)
        if (today != _uiState.value.today || normalized != _uiState.value.data) {
            publish(normalized, today)
        }
    }

    fun toggleTodayTask(taskId: String) {
        if (!TaskCheckWindow.isOpen(LocalTime.now())) return

        val state = _uiState.value
        val record = state.todayRecord
        val updatedRecord = record.copy(
            tasks = record.tasks.map { task ->
                if (task.taskId == taskId) task.copy(completed = !task.completed) else task
            }
        )
        publish(
            state.data.copy(records = state.data.records + (record.date to updatedRecord)),
            state.today
        )
    }

    fun addTask(title: String, type: TaskType) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        val state = _uiState.value
        val task = DisciplineTask(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            type = type,
            createdAt = System.currentTimeMillis()
        )
        publish(normalizeToday(state.data.copy(tasks = state.data.tasks + task), state.today), state.today)
    }

    fun updateTask(taskId: String, title: String, type: TaskType) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        val state = _uiState.value
        val updatedTasks = state.data.tasks.map { task ->
            if (task.id == taskId) task.copy(title = cleanTitle, type = type) else task
        }
        publish(normalizeToday(state.data.copy(tasks = updatedTasks), state.today), state.today)
    }

    fun deleteTask(taskId: String) {
        val state = _uiState.value
        val updated = state.data.copy(tasks = state.data.tasks.filterNot { it.id == taskId })
        publish(normalizeToday(updated, state.today), state.today)
    }

    fun clearStorageMessage() {
        _uiState.value = _uiState.value.copy(storageMessage = null)
    }

    fun exportBackup() {
        launchExport()
    }

    fun importBackup() {
        launchImport()
    }

    fun configureBackupDirectory(treeUri: Uri, operation: BackupOperation) {
        when (operation) {
            BackupOperation.EXPORT -> launchExport(treeUri)
            BackupOperation.IMPORT -> launchImport(treeUri)
        }
    }

    private fun launchExport(treeUri: Uri? = null) {
        if (_uiState.value.backupInProgress) return
        val dataSnapshot = _uiState.value.data

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(backupInProgress = true, storageMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    treeUri?.let(documentBackup::configureDirectory)
                    documentBackup.export(dataSnapshot)
                }
            }
            _uiState.value = _uiState.value.copy(
                backupDirectoryReady = documentBackup.hasDirectoryAccess(),
                backupInProgress = false,
                storageMessage = result.fold(
                    onSuccess = { "已导出到 ${DocumentJsonBackup.BACKUP_LOCATION}" },
                    onFailure = { it.message ?: "导出失败，请稍后重试。" }
                )
            )
        }
    }

    private fun launchImport(treeUri: Uri? = null) {
        if (_uiState.value.backupInProgress) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(backupInProgress = true, storageMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    treeUri?.let(documentBackup::configureDirectory)
                    val today = LocalDate.now()
                    val imported = normalizeToday(documentBackup.import(), today)
                    repository.save(imported)
                    ImportedSnapshot(imported, today)
                }
            }

            result.fold(
                onSuccess = { imported ->
                    _uiState.value = SelfStepUiState(
                        data = imported.data,
                        today = imported.today,
                        storageMessage = "导入成功，任务和历史记录已更新。",
                        backupDirectoryReady = documentBackup.hasDirectoryAccess(),
                        backupInProgress = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        backupDirectoryReady = documentBackup.hasDirectoryAccess(),
                        backupInProgress = false,
                        storageMessage = error.message ?: "导入失败，请检查备份文件。"
                    )
                }
            )
        }
    }

    private fun normalizeToday(data: SelfStepData, today: LocalDate): SelfStepData {
        val date = today.toString()
        val previousStatuses = data.records[date]?.tasks.orEmpty().associateBy { it.taskId }
        val currentStatuses = data.tasks
            .sortedBy { it.createdAt }
            .map { task ->
                DailyTaskStatus(
                    taskId = task.id,
                    title = task.title,
                    type = task.type,
                    completed = previousStatuses[task.id]?.completed ?: false
                )
            }
        val record = DailyRecord(date = date, tasks = currentStatuses)
        return data.copy(records = data.records + (date to record))
    }

    private fun publish(data: SelfStepData, today: LocalDate) {
        val storageError = runCatching { repository.save(data) }
            .exceptionOrNull()
            ?.let { "本地数据保存失败，请检查手机存储空间。" }

        _uiState.value = SelfStepUiState(
            data = data,
            today = today,
            storageMessage = storageError,
            backupDirectoryReady = documentBackup.hasDirectoryAccess(),
            backupInProgress = _uiState.value.backupInProgress
        )
    }

    private data class ImportedSnapshot(
        val data: SelfStepData,
        val today: LocalDate
    )

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SelfStepViewModel::class.java))
            return SelfStepViewModel(application) as T
        }
    }
}
