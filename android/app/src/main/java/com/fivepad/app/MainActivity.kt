package com.fivepad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fivepad.app.ui.HomeScreen
import com.fivepad.app.ui.theme.FivePadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Harus dipanggil sebelum super.onCreate(): di sinilah tema pembuka
        // ditukar kembali ke tema aplikasi lewat postSplashScreenTheme.
        installSplashScreen()

        // Aplikasi hanya bermode gelap, jadi bilah sistem dikunci gelap sejak awal
        // dan tidak perlu lagi disesuaikan saat tema berubah.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            FivePadTheme { HomeScreen() }
        }
    }
}
