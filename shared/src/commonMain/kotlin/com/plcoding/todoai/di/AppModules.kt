package com.plcoding.todoai.di

import com.plcoding.todoai.chat.presentation.ChatViewModel
import com.plcoding.todoai.todo.data.RoomTodoDataSource
import com.plcoding.todoai.todo.data.TodoDatabase
import com.plcoding.todoai.todo.domain.TodoLocalDataSource
import com.plcoding.todoai.todo.presentation.detail.TodoDetailViewModel
import com.plcoding.todoai.todo.presentation.list.TodoListViewModel
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

val chatPresentationModule = module {
    viewModelOf(::ChatViewModel)
}
