package com.plcoding.todoai.todo.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.plcoding.todoai.todo.presentation.detail.TodoDetailRoot
import com.plcoding.todoai.todo.presentation.list.TodoListRoot

fun NavGraphBuilder.todoGraph(navController: NavController) {
    composable<TodoListRoute> {
        TodoListRoot(
            onTodoClick = { navController.navigate(TodoDetailRoute(todoId = it)) },
            onAddTodoClick = { navController.navigate(TodoDetailRoute()) }
        )
    }
    composable<TodoDetailRoute> {
        TodoDetailRoot(
            onNavigateBack = { navController.navigateUp() }
        )
    }
}
