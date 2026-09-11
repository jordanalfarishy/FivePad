package com.fivepad.app

import android.app.Application
import com.fivepad.app.data.FivePadDatabase
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.data.ThemePreferences

class FivePadApplication : Application() {

    val repository: FivePadRepository by lazy { FivePadRepository(FivePadDatabase.build(this)) }

    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
}
