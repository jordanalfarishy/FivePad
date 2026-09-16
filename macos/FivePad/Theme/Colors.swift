import AppKit
import SwiftUI

/// Tema yang dipilih pengguna. Gelap adalah bawaannya, sama seperti Android.
enum ThemeMode: String, CaseIterable {
    case dark = "DARK"
    case light = "LIGHT"
}

/// Warna FivePad untuk satu tema.
///
/// Nilai-nilainya disalin persis dari `FivePadColors` di Android — bukan
/// ditafsirkan ulang. Dua aplikasi yang "mirip" tapi warnanya bergeser dua-tiga
/// langkah justru terasa seperti dua produk berbeda yang saling meniru; yang
/// membuatnya terasa satu produk adalah nilai yang sama sampai byte terakhir.
///
/// Android pindah ke kanvas nyaris-hitam dan aksen biru tunggal pada
/// pembaruan sistem visual 14 September 2026 (`docs/android-ui.md`); nilai di
/// sini mengikuti pembaruan itu, bukan palet merah sebelumnya.
///
/// Seperti di Android, yang disimpan hanya yang benar-benar berbeda antar tema.
/// Sisanya diturunkan dari `ink` dengan alpha yang sama di kedua tema.
struct FivePadColors {
    let isLight: Bool
    /// Latar isi — layar catatan dan layar tugas.
    let background: Color
    /// Bilah status, bilah atas, baris judul, dan bilah bawah.
    let bar: Color
    /// Kartu baris: tugas, pengaturan, dan isi lembar.
    let row: Color
    /// Permukaan tenang untuk jeda antarbagian.
    let separator: Color
    let ink: Color
    let checkboxFill: Color
    let checkboxStroke: Color
    /// Aksen per slot. Berbeda antar tema agar kontrasnya tetap ada.
    let slotAccents: [Color]
    /// Aksen tindakan tunggal: tombol tambah, pilihan aktif, tautan, dan kotak
    /// centang terisi. Sama di kedua tema sejak pembaruan sistem visual.
    let accent: Color
    /// Opasitas teks sekunder — nilai terendah yang mencapai 4,5:1 di tema itu.
    let mutedAlpha: Double
    /// Isian blok kode pada tampilan biasa. Hitam pekat di gelap, putih di terang.
    let codeFill: Color
    /// Teks tautan memakai aksen tindakan yang sama di kedua tema.
    let link: Color
    /// Garis pemisah chrome dari isi, dan tepi kolom isian — nilai eksplisit,
    /// bukan turunan dari ink, supaya tetap terlihat di antara dua permukaan
    /// yang sengaja sangat berdekatan.
    let hairline: Color
    /// Label dan placeholder kolom isian — opaque, bukan ink yang diencerkan,
    /// supaya tetap terbaca di atas permukaan kolom yang juga opaque.
    let fieldSecondary: Color

    var muted: Color { ink.opacity(mutedAlpha) }
    /// Permukaan kolom isian sama dengan `row` di kedua tema Android.
    var fieldSurface: Color { row }
    /// Tepi kolom isian sama dengan hairline di kedua tema Android.
    var fieldBorder: Color { hairline }
    /// Kotak centang yang sudah dicentang — isian beraksen.
    var checkedFill: Color { accent }
    /// Garis tepi tipis di dalam setiap titik slot.
    var dotStroke: Color { ink.opacity(0.24) }
    /// Cincin titik aktif, digambar di luar lingkaran 24 pt.
    var dotRing: Color { ink }
    /// Pegangan seret pada baris tugas.
    var dragHandle: Color { ink.opacity(0.1) }

    static let dark = FivePadColors(
        isLight: false,
        background: Color(hex: 0x0F0F10),
        bar: Color(hex: 0x0F0F10),
        row: Color(hex: 0x1E1E21),
        separator: Color(hex: 0x161618),
        ink: Color(hex: 0xECECEE),
        checkboxFill: Color(hex: 0x26262A),
        checkboxStroke: Color(hex: 0x66666E),
        slotAccents: [0xEF7A5A, 0xE0A63F, 0x63BC85, 0x48BEDD, 0xA186D6].map { Color(hex: $0) },
        accent: Color(hex: 0x3A7BFD),
        mutedAlpha: 0.64,
        codeFill: .black,
        link: Color(hex: 0x3A7BFD),
        hairline: Color(hex: 0x2A2A2E),
        fieldSecondary: Color(hex: 0x9A9AA2),
    )

    static let light = FivePadColors(
        isLight: true,
        background: .white,
        bar: .white,
        row: Color(hex: 0xF1F1F3),
        separator: Color(hex: 0xF4F4F6),
        ink: Color(hex: 0x16161A),
        checkboxFill: Color(hex: 0xF1F1F3),
        checkboxStroke: Color(hex: 0x9A9AA6),
        slotAccents: [0xDB2F00, 0xA06700, 0x1A8442, 0x0E7D9B, 0x5320B7].map { Color(hex: $0) },
        accent: Color(hex: 0x3A7BFD),
        mutedAlpha: 0.64,
        codeFill: .white,
        link: Color(hex: 0x3A7BFD),
        hairline: Color(hex: 0xE6E6EA),
        fieldSecondary: Color(hex: 0x66666E),
    )

    static func of(_ mode: ThemeMode) -> FivePadColors { mode == .light ? .light : .dark }
}

/// Tepi kotak centang tercentang — tekanan dari aksen biru.
let checkedStroke = Color(hex: 0x2E63D6)

/// Isian CTA utama, sama dengan aksen merek dan dipasangkan dengan teks putih.
let filledAccent = Color(hex: 0x3A7BFD)

/// Opasitas titik slot yang tidak aktif. Lihat catatan panjangnya di Android.
let dotInactiveAlpha = 0.40

/// Opasitas isian kutipan pada tampilan biasa — aksen slot di atas latar halaman.
let quoteFillAlpha = 0.12

extension Color {
    /// Mencampur warna ini dengan putih. Dipakai pola pita, dan dihitung di
    /// ruang sRGB yang sama dengan `Color.mix` di Android.
    func lightened(by amount: Double) -> Color {
        let c = NSColor(self).usingColorSpace(.sRGB) ?? .white
        return Color(
            .sRGB,
            red: c.redComponent + (1 - c.redComponent) * amount,
            green: c.greenComponent + (1 - c.greenComponent) * amount,
            blue: c.blueComponent + (1 - c.blueComponent) * amount,
            opacity: 1,
        )
    }

    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1,
        )
    }
}

private struct FivePadColorsKey: EnvironmentKey {
    static let defaultValue = FivePadColors.dark
}

extension EnvironmentValues {
    var fivePad: FivePadColors {
        get { self[FivePadColorsKey.self] }
        set { self[FivePadColorsKey.self] = newValue }
    }
}
