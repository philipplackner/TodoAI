package com.plcoding.todoai.todo.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.todoai.core.designsystem.AppIcons
import com.plcoding.todoai.core.designsystem.AppTheme
import com.plcoding.todoai.core.presentation.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.cd_delete_todo
import todoai.shared.generated.resources.cd_navigate_back
import todoai.shared.generated.resources.todo_added_at
import todoai.shared.generated.resources.todo_detail_description_label
import todoai.shared.generated.resources.todo_detail_done_label
import todoai.shared.generated.resources.todo_detail_edit_title
import todoai.shared.generated.resources.todo_detail_new_title
import todoai.shared.generated.resources.todo_detail_save
import todoai.shared.generated.resources.todo_detail_title_label
import todoai.shared.generated.resources.todo_updated_at

@Composable
fun TodoDetailRoot(
    onNavigateBack: () -> Unit,
    viewModel: TodoDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TodoDetailEvent.NavigateBack -> onNavigateBack()
        }
    }

    TodoDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    state: TodoDetailState,
    onAction: (TodoDetailAction) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isNewTodo) {
                                Res.string.todo_detail_new_title
                            } else {
                                Res.string.todo_detail_edit_title
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = AppIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    if (!state.isNewTodo) {
                        IconButton(onClick = { onAction(TodoDetailAction.OnDeleteClick) }) {
                            Icon(
                                painter = AppIcons.Delete,
                                contentDescription = stringResource(Res.string.cd_delete_todo)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { onAction(TodoDetailAction.OnSaveClick) },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Text(text = stringResource(Res.string.todo_detail_save))
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onAction(TodoDetailAction.OnTitleChange(it)) },
                label = { Text(text = stringResource(Res.string.todo_detail_title_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { onAction(TodoDetailAction.OnDescriptionChange(it)) },
                label = { Text(text = stringResource(Res.string.todo_detail_description_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            DoneToggleRow(
                isDone = state.isDone,
                onDoneChange = { onAction(TodoDetailAction.OnDoneChange(it)) }
            )
            if (state.formattedCreatedAt != null && state.formattedUpdatedAt != null) {
                TodoDatesCard(
                    formattedCreatedAt = state.formattedCreatedAt,
                    formattedUpdatedAt = state.formattedUpdatedAt
                )
            }
        }
    }
}

@Composable
private fun DoneToggleRow(
    isDone: Boolean,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isDone,
                    role = Role.Switch,
                    onValueChange = onDoneChange
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.todo_detail_done_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isDone,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun TodoDatesCard(
    formattedCreatedAt: String,
    formattedUpdatedAt: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.todo_added_at, formattedCreatedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.todo_updated_at, formattedUpdatedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(name = "Existing to-do")
@Composable
private fun TodoDetailScreenPreview() {
    AppTheme {
        TodoDetailScreen(
            state = TodoDetailState(
                title = "Buy groceries",
                description = "Milk, eggs, bread and some fruit for the week",
                isDone = false,
                formattedCreatedAt = "Oct 19, 2026, 09:30",
                formattedUpdatedAt = "Oct 19, 2026, 10:12",
                isNewTodo = false
            ),
            onAction = {},
            onBackClick = {}
        )
    }
}

@Preview(name = "New to-do")
@Composable
private fun TodoDetailScreenNewPreview() {
    AppTheme {
        TodoDetailScreen(
            state = TodoDetailState(),
            onAction = {},
            onBackClick = {}
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun TodoDetailScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        TodoDetailScreen(
            state = TodoDetailState(
                title = "Book dentist appointment",
                description = "Call before noon",
                isDone = true,
                formattedCreatedAt = "Oct 17, 2026, 17:05",
                formattedUpdatedAt = "Oct 18, 2026, 08:40",
                isNewTodo = false
            ),
            onAction = {},
            onBackClick = {}
        )
    }
}
