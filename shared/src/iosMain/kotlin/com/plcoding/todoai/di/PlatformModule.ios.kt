package com.plcoding.todoai.di

import com.plcoding.todoai.todo.data.createTodoDatabase
import com.plcoding.todoai.todo.data.todoDatabaseBuilder
import org.koin.dsl.module

actual val platformModule = module {
    single { createTodoDatabase(todoDatabaseBuilder()) }
}
