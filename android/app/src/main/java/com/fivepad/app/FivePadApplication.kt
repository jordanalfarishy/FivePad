package com.fivepad.app

import android.app.Application
import com.fivepad.app.data.FivePadDatabase
import com.fivepad.app.data.FivePadRepository

class FivePadApplication : Application() {

    val repository: FivePadRepository by lazy { FivePadRepository(FivePadDatabase.build(this)) }
}
