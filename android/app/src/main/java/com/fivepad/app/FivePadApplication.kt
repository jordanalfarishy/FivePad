package com.fivepad.app

import android.app.Application
import com.fivepad.app.data.FivePadDatabase
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.data.ThemePreferences

class FivePadApplication : Application() {

    val repository: FivePadRepository by lazy {
        val db = FivePadDatabase.build(this)
        FivePadRepository(db.notes(), db.todos())
    }

    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
}
