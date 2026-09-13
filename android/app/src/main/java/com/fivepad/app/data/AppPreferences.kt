package com.fivepad.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tema yang dipilih pengguna. Gelap adalah bawaannya. */
enum class ThemeMode { DARK, LIGHT }

/**
 * Pilihan tampilan lokal: tema, slot terakhir, dan tampilan Markdown.
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

    var lastSlot: Int
        get() = prefs.getInt("lastSlot", 1).coerceIn(1, Note.SLOT_COUNT)
        set(value) { prefs.edit().putInt("lastSlot", value.coerceIn(1, Note.SLOT_COUNT)).apply() }

    var markdownView: Boolean
        get() = prefs.getBoolean("markdownView", false)
        set(value) { prefs.edit().putBoolean("markdownView", value).apply() }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _theme.value = mode
    }

    /**
     * Mode gelap terakhir yang sudah dititipkan ke sistem, atau null.
     *
     * Dicatat sendiri, bukan dibaca dari sistem: `UiModeManager.nightMode`
     * menjawab setelan **perangkat**, bukan yang berlaku untuk aplikasi ini.
     * Membandingkan dengan jawaban yang salah berarti menitipkan ulang nilai
     * yang sama di setiap kali buka — dan tiap penitipan memicu aplikasi
     * dibuat ulang.
     */
    var appliedNightMode: Int?
        get() = if (prefs.contains(KEY_NIGHT_MODE)) prefs.getInt(KEY_NIGHT_MODE, 0) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_NIGHT_MODE) else putInt(KEY_NIGHT_MODE, value)
            }.apply()
        }

    private fun readTheme(): ThemeMode {
        val stored = prefs.getString(KEY_THEME, null) ?: return ThemeMode.DARK
        // Nilai tersimpan bisa berasal dari versi yang tidak lagi mengenalnya —
        // misalnya "SYSTEM" bila suatu saat ditambahkan lalu dicabut lagi.
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.DARK
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_NIGHT_MODE = "appliedNightMode"
    }
}
