package com.fivepad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fivepad.app.ui.HomeScreen
import com.fivepad.app.ui.theme.FivePadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val themePrefs = (application as FivePadApplication).themePreferences

        setContent {
            val mode by themePrefs.mode.collectAsStateWithLifecycle()
            FivePadTheme(mode = mode) {
                HomeScreen(
                    themeMode = mode,
                    onThemeChange = themePrefs::set,
                )
            }
        }
    }
}
