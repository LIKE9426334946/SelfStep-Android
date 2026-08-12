package com.noart.selfstep.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.noart.selfstep.SelfStepUiState
import com.noart.selfstep.model.DisciplineTask
import com.noart.selfstep.model.TaskType
import com.noart.selfstep.ui.screens.HistoryScreen
import com.noart.selfstep.ui.screens.TaskManagementScreen
import com.noart.selfstep.ui.screens.TodayScreen

private enum class AppTab(val label: String, val glyph: String) {
    TODAY("今日", "✓"),
    HISTORY("记录", "◷"),
    MANAGE("任务", "☷")
}

@Composable
fun SelfStepApp(
    state: SelfStepUiState,
    onToggleTask: (String) -> Unit,
    onAddTask: (String, TaskType) -> Unit,
    onUpdateTask: (String, String, TaskType) -> Unit,
    onDeleteTask: (String) -> Unit,
    onStorageMessageShown: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.storageMessage) {
        state.storageMessage?.let {
            snackbarHostState.showSnackbar(it)
            onStorageMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Text(
                                text = tab.glyph,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "page",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    AppTab.TODAY -> TodayScreen(
                        state = state,
                        contentPadding = contentPadding,
                        onToggleTask = onToggleTask,
                        onOpenTaskManagement = { selectedTab = AppTab.MANAGE }
                    )

                    AppTab.HISTORY -> HistoryScreen(
                        state = state,
                        contentPadding = contentPadding
                    )

                    AppTab.MANAGE -> TaskManagementScreen(
                        tasks = state.data.tasks,
                        contentPadding = contentPadding,
                        onAddTask = onAddTask,
                        onUpdateTask = onUpdateTask,
                        onDeleteTask = onDeleteTask
                    )
                }
            }
        }
    }
}
