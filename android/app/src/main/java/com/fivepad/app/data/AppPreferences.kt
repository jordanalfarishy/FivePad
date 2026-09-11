package com.fivepad.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tema yang dipilih pengguna. Gelap adalah bawaannya. */
enum class ThemeMode { DARK, LIGHT }

/**
 * Pilihan pengguna yang bukan data: sejauh ini hanya tema.
 *
 * Memakai SharedPreferences, bukan DataStore, justru karena ia sinkron. Tema
 * harus sudah diketahui pada bingkai pertama — kalau dibaca secara asinkron,
 * aplikasi menggambar tema gelap dulu lalu berkedip ke terang, dan kedipan itu
 * muncul di setiap kali buka, bukan sekali.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("fivepad", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemeMode> = _theme

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _theme.value = mode
    }

    private fun readTheme(): ThemeMode {
        val stored = prefs.getString(KEY_THEME, null) ?: return ThemeMode.DARK
        // Nilai tersimpan bisa berasal dari versi yang tidak lagi mengenalnya —
        // misalnya "SYSTEM" bila suatu saat ditambahkan lalu dicabut lagi.
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.DARK
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
