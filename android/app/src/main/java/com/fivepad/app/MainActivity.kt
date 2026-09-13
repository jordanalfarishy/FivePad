package com.fivepad.app

import android.app.UiModeManager
import android.content.Intent
import android.os.Build
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
        // Latar jendela disetel sebelum apa pun digambar.
        val preferences = (application as FivePadApplication).preferences
        if (preferences.theme.value == ThemeMode.LIGHT) setTheme(R.style.Theme_FivePad_Light)

        // Layar pembuka digambar sistem dari tema di manifes, sebelum satu
        // baris kode aplikasi pun berjalan — jadi ia tidak bisa dibuat
        // mengikuti pilihan di dalam aplikasi dari dalam aplikasi. Yang bisa
        // adalah sebaliknya: menitipkan pilihan itu ke sistem, yang lalu
        // menyelesaikan sumber daya aplikasi ini — termasuk tema pembukanya —
        // dalam mode yang sama. Tersedia sejak Android 12; di bawah itu layar
        // pembuka hanya bisa mengikuti mode perangkat.
        syncSystemNightMode()

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
                    onRequestHandled = {
                        request.value = LaunchRequest()
                        setIntent(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
                    },
                )
            }
        }
    }

    /**
     * Menitipkan mode gelap aplikasi ke sistem.
     *
     * Hanya dipanggil bila jawabannya berbeda dari yang sedang berlaku:
     * memanggilnya dengan nilai yang sama tidak berguna, dan memanggilnya
     * dengan nilai berbeda memicu aktivitas dibuat ulang.
     */
    private fun syncSystemNightMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val preferences = (application as FivePadApplication).preferences
        val wanted = if (preferences.theme.value == ThemeMode.LIGHT) {
            UiModeManager.MODE_NIGHT_NO
        } else {
            UiModeManager.MODE_NIGHT_YES
        }
        if (preferences.appliedNightMode == wanted) return
        val manager = getSystemService(UiModeManager::class.java) ?: return
        preferences.appliedNightMode = wanted
        manager.setApplicationNightMode(wanted)
    }

    /**
     * Tema yang baru dipilih dititipkan ke sistem saat layar sudah ditinggalkan.
     *
     * Menitipkannya seketika akan membuat aktivitas dibuat ulang di depan mata
     * pengguna, padahal Compose sudah mengganti warnanya sendiri tanpa itu.
     * Yang tertunda hanya warna layar pembuka, dan layar pembuka berikutnya
     * baru datang setelah aplikasi ditinggalkan.
     */
    override fun onStop() {
        super.onStop()
        syncSystemNightMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        request.value = LaunchRequest.from(intent)
    }
}
