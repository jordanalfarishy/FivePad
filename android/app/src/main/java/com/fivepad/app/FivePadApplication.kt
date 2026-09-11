package com.fivepad.app

import android.app.Application
import com.fivepad.app.data.AppPreferences
import com.fivepad.app.data.FivePadDatabase
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.reminder.Reminders

class FivePadApplication : Application() {

    val repository: FivePadRepository by lazy { FivePadRepository(FivePadDatabase.build(this)) }

    val preferences: AppPreferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        // Kanal harus ada sebelum notifikasi pertama dikirim, dan membuatnya
        // berulang kali tidak berbahaya — jadi dibuat sekali di sini.
        Reminders.ensureChannel(this)
    }
}
