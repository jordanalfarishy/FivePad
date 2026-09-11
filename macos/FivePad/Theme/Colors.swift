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
/// Seperti di Android, yang disimpan hanya yang benar-benar berbeda antar tema.
/// Sisanya diturunkan dari `ink` dengan alpha yang sama di kedua tema.
struct FivePadColors {
    let isLight: Bool
    /// Latar isi — layar catatan dan layar tugas.
    let background: Color
    /// Bilah atas, baris judul, dan bilah bawah.
    let bar: Color
    /// Kartu baris: tugas, pengaturan, dan isi popover.
    let row: Color
    /// Pita pemisah antar bagian.
    let separator: Color
    let ink: Color
    let checkboxFill: Color
    let checkboxStroke: Color
    /// Aksen per slot. Berbeda antar tema agar kontrasnya tetap ada.
    let slotAccents: [Color]
    /// Aksen tindakan.
    let accent: Color
    /// Teks dan ikon di ATAS aksen yang terisi penuh.
    let onAccent: Color
    /// Opasitas teks sekunder — nilai terendah yang mencapai 4,5:1 di tema itu.
    let mutedAlpha: Double

    var hairline: Color { isLight ? ink.opacity(0.16) : .black.opacity(0.16) }
    var muted: Color { ink.opacity(mutedAlpha) }
    var dotStroke: Color { ink.opacity(0.24) }
    var dotRing: Color { ink }
    var dragHandle: Color { ink.opacity(0.1) }
    var checkedFill: Color { accent }

    static let dark = FivePadColors(
        isLight: false,
        background: Color(hex: 0x19191B),
        bar: Color(hex: 0x232324),
        row: Color(hex: 0x242525),
        separator: Color(hex: 0x131314),
        ink: .white,
        checkboxFill: Color(hex: 0x48484B),
        checkboxStroke: Color(hex: 0x6B6B6B),
        slotAccents: [0xEF7A5A, 0xE0A63F, 0x63BC85, 0x48BEDD, 0xA186D6].map { Color(hex: $0) },
        accent: Color(hex: 0xFF5242),
        onAccent: Color(hex: 0x19191B),
        mutedAlpha: 0.47,
    )

    static let light = FivePadColors(
        isLight: true,
        background: Color(hex: 0xEAEAE8),
        bar: Color(hex: 0xF9F9F9),
        row: .white,
        separator: Color(hex: 0xDDDDDA),
        ink: Color(hex: 0x25242C),
        checkboxFill: Color(hex: 0xEFEFED),
        checkboxStroke: Color(hex: 0xD7D7D7),
        slotAccents: [0xDB2F00, 0xA06700, 0x1A8442, 0x0E7D9B, 0x5320B7].map { Color(hex: $0) },
        accent: Color(hex: 0xC71C0D),
        onAccent: .white,
        mutedAlpha: 0.65,
    )

    static func of(_ mode: ThemeMode) -> FivePadColors { mode == .light ? .light : .dark }
}

/// Tepi kotak centang yang tercentang — selalu lebih terang dari isiannya.
let checkedStroke = Color(hex: 0xFF4332)

/// Opasitas titik slot yang tidak aktif. Lihat catatan panjangnya di Android.
let dotInactiveAlpha = 0.40

/// Latar pil navigasi yang aktif: warna tab itu sendiri, 16%.
let pillAlpha = 0.16

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
