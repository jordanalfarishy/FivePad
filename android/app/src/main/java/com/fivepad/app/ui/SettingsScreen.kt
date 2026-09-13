package com.fivepad.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fivepad.app.R
import com.fivepad.app.data.ThemeMode
import com.fivepad.app.ui.theme.FilledAccent
import com.fivepad.app.ui.theme.LocalFivePadColors
import com.fivepad.app.ui.theme.Tokens
import java.util.Locale

// Nilai diambil langsung dari Figma (node 6:2425). Sama seperti daftar tugas,
// kecuali padding bawah tiap bagian: 12 dp, bukan 4 dp.
private val SECTION_PAD_H = 8.dp
private val SECTION_PAD_TOP = 4.dp
private val SECTION_PAD_BOTTOM = 12.dp
private val ITEM_GAP = 2.dp
private val BLOCK_RADIUS = 12.dp
private val ROW_RADIUS = 4.dp
private val ROW_PAD = 12.dp
private val ROW_GAP = 8.dp
private val SEPARATOR_HEIGHT = 7.dp

/** Tombol Login — node 16:380. Lebih besar dari tingginya, jadi selalu bulat penuh. */
private val LOGIN_BUTTON_RADIUS = 35.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val colors = LocalFivePadColors.current
    val context = LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val termsUrl = stringResource(R.string.url_terms)
    val privacyUrl = stringResource(R.string.url_privacy)
    val loginPending = stringResource(R.string.settings_login_pending)
    val theme by vm.theme.collectAsStateWithLifecycle()
    var themePicker by remember { mutableStateOf(false) }
    var backups by remember { mutableStateOf(false) }
    if (backups) NoteBackupsSheet(vm) { backups = false }

    fun open(url: String) {
        // Situsnya belum ada. Bila tidak ada peramban sama sekali, niatnya
        // gagal tanpa suara — dan halaman hukum yang diam-diam tidak terbuka
        // adalah persis jenis kegagalan yang tidak boleh tak terlihat.
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, url, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(colors.bar),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .height(Tokens.topBarHeight)
                .background(colors.bar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(Tokens.topBarHeight)
                    .fillMaxHeight()
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.settings_back),
                    tint = colors.ink,
                    modifier = Modifier.size(Tokens.space6),
                )
            }
            // Rata kiri, tidak seperti judul "Tasks" yang rata tengah. Bukan
            // kelalaian: layar ini punya tombol kembali dan tidak punya
            // penyeimbang di kanan, jadi judul yang dipusatkan akan terlihat
            // meleset ke kanan.
            Text(
                stringResource(R.string.settings_title),
                fontSize = 22.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(Tokens.topBarHeight))
        }
        HorizontalDivider(color = colors.hairline)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Section(stringResource(R.string.settings_section_app)) {
                ValueRow(
                    label = stringResource(R.string.settings_theme),
                    value = stringResource(
                        if (theme == ThemeMode.LIGHT) R.string.theme_light else R.string.theme_dark,
                    ),
                    onClick = { themePicker = true },
                )
                ValueRow(
                    label = stringResource(R.string.settings_language),
                    value = locale.getDisplayLanguage(locale)
                        .replaceFirstChar { it.uppercase() },
                    onClick = { openLanguageSettings(context) },
                )
            }

            Separator()
            Section(stringResource(R.string.notes_backups)) {
                LinkRow(label = stringResource(R.string.notes_manage_backups), onClick = { backups = true })
            }
            Separator()

            Section(stringResource(R.string.settings_section_version, versionName(context))) {
                LinkRow(
                    label = stringResource(R.string.settings_check_update),
                    onClick = { openStoreListing(context) },
                )
                LinkRow(
                    label = stringResource(R.string.settings_terms),
                    onClick = { open(termsUrl) },
                )
                LinkRow(
                    label = stringResource(R.string.settings_privacy),
                    onClick = { open(privacyUrl) },
                )
            }

            LoginSection(
                onLogin = {
                    Toast.makeText(
                        context,
                        loginPending,
                        Toast.LENGTH_LONG,
                    ).show()
                },
            )
        }
    }

    if (themePicker) {
        OptionsSheet(
            title = stringResource(R.string.settings_theme),
            actions = listOf(
                SheetAction(
                    label = stringResource(R.string.theme_dark),
                    icon = painterResource(R.drawable.ic_dark_mode),
                    onClick = { vm.setTheme(ThemeMode.DARK) },
                ),
                SheetAction(
                    label = stringResource(R.string.theme_light),
                    icon = painterResource(R.drawable.ic_light_mode),
                    onClick = { vm.setTheme(ThemeMode.LIGHT) },
                ),
            ),
            onDismiss = { themePicker = false },
        )
    }
}

/**
 * Membuka pemilih bahasa per-aplikasi milik sistem.
 *
 * Sejak Android 13 bahasa per-aplikasi adalah urusan sistem, bukan urusan
 * aplikasi: pilihannya tersimpan di sana, terbaca oleh peluncur, dan tidak
 * hilang saat data aplikasi dibersihkan. Membuat pemilih sendiri berarti dua
 * tempat yang bisa berbeda jawaban. Di bawah 13 tidak ada layar itu, jadi yang
 * dibuka adalah detail aplikasi — tempat terdekat yang benar-benar ada.
 */
/** Versi dibaca dari paket yang terpasang, bukan dari BuildConfig — satu sumber, dan tidak menuntut buildConfig dinyalakan. */
private fun versionName(context: android.content.Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

private fun openLanguageSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${context.packageName}"))
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.settings_language_unavailable),
            Toast.LENGTH_LONG,
        ).show()
    }
}

/**
 * Ajakan masuk akun — node 16:367.
 *
 * Tombolnya hidup meski halaman masuk baru datang di M2. Tombol mati yang
 * tidak menjelaskan apa-apa membuat orang mengetuknya berulang kali dan
 * menyangka aplikasinya rusak; tombol yang menjawab "belum, tapi nanti"
 * setidaknya menjawab.
 */
@Composable
private fun LoginSection(onLogin: () -> Unit) {
    val colors = LocalFivePadColors.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.space3),
    ) {
        Text(
            stringResource(R.string.settings_login_blurb),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier
                .clip(RoundedCornerShape(LOGIN_BUTTON_RADIUS))
                .background(FilledAccent)
                .clickable(onClick = onLogin)
                .padding(horizontal = Tokens.space5, vertical = Tokens.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_login),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}

/**
 * Membuka halaman aplikasi di Play Store.
 *
 * Belum ada mekanisme pembaruan sendiri, dan tidak akan pernah ada: pembaruan
 * adalah urusan toko. Yang bisa dilakukan baris ini adalah mengantar ke sana.
 */
private fun openStoreListing(context: android.content.Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"),
    )
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(web)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, web.dataString, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = LocalFivePadColors.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = SECTION_PAD_H,
                end = SECTION_PAD_H,
                top = SECTION_PAD_TOP,
                bottom = SECTION_PAD_BOTTOM,
            ),
        verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SECTION_PAD_H, vertical = SECTION_PAD_TOP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
            )
        }
        Column(
            Modifier.clip(RoundedCornerShape(BLOCK_RADIUS)),
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
        ) {
            content()
        }
    }
}

@Composable
private fun Separator() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SEPARATOR_HEIGHT)
            .background(LocalFivePadColors.current.separator),
    )
}

/** Baris dua baris: nama pengaturan di atas, nilainya yang sekarang di bawah. */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    SettingRow(onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = LocalFivePadColors.current.muted,
            )
            Text(
                value,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = LocalFivePadColors.current.ink,
            )
        }
    }
}

/** Baris satu baris yang membawa keluar aplikasi. */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    SettingRow(onClick = onClick) {
        Text(
            label,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            color = LocalFivePadColors.current.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingRow(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val colors = LocalFivePadColors.current

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ROW_RADIUS))
            .background(colors.row)
            .clickable(onClick = onClick)
            .padding(ROW_PAD),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Icon(
            painterResource(R.drawable.ic_chevron_forward),
            contentDescription = null,
            // Di Figma panah ini putih 40% pada kedua tema; pada tema terang
            // itu berarti tidak ada panah sama sekali. Dipakai tinta redup.
            tint = colors.muted,
            modifier = Modifier.size(Tokens.space6),
        )
    }
}
