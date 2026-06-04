package com.example.ubereatscompanion

import android.app.Application
import com.example.ubereatscompanion.data.AppDatabase
import com.example.ubereatscompanion.data.CompanionRepository

class UberCompanionApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: CompanionRepository by lazy { CompanionRepository(database) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: UberCompanionApp
            private set
    }
}
