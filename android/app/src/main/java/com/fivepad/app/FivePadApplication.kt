package com.fivepad.app

import android.app.Application
import com.fivepad.app.data.AppPreferences
import com.fivepad.app.data.FivePadDatabase
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.reminder.Reminders
import com.fivepad.app.data.NoteWriteQueue
import com.fivepad.app.backup.NoteBackupStore
import com.fivepad.app.widget.NoteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FivePadApplication : Application() {

    val repository: FivePadRepository by lazy { FivePadRepository(FivePadDatabase.build(this)) }

    val preferences: AppPreferences by lazy { AppPreferences(this) }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val noteErrors = MutableStateFlow(false)
    val noteWrites by lazy { NoteWriteQueue(applicationScope) { noteErrors.value = true } }
    val noteBackups by lazy { NoteBackupStore(File(filesDir, "note-backups")) }

    override fun onCreate() {
        super.onCreate()
        // Kanal harus ada sebelum notifikasi pertama dikirim, dan membuatnya
        // berulang kali tidak berbahaya — jadi dibuat sekali di sini.
        Reminders.ensureChannel(this)
        applicationScope.launch {
            combine(repository.observeNotes(), preferences.theme) { notes, _ -> notes }.collectLatest { notes ->
                if (notes.size != 5) return@collectLatest
                delay(1_000)
                try {
                    withContext(Dispatchers.IO) {
                        noteBackups.saveDaily(notes)
                        NoteWidget.updateAll(this@FivePadApplication, notes)
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    noteErrors.value = true
                }
            }
        }
    }
}
