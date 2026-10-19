package com.plcoding.todoai.di

import com.plcoding.todoai.agent.LiteRtLmEngine
import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.agent.iosModelPaths
import com.plcoding.todoai.todo.data.createTodoDatabase
import com.plcoding.todoai.todo.data.todoDatabaseBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

actual val platformModule = module {
    single { createTodoDatabase(todoDatabaseBuilder()) }
    single { iosModelPaths() }
    single<LlmEngine> { LiteRtLmEngine() }
    single { HttpClient(Darwin) }
}
