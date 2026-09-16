import AppKit

/// Rentang satu blok berlatar penuh — kutipan atau kode — untuk digambar oleh
/// `MarkdownLayoutManager`.
struct MarkdownBlockRange {
    let range: NSRange
    let isCode: Bool
}

/// Rentang satu kotak centang `- [ ]` / `- [x]`.
///
/// Berbeda dari Android, tidak ada glyph pengganti: `statusRange` menunjuk
/// tepat pada karakter status (spasi atau `x`) di dalam kurung siku, dan
/// membaliknya cukup mengganti **satu karakter itu saja** di teks sumber —
/// operasi sepanjang tetap yang tidak pernah menggeser offset kursor di
/// tempat lain. `lineContentRange` menandai sisa baris, untuk coretan dan
/// warna redup saat tercentang.
struct MarkdownCheckboxRange {
    let statusRange: NSRange
    let lineContentRange: NSRange
    let checked: Bool
}

/// Rentang satu tautan `[label](alamat)`.
struct MarkdownLinkRange {
    let labelRange: NSRange
    let url: String
}

/// Hasil satu kali pemindaian: teks berlaku sama persis dengan sumbernya —
/// tidak ada karakter yang disisipkan atau dibuang — hanya atribut yang
/// berubah. Itulah yang membuat posisi kursor tidak pernah perlu dipetakan
/// ulang: rentang yang sama di teks sumber adalah rentang yang sama di sini.
struct MarkdownScanResult {
    let attributed: NSAttributedString
    let blocks: [MarkdownBlockRange]
    let checkboxes: [MarkdownCheckboxRange]
    let links: [MarkdownLinkRange]
}

enum MarkdownScanner {
    private static let heading = try! NSRegularExpression(pattern: #"^(#{1,6})\s+"#)
    private static let checkbox = try! NSRegularExpression(pattern: #"^(\s*)([-*+])\s+\[([ xX]?)]\s*"#)
    private static let quote = try! NSRegularExpression(pattern: #"^\s*>\s?"#)
    private static let fence = try! NSRegularExpression(pattern: #"^\s*```"#)

    private static let boldItalic = try! NSRegularExpression(pattern: #"\*\*\*([^*\n]+)\*\*\*"#)
    private static let bold = try! NSRegularExpression(pattern: #"\*\*([^*\n]+)\*\*"#)
    private static let italicStar = try! NSRegularExpression(pattern: #"(?<!\*)\*([^*\n]+)\*(?!\*)"#)
    private static let italicUnder = try! NSRegularExpression(pattern: #"(?<![\w_])_([^_\n]+)_(?![\w_])"#)
    private static let strike = try! NSRegularExpression(pattern: #"~~([^~\n]+)~~"#)
    private static let code = try! NSRegularExpression(pattern: #"`([^`\n]+)`"#)
    private static let link = try! NSRegularExpression(pattern: #"\[([^\]\n]*)]\(([^)\n]*)\)"#)

    /// Lebar indentasi kutipan, sama seperti Android (`BLOCK_INDENT_DP`).
    private static let blockIndent: CGFloat = 12

    static func scan(_ text: String, palette: MarkdownPalette) -> MarkdownScanResult {
        let ns = text as NSString
        let result = NSMutableAttributedString(string: text)
        result.addAttributes(
            [.font: NSFont.custom(FivePadFont.interRegular, size: palette.baseSize), .foregroundColor: palette.ink],
            range: NSRange(location: 0, length: ns.length),
        )

        var blocks: [MarkdownBlockRange] = []
        var checkboxes: [MarkdownCheckboxRange] = []
        var links: [MarkdownLinkRange] = []

        var inFence = false
        var fenceStart = 0
        var quoteStart = -1
        var quoteEnd = -1

        func closeQuoteRun() {
            if quoteStart >= 0, quoteEnd > quoteStart {
                blocks.append(MarkdownBlockRange(range: NSRange(location: quoteStart, length: quoteEnd - quoteStart), isCode: false))
            }
            quoteStart = -1
        }

        func setParagraph(_ range: NSRange, indent: CGFloat, lineHeight: CGFloat? = nil) {
            let style = NSMutableParagraphStyle()
            style.firstLineHeadIndent = indent
            style.headIndent = indent
            if let lineHeight {
                style.minimumLineHeight = lineHeight
                style.maximumLineHeight = lineHeight
            }
            result.addAttribute(.paragraphStyle, value: style, range: range)
        }

        var lineStart = 0
        while lineStart <= ns.length {
            let remaining = ns.length - lineStart
            let nextBreak = ns.range(of: "\n", range: NSRange(location: lineStart, length: remaining))
            let lineEnd = nextBreak.location == NSNotFound ? ns.length : nextBreak.location
            let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)
            let line = ns.substring(with: lineRange) as NSString

            defer { lineStart = nextBreak.location == NSNotFound ? ns.length + 1 : nextBreak.location + 1 }

            if inFence {
                if fence.firstMatch(in: line as String, range: NSRange(location: 0, length: line.length)) != nil {
                    if lineStart > fenceStart {
                        blocks.append(MarkdownBlockRange(range: NSRange(location: fenceStart, length: lineStart - 1 - fenceStart), isCode: true))
                    }
                    result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: lineRange)
                    inFence = false
                } else {
                    result.addAttribute(
                        .font,
                        value: NSFont.monospacedSystemFont(ofSize: palette.baseSize, weight: .regular),
                        range: lineRange,
                    )
                    setParagraph(lineRange, indent: blockIndent)
                }
                continue
            }

            if fence.firstMatch(in: line as String, range: NSRange(location: 0, length: line.length)) != nil {
                closeQuoteRun()
                result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: lineRange)
                fenceStart = lineRange.location + lineRange.length + 1
                inFence = true
                continue
            }

            var contentStart = lineRange.location
            var indent: CGFloat = 0
            if let m = quote.firstMatch(in: line as String, range: NSRange(location: 0, length: line.length)) {
                let markerLength = m.range.length
                contentStart = lineRange.location + markerLength
                result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: NSRange(location: lineRange.location, length: markerLength))
                indent = blockIndent
                if quoteStart < 0 { quoteStart = contentStart }
                quoteEnd = lineRange.location + lineRange.length
            } else {
                closeQuoteRun()
            }

            let contentRange = NSRange(location: contentStart, length: lineRange.location + lineRange.length - contentStart)
            let content = ns.substring(with: contentRange) as NSString

            if let m = heading.firstMatch(in: content as String, range: NSRange(location: 0, length: content.length)) {
                let level = m.range(at: 1).length
                let markerRange = NSRange(location: contentRange.location, length: m.range.length)
                result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: markerRange)
                let size: CGFloat = level == 1 ? 26 : (level == 2 ? 20 : 18)
                let lineHeight: CGFloat = level == 1 ? 36 : (level == 2 ? 32 : 30)
                let textRange = NSRange(location: markerRange.location + markerRange.length, length: contentRange.length - markerRange.length)
                result.addAttributes(
                    [.font: NSFont.custom(FivePadFont.soraBold, size: size)],
                    range: textRange,
                )
                setParagraph(lineRange, indent: indent, lineHeight: lineHeight)
            } else if let m = checkbox.firstMatch(in: content as String, range: NSRange(location: 0, length: content.length)) {
                let markerRange = NSRange(location: contentRange.location, length: m.range.length)
                let statusGroup = m.range(at: 3)
                let checked = content.substring(with: statusGroup).lowercased() == "x"
                let statusRange = NSRange(location: contentRange.location + statusGroup.location, length: max(statusGroup.length, 1))
                result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: markerRange)
                let restRange = NSRange(location: markerRange.location + markerRange.length, length: contentRange.length - markerRange.length)
                if checked {
                    result.addAttributes(
                        [.foregroundColor: palette.mutedMarker, .strikethroughStyle: NSUnderlineStyle.single.rawValue],
                        range: restRange,
                    )
                }
                checkboxes.append(MarkdownCheckboxRange(statusRange: statusRange, lineContentRange: restRange, checked: checked))
                setParagraph(lineRange, indent: indent)
                styleInline(result, in: restRange, ns: ns, palette: palette, links: &links)
            } else {
                setParagraph(lineRange, indent: indent)
                styleInline(result, in: contentRange, ns: ns, palette: palette, links: &links)
            }
        }
        closeQuoteRun()

        return MarkdownScanResult(attributed: result, blocks: blocks, checkboxes: checkboxes, links: links)
    }

    /// Menandai tebal, miring, coret, kode sebaris, dan tautan di dalam satu
    /// rentang baris. Penandanya diredupkan, bukan disembunyikan — isinya
    /// tetap ditebalkan/dimiringkan/dicoret di atas karakter aslinya.
    private static func styleInline(
        _ result: NSMutableAttributedString,
        in range: NSRange,
        ns: NSString,
        palette: MarkdownPalette,
        links: inout [MarkdownLinkRange],
    ) {
        guard range.length > 0 else { return }
        let text = ns.substring(with: range) as NSString
        let full = NSRange(location: 0, length: text.length)

        func apply(_ regex: NSRegularExpression, attrs: [NSAttributedString.Key: Any]) {
            regex.enumerateMatches(in: text as String, range: full) { match, _, _ in
                guard let match else { return }
                let inner = match.range(at: 1)
                let base = range.location
                result.addAttributes(attrs, range: NSRange(location: base + inner.location, length: inner.length))
                let leadLength = inner.location - match.range.location
                if leadLength > 0 {
                    result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: NSRange(location: base + match.range.location, length: leadLength))
                }
                let tailStart = inner.location + inner.length
                let tailLength = match.range.location + match.range.length - tailStart
                if tailLength > 0 {
                    result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: NSRange(location: base + tailStart, length: tailLength))
                }
            }
        }

        apply(boldItalic, attrs: [.font: NSFont.custom(FivePadFont.interBoldItalic, size: palette.baseSize)])
        apply(bold, attrs: [.font: NSFont.custom(FivePadFont.interBold, size: palette.baseSize)])
        apply(italicStar, attrs: [.font: NSFont.custom(FivePadFont.interItalic, size: palette.baseSize)])
        apply(italicUnder, attrs: [.font: NSFont.custom(FivePadFont.interItalic, size: palette.baseSize)])
        apply(strike, attrs: [.strikethroughStyle: NSUnderlineStyle.single.rawValue])
        apply(code, attrs: [
            .font: NSFont.monospacedSystemFont(ofSize: palette.baseSize - 1, weight: .regular),
            .backgroundColor: palette.codeFill,
        ])

        link.enumerateMatches(in: text as String, range: full) { match, _, _ in
            guard let match else { return }
            let base = range.location
            let labelGroup = match.range(at: 1)
            let urlGroup = match.range(at: 2)
            guard urlGroup.location != NSNotFound else { return }
            let url = text.substring(with: urlGroup)
            let labelRange = NSRange(location: base + labelGroup.location, length: labelGroup.length)
            result.addAttributes(
                [.foregroundColor: palette.accent, .underlineStyle: NSUnderlineStyle.single.rawValue],
                range: labelRange,
            )
            let openBracket = NSRange(location: base + match.range.location, length: labelGroup.location - match.range.location)
            let closing = NSRange(
                location: base + labelGroup.location + labelGroup.length,
                length: match.range.location + match.range.length - (labelGroup.location + labelGroup.length),
            )
            result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: openBracket)
            result.addAttribute(.foregroundColor, value: palette.mutedMarker, range: closing)
            links.append(MarkdownLinkRange(labelRange: labelRange, url: url))
        }
    }
}

extension NSFont {
    /// Memuat gaya bernama dari katalog `FivePadFont`, jatuh ke fon sistem
    /// bila belum terdaftar — supaya editor tidak pernah kosong sama sekali.
    static func custom(_ name: String, size: CGFloat) -> NSFont {
        NSFont(name: name, size: size) ?? .systemFont(ofSize: size)
    }
}
