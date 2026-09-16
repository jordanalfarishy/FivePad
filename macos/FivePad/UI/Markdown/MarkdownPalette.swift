import AppKit

/// Warna dan ukuran dasar yang dipakai penataan Markdown tampilan biasa.
/// Padanan `MarkdownPalette` di Android, disusun ulang tiap kali slot atau
/// tema berganti karena aksennya mengikuti slot yang sedang aktif.
struct MarkdownPalette {
    let ink: NSColor
    /// Aksen slot: pita kiri kutipan dan blok kode, serta warna tautan/kotak
    /// centang tercentang.
    let accent: NSColor
    /// Isian kutipan — aksen 12% di atas latar halaman.
    let quoteFill: NSColor
    /// Isian blok kode: hitam pekat di gelap, putih bersih di terang.
    let codeFill: NSColor
    /// Penanda Markdown yang diredupkan, bukan disembunyikan — konvensi
    /// "pratinjau langsung" gaya Bear/iA Writer, bukan penggantian karakter.
    let mutedMarker: NSColor
    let checkboxStroke: NSColor
    let baseSize: CGFloat

    init(colors: FivePadColors, slotAccent: NSColor, baseSize: CGFloat = Tokens.bodyTextSize) {
        ink = NSColor(colors.ink)
        accent = slotAccent
        quoteFill = slotAccent.withAlphaComponent(quoteFillAlpha)
        codeFill = NSColor(colors.codeFill)
        mutedMarker = NSColor(colors.muted)
        checkboxStroke = NSColor(colors.checkboxStroke)
        self.baseSize = baseSize
    }
}
