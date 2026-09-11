package com.fivepad.app.data

import android.content.Context
import com.fivepad.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pilihan tema disimpan per perangkat dan sengaja tidak ikut tersinkronisasi —
 * mengikuti preseden FR-5.2, di mana ukuran huruf juga milik perangkat, bukan akun.
 * Seseorang bisa saja ingin mode gelap di ponsel tapi terang di Mac.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("fivepad_settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(value: ThemeMode) {
        prefs.edit().putString(KEY_THEME, value.name).apply()
        _mode.value = value
    }

    /** Memutar SYSTEM → LIGHT → DARK → SYSTEM. */
    fun cycle() {
        set(
            when (_mode.value) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            },
        )
    }

    private fun read(): ThemeMode =
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
