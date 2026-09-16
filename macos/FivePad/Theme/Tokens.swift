import SwiftUI

/// Titik pendaratan nilai desain, kembaran `Tokens.kt` di Android.
///
/// Angka Figma dipakai apa adanya: bingkai 375 pt di Figma = 375 pt di macOS,
/// sama seperti 375 dp di Android. Yang berubah antar platform hanya tata
/// letaknya, bukan ukurannya.
enum Tokens {
    static let space1: CGFloat = 4
    static let space2: CGFloat = 8
    static let space3: CGFloat = 12
    static let space4: CGFloat = 16
    static let space5: CGFloat = 20
    static let space6: CGFloat = 24

    /// Sisi kiri-kanan isi catatan.
    static let screenPadding = space4

    static let radiusSm: CGFloat = 12
    static let radiusMd: CGFloat = 16
    static let radiusLg: CGFloat = 18
    static let radiusPill: CGFloat = 999

    // Bilah atas
    static let topBarHeight: CGFloat = 56
    static let dot: CGFloat = 24
    static let dotRing: CGFloat = 2
    static let dotGap = space4
    static let titleRowHeight: CGFloat = 40
    static let stripeHeight: CGFloat = 4

    // Bilah bawah
    static let navHeight: CGFloat = 62
    static let pillWidth: CGFloat = 84
    static let pillHeight: CGFloat = 52

    /// Area sentuh/klik minimum yang wajar — titik slot 24 pt jauh di
    /// bawahnya, jadi sasaran kliknya diperbesar tanpa mengubah ukuran
    /// lingkaran yang tampak.
    static let touchTarget: CGFloat = 48

    static let bodyTextSize: CGFloat = 16
    static let bodyLineHeight: CGFloat = 28
    static let captionTextSize: CGFloat = 12

    /// FR-5.1: jendela tidak boleh lebih kecil dari ini.
    static let minWindow = CGSize(width: 480, height: 420)

    /// Di bawah lebar ini, tata letaknya runtuh jadi satu kolom bertab —
    /// persis layar Android. Di atasnya, catatan dan tugas berdampingan.
    static let sideBySideWidth: CGFloat = 720
}
