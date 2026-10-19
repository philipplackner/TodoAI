package com.plcoding.todoai.di

import com.plcoding.todoai.agent.LiteRtLmEngine
import com.plcoding.todoai.agent.androidModelPaths
import com.plcoding.todoai.agent.domain.LlmEngine
import com.plcoding.todoai.todo.data.createTodoDatabase
import com.plcoding.todoai.todo.data.todoDatabaseBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { createTodoDatabase(todoDatabaseBuilder(androidContext())) }
    single { androidModelPaths(androidContext()) }
    single<LlmEngine> { LiteRtLmEngine() }
    single { HttpClient(OkHttp) }
}
