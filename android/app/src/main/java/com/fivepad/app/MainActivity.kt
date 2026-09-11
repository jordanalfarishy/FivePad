package com.fivepad.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fivepad.app.ui.HomeScreen
import com.fivepad.app.ui.LaunchRequest
import com.fivepad.app.ui.theme.FivePadTheme

class MainActivity : ComponentActivity() {

    // Aktivitas ini singleTask: niat kedua tidak membuat aktivitas baru,
    // melainkan mendarat di onNewIntent. Tanpa state yang bisa diamati di sini,
    // niat kedua itu akan hilang tanpa jejak.
    private val request = mutableStateOf(LaunchRequest())

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
        request.value = LaunchRequest.from(intent)
        setContent {
            FivePadTheme {
                HomeScreen(
                    request = request.value,
                    onRequestHandled = { request.value = LaunchRequest() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        request.value = LaunchRequest.from(intent)
    }
}
