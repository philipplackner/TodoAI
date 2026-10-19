package com.plcoding.todoai.di

import com.plcoding.todoai.agent.data.ModelDownloader
import com.plcoding.todoai.agent.data.ModelManager
import com.plcoding.todoai.agent.domain.TodoAgent
import com.plcoding.todoai.agent.tools.CreateTodoTool
import com.plcoding.todoai.agent.tools.GetTodosTool
import com.plcoding.todoai.agent.tools.SearchTodosTool
import com.plcoding.todoai.agent.tools.SetTodoDoneTool
import com.plcoding.todoai.agent.tools.SetTodoDueDateTool
import com.plcoding.todoai.chat.presentation.ChatViewModel
import com.plcoding.todoai.todo.data.RoomTodoDataSource
import com.plcoding.todoai.todo.data.TodoDatabase
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import com.plcoding.todoai.todo.presentation.detail.TodoDetailViewModel
import com.plcoding.todoai.todo.presentation.list.TodoListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val todoDataModule = module {
    includes(platformModule)
    single { get<TodoDatabase>().todoDao }
    singleOf(::RoomTodoDataSource) { bind<TodoLocalDataSource>() }
}

val todoPresentationModule = module {
    viewModelOf(::TodoListViewModel)
    viewModelOf(::TodoDetailViewModel)
}

val agentModule = module {
    singleOf(::ModelDownloader)
    single {
        ModelManager(
            engine = get(),
            downloader = get(),
            paths = get(),
            applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
    single {
        val todoDataSource = get<TodoLocalDataSource>()
        TodoAgent(
            engine = get(),
            tools = listOf(
                GetTodosTool(todoDataSource),
                SearchTodosTool(todoDataSource),
                CreateTodoTool(todoDataSource),
                SetTodoDoneTool(todoDataSource),
                SetTodoDueDateTool(todoDataSource)
            )
        )
    }
}

val chatPresentationModule = module {
    viewModelOf(::ChatViewModel)
}
