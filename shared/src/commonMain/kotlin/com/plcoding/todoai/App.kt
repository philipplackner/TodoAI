package com.plcoding.todoai

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.plcoding.todoai.core.designsystem.AppTheme
import com.plcoding.todoai.todo.presentation.TodoListRoute
import com.plcoding.todoai.todo.presentation.todoGraph

@Composable
fun App() {
    AppTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = TodoListRoute
        ) {
            todoGraph(navController)
        }
    }
}
