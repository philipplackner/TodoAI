package com.plcoding.todoai

import android.app.Application
import com.plcoding.todoai.di.initKoin
import org.koin.android.ext.koin.androidContext

class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TodoApplication)
        }
    }
}
