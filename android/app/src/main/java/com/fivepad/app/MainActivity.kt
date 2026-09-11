package com.fivepad.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fivepad.app.data.ThemeMode
import com.fivepad.app.ui.HomeScreen
import com.fivepad.app.ui.LaunchRequest
import com.fivepad.app.ui.theme.FivePadTheme

class MainActivity : ComponentActivity() {

    // Aktivitas ini singleTask: niat kedua tidak membuat aktivitas baru,
    // melainkan mendarat di onNewIntent. Tanpa state yang bisa diamati di sini,
    // niat kedua itu akan hilang tanpa jejak.
    private val request = mutableStateOf(LaunchRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Latar jendela disetel sebelum apa pun digambar. Layar pembukanya
        // sendiri tetap gelap di kedua tema: jendela awal digambar sistem dari
        // tema di manifes, sebelum satu baris kode aplikasi pun berjalan.
        val preferences = (application as FivePadApplication).preferences
        if (preferences.theme.value == ThemeMode.LIGHT) setTheme(R.style.Theme_FivePad_Light)

        // Harus dipanggil sebelum super.onCreate(): di sinilah tema pembuka
        // ditukar kembali ke tema aplikasi lewat postSplashScreenTheme.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        request.value = LaunchRequest.from(intent)

        setContent {
            val theme by preferences.theme.collectAsStateWithLifecycle()

            // Ikon bilah sistem harus ikut berbalik saat tema berganti: ikon
            // putih di atas bilah terang tidak terlihat sama sekali. Dipanggil
            // ulang setiap tema berubah, bukan sekali di onCreate.
            DisposableEffect(theme) {
                val transparent = android.graphics.Color.TRANSPARENT
                if (theme == ThemeMode.LIGHT) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(transparent, transparent),
                        navigationBarStyle = SystemBarStyle.light(transparent, transparent),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(transparent),
                        navigationBarStyle = SystemBarStyle.dark(transparent),
                    )
                }
                onDispose {}
            }

            FivePadTheme(theme) {
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
