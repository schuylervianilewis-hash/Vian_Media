package com.example

import android.app.Application

class VianApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LogKeeper.init(this)
    }
}
