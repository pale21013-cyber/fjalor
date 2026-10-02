package com.example

import android.app.Application

class FjalorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FjalorApp
            private set
    }
}
