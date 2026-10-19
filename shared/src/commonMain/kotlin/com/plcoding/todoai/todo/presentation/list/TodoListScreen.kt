package com.plcoding.todoai.todo.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.todoai.chat.presentation.ChatBottomSheetRoot
import com.plcoding.todoai.core.designsystem.AppIcons
import com.plcoding.todoai.core.designsystem.AppTheme
import com.plcoding.todoai.todo.presentation.TodoUi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.cd_add_todo
import todoai.shared.generated.resources.cd_open_chat
import todoai.shared.generated.resources.todo_list_empty_description
import todoai.shared.generated.resources.todo_list_empty_title
import todoai.shared.generated.resources.todo_list_open_count
import todoai.shared.generated.resources.todo_list_title

@Composable
fun TodoListRoot(
    onTodoClick: (String) -> Unit,
    onAddTodoClick: () -> Unit,
    viewModel: TodoListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TodoListScreen(
        state = state,
        onAction = viewModel::onAction,
        onTodoClick = onTodoClick,
        onAddTodoClick = onAddTodoClick
    )

    if (state.isChatOpen) {
        ChatBottomSheetRoot(
            onDismiss = { viewModel.onAction(TodoListAction.OnChatDismiss) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    state: TodoListState,
    onAction: (TodoListAction) -> Unit,
    onTodoClick: (String) -> Unit,
    onAddTodoClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(Res.string.todo_list_title))
                        if (state.todos.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    Res.string.todo_list_open_count,
                                    state.openCount,
                                    state.doneCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(TodoListAction.OnChatClick) }) {
                        Icon(
                            painter = AppIcons.Chat,
                            contentDescription = stringResource(Res.string.cd_open_chat)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTodoClick) {
                Icon(
                    painter = AppIcons.Add,
                    contentDescription = stringResource(Res.string.cd_add_todo)
                )
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.todos.isEmpty() -> {
                TodoListEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.todos,
                        key = { it.id }
                    ) { todo ->
                        TodoListItem(
                            title = todo.title,
                            description = todo.description,
                            formattedCreatedAt = todo.formattedCreatedAt,
                            isDone = todo.isDone,
                            onClick = { onTodoClick(todo.id) },
                            onDoneToggle = { isDone ->
                                onAction(TodoListAction.OnDoneToggle(todo.id, isDone))
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoListEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.todo_list_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.todo_list_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val previewTodos = listOf(
    TodoUi(
        id = "1",
        title = "Buy groceries",
        description = "Milk, eggs, bread and some fruit for the week",
        isDone = false,
        formattedCreatedAt = "Oct 19, 2026, 09:30"
    ),
    TodoUi(
        id = "2",
        title = "Record the on-device AI agent video",
        description = "Show how the agent adds and completes to-dos",
        isDone = false,
        formattedCreatedAt = "Oct 18, 2026, 14:00"
    ),
    TodoUi(
        id = "3",
        title = "Book dentist appointment",
        description = "",
        isDone = true,
        formattedCreatedAt = "Oct 17, 2026, 17:05"
    )
)

@Preview(name = "With to-dos")
@Composable
private fun TodoListScreenPreview() {
    AppTheme {
        TodoListScreen(
            state = TodoListState(todos = previewTodos, isLoading = false),
            onAction = {},
            onTodoClick = {},
            onAddTodoClick = {}
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun TodoListScreenEmptyPreview() {
    AppTheme {
        TodoListScreen(
            state = TodoListState(isLoading = false),
            onAction = {},
            onTodoClick = {},
            onAddTodoClick = {}
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun TodoListScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        TodoListScreen(
            state = TodoListState(todos = previewTodos, isLoading = false),
            onAction = {},
            onTodoClick = {},
            onAddTodoClick = {}
        )
    }
}
