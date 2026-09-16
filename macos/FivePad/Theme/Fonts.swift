import AppKit
import SwiftUI

/// Sora dan Inter — berkas yang sama dengan Android
/// (`android/app/src/main/res/font/*_variable.ttf`), dibundel di
/// `Resources/Fonts/` dan didaftarkan lewat `ATSApplicationFontsPath`.
///
/// Sora membawa judul layar, nama bagian, tombol, dan label navigasi. Inter
/// membawa teks catatan, tugas, deskripsi, dan metadata — persis pembagian
/// yang sama di `ui/theme/Type.kt`.
enum FivePadFont {
    // PostScript names of the named instances, confirmed by loading both
    // variable fonts through Core Text (Sora has no Medium instance — it
    // jumps Regular -> SemiBold -> Bold -> ExtraBold, which is why
    // `FivePadText` below never asks for a Sora weight between those).
    static let soraRegular = "Sora-Regular"
    static let soraSemiBold = "Sora-Regular_SemiBold"
    static let soraBold = "Sora-Regular_Bold"
    static let interRegular = "Inter-Regular"
    static let interMedium = "Inter-Regular_Medium"
    static let interSemiBold = "Inter-Regular_SemiBold"
    static let interBold = "Inter-Regular_Bold"
    static let interItalic = "Inter-Italic"
    static let interBoldItalic = "Inter-Italic_Bold-Italic"

    /// `ATSApplicationFontsPath` biasanya cukup sendirian, tapi pendaftaran
    /// eksplisit ini menjaga instans bernama pada fon variabel tetap
    /// ditemukan di setiap versi macOS — dan tidak berbahaya dipanggil dua
    /// kali, karena `CTFontManagerRegisterFontsForURL` menolak berkas yang
    /// sudah terdaftar tanpa efek samping lain.
    static func registerIfNeeded() {
        guard NSFontManager.shared.availableMembers(ofFontFamily: "Sora") == nil else { return }
        for name in ["sora_variable", "inter_variable", "inter_italic_variable"] {
            guard let url = Bundle.main.url(forResource: name, withExtension: "ttf", subdirectory: "Fonts") else {
                continue
            }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }
}

/// Satu gaya teks bernama — padanan `FivePadText` di Android.
struct FivePadTextStyle {
    let postScriptName: String
    let size: CGFloat
    let lineHeight: CGFloat
    let tracking: CGFloat
}

/// Katalog gaya teks. Ukuran, bobot, tinggi baris, dan kerning disalin persis
/// dari `FivePadText` di `ui/theme/Type.kt`.
enum FivePadText {
    static let display = FivePadTextStyle(postScriptName: FivePadFont.soraBold, size: 32, lineHeight: 38, tracking: -0.5)
    static let screenTitle = FivePadTextStyle(postScriptName: FivePadFont.soraSemiBold, size: 24, lineHeight: 30, tracking: -0.3)
    static let sectionTitle = FivePadTextStyle(postScriptName: FivePadFont.soraSemiBold, size: 15, lineHeight: 20, tracking: 0)
    static let headerName = FivePadTextStyle(postScriptName: FivePadFont.soraSemiBold, size: 17, lineHeight: 22, tracking: -0.1)
    static let button = FivePadTextStyle(postScriptName: FivePadFont.soraSemiBold, size: 15, lineHeight: 19, tracking: 0)
    static let tab = FivePadTextStyle(postScriptName: FivePadFont.soraSemiBold, size: 10, lineHeight: 12, tracking: 0.2)
    static let body = FivePadTextStyle(postScriptName: FivePadFont.interRegular, size: 16, lineHeight: 25, tracking: 0)
    static let message = FivePadTextStyle(postScriptName: FivePadFont.interRegular, size: 15, lineHeight: 23, tracking: 0)
    static let description = FivePadTextStyle(postScriptName: FivePadFont.interRegular, size: 13, lineHeight: 19, tracking: 0)
    static let meta = FivePadTextStyle(postScriptName: FivePadFont.interMedium, size: 11, lineHeight: 14, tracking: 0.1)
}

extension View {
    /// `.lineSpacing` menambah jarak ANTAR baris (bukan tinggi total seperti
    /// `lineHeight` Compose), jadi dihitung dari metrik fon sungguhan, bukan
    /// kelipatan kira-kira.
    func fivePadStyle(_ style: FivePadTextStyle) -> some View {
        let nsFont = NSFont(name: style.postScriptName, size: style.size) ?? .systemFont(ofSize: style.size)
        let naturalHeight = nsFont.ascender - nsFont.descender + nsFont.leading
        return self
            .font(.custom(style.postScriptName, size: style.size))
            .tracking(style.tracking)
            .lineSpacing(max(0, style.lineHeight - naturalHeight))
    }
}
