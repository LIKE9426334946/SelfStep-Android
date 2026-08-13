package com.noart.selfstep

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noart.selfstep.ui.SelfStepApp
import com.noart.selfstep.ui.theme.SelfStepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SelfStepTheme {
                val application = LocalContext.current.applicationContext as Application
                val viewModel: SelfStepViewModel = viewModel(
                    factory = SelfStepViewModel.Factory(application)
                )
                val uiState by viewModel.uiState
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshToday()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                SelfStepApp(
                    state = uiState,
                    onToggleTask = viewModel::toggleTodayTask,
                    onAddTask = viewModel::addTask,
                    onUpdateTask = viewModel::updateTask,
                    onDeleteTask = viewModel::deleteTask,
                    onExportData = viewModel::exportBackup,
                    onImportData = viewModel::importBackup,
                    onBackupDirectorySelected = viewModel::configureBackupDirectory,
                    onStorageMessageShown = viewModel::clearStorageMessage
                )
            }
        }
    }
}
