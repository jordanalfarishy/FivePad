import Foundation

/// Tindakan penyuntingan teks pada catatan. Padanan `MarkdownAction` di
/// Android — sebelas tindakan, tidak lebih, karena setiap baris tambahan di
/// lembar format memakan tinggi yang menutupi catatan yang sedang disunting.
enum MarkdownAction: String, CaseIterable, Identifiable {
    case header, subHeader, bold, italic, strike, list, orderedList, todo, quote, code, link

    var id: String { rawValue }

    var syntax: String {
        switch self {
        case .header: "#"
        case .subHeader: "##"
        case .bold: "**"
        case .italic: "*"
        case .strike: "~~"
        case .list: "-"
        case .orderedList: "1."
        case .todo: "- [ ]"
        case .quote: ">"
        case .code: "`"
        case .link: "[ ]( )"
        }
    }

    var label: String {
        switch self {
        case .header: "Header"
        case .subHeader: "Sub header"
        case .bold: "Bold"
        case .italic: "Italic"
        case .strike: "Strike"
        case .list: "List"
        case .orderedList: "Ordered List"
        case .todo: "To do"
        case .quote: "Quote"
        case .code: "Code"
        case .link: "Link"
        }
    }
}

/// Hasil satu tindakan penyuntingan: teks baru dan seleksi barunya.
struct MarkdownEdit {
    let text: String
    let selection: NSRange
}

/// Operasi yang menunggu diterapkan oleh `MarkdownTextView` pada seleksi
/// aktifnya. `.insertLink` datang belakangan, setelah `LinkSheet` menjawab
/// permintaan `onRequestLink`.
enum MarkdownPendingOperation: Equatable {
    case action(MarkdownAction)
    case insertLink(label: String, url: String)
}

/// Menerapkan [action] pada [text]/[selection], menghormati seleksi yang
/// sedang aktif. Padanan `applyMarkdown` di Android — semua tindakan bersifat
/// **membalik**: menerapkannya pada teks yang sudah memakainya justru
/// mencabutnya kembali.
func applyMarkdown(_ text: String, _ selection: NSRange, _ action: MarkdownAction) -> MarkdownEdit {
    switch action {
    case .bold: wrap(text, selection, "**")
    case .italic: wrap(text, selection, "*")
    case .strike: wrap(text, selection, "~~")
    case .code: wrap(text, selection, "`")
    case .header: heading(text, selection, level: 1)
    case .subHeader: heading(text, selection, level: 2)
    case .quote: quoteLines(text, selection)
    case .list: prefixLines(text, selection, prefix: "- ", alternates: ["* ", "+ "])
    case .orderedList: numberLines(text, selection)
    case .todo: prefixLines(text, selection, prefix: "- [ ] ", alternates: ["- [x] ", "- [] "])
    // Tautan tidak pernah sampai ke sini: alamatnya ditanyakan lebih dulu lewat LinkSheet.
    case .link: MarkdownEdit(text: text, selection: selection)
    }
}

// MARK: - Tautan

private let wholeLinkRegex = try! NSRegularExpression(pattern: #"^\[([^\]\n]*)]\(([^)\n]*)\)$"#)

/// Tautan yang sedang terseleksi, sebagai pasangan label dan alamat — dipakai
/// untuk mengisi lebih dulu lembar alamat, dan untuk memutuskan apakah
/// tindakan Tautan kali ini membongkar tautan yang sudah ada.
func selectedLink(_ text: String, _ selection: NSRange) -> (label: String, url: String)? {
    let ns = text as NSString
    guard selection.location + selection.length <= ns.length else { return nil }
    let selected = ns.substring(with: selection) as NSString
    guard let m = wholeLinkRegex.firstMatch(in: selected as String, range: NSRange(location: 0, length: selected.length)) else {
        return nil
    }
    return (selected.substring(with: m.range(at: 1)), selected.substring(with: m.range(at: 2)))
}

/// Membongkar tautan yang terseleksi kembali menjadi labelnya saja.
func unlink(_ text: String, _ selection: NSRange) -> MarkdownEdit {
    guard let link = selectedLink(text, selection) else { return MarkdownEdit(text: text, selection: selection) }
    let ns = text as NSString
    let newText = ns.replacingCharacters(in: selection, with: link.label)
    return MarkdownEdit(text: newText, selection: NSRange(location: selection.location, length: (link.label as NSString).length))
}

/// Menyisipkan tautan dengan label dan alamat yang sudah ditentukan lewat
/// `LinkSheet` — bukan diketik langsung ke catatan.
func insertLink(_ text: String, _ selection: NSRange, label: String, url: String) -> MarkdownEdit {
    let inserted = "[\(label)](\(url))"
    let ns = text as NSString
    let newText = ns.replacingCharacters(in: selection, with: inserted)
    let insertedLength = (inserted as NSString).length
    return MarkdownEdit(text: newText, selection: NSRange(location: selection.location + insertedLength, length: 0))
}

// MARK: - Kotak centang

/// Membalik status satu kotak centang. `statusRange` menunjuk tepat pada
/// karakter status (spasi atau `x`) — operasi sepanjang tetap, jadi kursor
/// di tempat lain tidak pernah perlu dipetakan ulang.
func toggleCheckbox(_ text: String, _ statusRange: NSRange) -> String {
    let ns = NSMutableString(string: text)
    guard statusRange.location + statusRange.length <= ns.length else { return text }
    let checked = ns.substring(with: statusRange).lowercased() == "x"
    ns.replaceCharacters(in: statusRange, with: checked ? " " : "x")
    return ns as String
}

// MARK: - Enter melanjutkan daftar (FR-1.16)

private let todoLineRegex = try! NSRegularExpression(pattern: #"^(\s*)([-*+])\s+\[[ xX]?]\s*(.*)$"#)
private let orderedLineRegex = try! NSRegularExpression(pattern: #"^(\s*)(\d+)\.\s+(.*)$"#)
private let bulletLineRegex = try! NSRegularExpression(pattern: #"^(\s*)([-*+])\s+(.*)$"#)

/// Dipanggil dari `insertNewline(_:)`, sebelum baris baru sungguhan
/// disisipkan — bukan lewat perbandingan sebelum/sesudah seperti Android,
/// karena AppKit memberi kait langsung pada tombol Enter. Mengembalikan
/// `nil` bila barisnya bukan bagian dari daftar/kutipan, sehingga
/// pemanggilnya cukup melanjutkan perilaku normal.
func continueListOnNewline(_ text: String, caret: Int) -> MarkdownEdit? {
    let ns = text as NSString
    guard let lineStart = lineStartIndex(ns, before: caret) else { return nil }
    let line = ns.substring(with: NSRange(location: lineStart, length: caret - lineStart))
    let lineNS = line as NSString
    let full = NSRange(location: 0, length: lineNS.length)

    let quoteMatch = quoteLeadRegex.firstMatch(in: line, range: full)
    let todoMatch = todoLineRegex.firstMatch(in: line, range: full)
    let orderedMatch = todoMatch == nil ? orderedLineRegex.firstMatch(in: line, range: full) : nil
    let bulletMatch = (todoMatch == nil && orderedMatch == nil) ? bulletLineRegex.firstMatch(in: line, range: full) : nil

    let prefix: String
    let content: String
    if let m = todoMatch {
        // Butir tugas baru selalu lahir belum tercentang, apa pun status butir di atasnya.
        prefix = lineNS.substring(with: m.range(at: 1)) + lineNS.substring(with: m.range(at: 2)) + " [ ] "
        content = lineNS.substring(with: m.range(at: 3))
    } else if let m = orderedMatch {
        let number = Int(lineNS.substring(with: m.range(at: 2))) ?? 0
        prefix = lineNS.substring(with: m.range(at: 1)) + "\(number + 1). "
        content = lineNS.substring(with: m.range(at: 3))
    } else if let m = bulletMatch {
        prefix = lineNS.substring(with: m.range(at: 1)) + lineNS.substring(with: m.range(at: 2)) + " "
        content = lineNS.substring(with: m.range(at: 3))
    } else if let m = quoteMatch {
        prefix = lineNS.substring(with: m.range)
        content = nsDrop(line, m.range.length)
    } else {
        return nil
    }

    if content.trimmingCharacters(in: .whitespaces).isEmpty {
        // Penandanya dicabut bersama baris baru yang barusan dibuat — itulah
        // cara mengakhiri daftar, karena penandanya tidak terlihat untuk
        // dihapus manual di tampilan biasa.
        let newText = ns.replacingCharacters(in: NSRange(location: lineStart, length: caret - lineStart), with: "")
        return MarkdownEdit(text: newText, selection: NSRange(location: lineStart, length: 0))
    }

    let insertion = "\n" + prefix
    let newText = ns.replacingCharacters(in: NSRange(location: caret, length: 0), with: insertion)
    let newCaret = caret + (insertion as NSString).length
    return MarkdownEdit(text: newText, selection: NSRange(location: newCaret, length: 0))
}

// MARK: - Penekanan berhenti di spasi (FR-1.21)

private let emphasisMarkers = ["***", "**", "~~", "*"]

/// Dipanggil sebelum spasi (dari `insertText`) atau baris baru (dari
/// `insertNewline`) sungguhan disisipkan. Bila kursor persis di depan sepasang
/// penanda penekanan yang sudah dibuka lebih dulu di baris yang sama, karakter
/// yang diketik dipindah ke seberang penanda penutup itu — begitulah
/// penekanan "ditutup" saat mengetik menembusnya, bukan di dalamnya.
func closeEmphasisOnBreak(_ text: String, caret: Int, typed: String) -> MarkdownEdit? {
    let ns = text as NSString
    guard let lineStart = lineStartIndex(ns, before: caret) else { return nil }
    for marker in emphasisMarkers {
        let markerLength = (marker as NSString).length
        guard caret + markerLength <= ns.length else { continue }
        guard ns.substring(with: NSRange(location: caret, length: markerLength)) == marker else { continue }
        let before = ns.substring(with: NSRange(location: lineStart, length: caret - lineStart))
        guard before.contains(marker) else { continue }
        let past = caret + markerLength
        let newText = ns.replacingCharacters(in: NSRange(location: past, length: 0), with: typed)
        let newCaret = past + (typed as NSString).length
        return MarkdownEdit(text: newText, selection: NSRange(location: newCaret, length: 0))
    }
    return nil
}

// MARK: - Bantuan bersama

private let quoteLeadRegex = try! NSRegularExpression(pattern: #"^\s*>\s?"#)
private let anyHeadingRegex = try! NSRegularExpression(pattern: #"^#{1,6}\s+"#)
private let numberPrefixRegex = try! NSRegularExpression(pattern: #"^\d+\.\s"#)

private func leadOf(_ line: String) -> String {
    let ns = line as NSString
    guard let m = quoteLeadRegex.firstMatch(in: line, range: NSRange(location: 0, length: ns.length)) else { return "" }
    return ns.substring(with: m.range)
}

private func nsDrop(_ line: String, _ n: Int) -> String {
    let ns = line as NSString
    guard n < ns.length else { return "" }
    return ns.substring(from: n)
}

private func containsMatch(_ regex: NSRegularExpression, _ line: String) -> Bool {
    regex.firstMatch(in: line, range: NSRange(location: 0, length: (line as NSString).length)) != nil
}

private func replaceFirst(_ regex: NSRegularExpression, in line: String, with replacement: String) -> String {
    let ns = line as NSString
    guard let m = regex.firstMatch(in: line, range: NSRange(location: 0, length: ns.length)) else { return line }
    return ns.replacingCharacters(in: m.range, with: replacement)
}

private func lineStartIndex(_ ns: NSString, before caret: Int) -> Int? {
    guard caret > 0 else { return 0 }
    let found = ns.range(of: "\n", options: .backwards, range: NSRange(location: 0, length: caret))
    return found.location == NSNotFound ? 0 : found.location + 1
}

/// Rentang baris — dari awal baris pertama sampai akhir baris terakhir —
/// yang disentuh seleksi. Akhirnya eksklusif, sama seperti pemakaiannya di
/// Android lewat `substring(span.first, span.last)`.
private func lineSpan(_ text: String, _ selection: NSRange) -> NSRange {
    let ns = text as NSString
    let selMin = selection.location
    let selMax = selection.location + selection.length

    var start = 0
    if selMin != 0 {
        let searchFrom = max(selMin - 1, 0)
        let found = ns.range(of: "\n", options: .backwards, range: NSRange(location: 0, length: searchFrom + 1))
        if found.location != NSNotFound { start = found.location + 1 }
    }

    let lastSelected = selection.length == 0 ? selMax : selMax - 1
    var end = ns.length
    if lastSelected < ns.length {
        let found = ns.range(of: "\n", range: NSRange(location: lastSelected, length: ns.length - lastSelected))
        if found.location != NSNotFound { end = found.location }
    }
    return NSRange(location: start, length: max(end - start, 0))
}

/// Mengganti [span] dengan [replacement] dan memetakan ulang posisi seleksi
/// lama ke posisi barunya — padanan `replaceSpan` di Android. Dibutuhkan
/// karena awalan baris (`#`, `- `, `> `, ...) bisa berbeda panjang sebelum
/// dan sesudah, jadi kursor tidak bisa sekadar mengikuti offset lama.
private func replaceSpan(_ text: String, _ selection: NSRange, _ span: NSRange, _ replacement: String) -> MarkdownEdit {
    let ns = text as NSString
    let spanEnd = span.location + span.length
    let original = ns.substring(with: span)
    let oldLines = original.components(separatedBy: "\n")
    let newLines = replacement.components(separatedBy: "\n")
    let replacementLength = (replacement as NSString).length
    let originalLength = (original as NSString).length

    func remap(_ position: Int) -> Int {
        if position < span.location { return position }
        if position > spanEnd { return position + replacementLength - originalLength }
        var oldStart = span.location
        var newStart = span.location
        for index in 0..<oldLines.count {
            let old = oldLines[index] as NSString
            let new = (index < newLines.count ? newLines[index] : "") as NSString
            if position <= oldStart + old.length {
                let shared = commonSuffixLength(old, new)
                let oldPrefix = old.length - shared
                let newPrefix = new.length - shared
                let column = position - oldStart
                return newStart + (column >= oldPrefix ? column - oldPrefix + newPrefix : newPrefix)
            }
            oldStart += old.length + 1
            newStart += new.length + 1
        }
        return span.location + replacementLength
    }

    let newText = ns.replacingCharacters(in: span, with: replacement)
    let newStart = remap(selection.location)
    let newEnd = remap(selection.location + selection.length)
    return MarkdownEdit(text: newText, selection: NSRange(location: newStart, length: max(newEnd - newStart, 0)))
}

private func commonSuffixLength(_ a: NSString, _ b: NSString) -> Int {
    var count = 0
    while count < a.length, count < b.length, a.character(at: a.length - 1 - count) == b.character(at: b.length - 1 - count) {
        count += 1
    }
    return count
}

// MARK: - Tindakan garis dan bungkus

/// Membungkus seleksi dengan [marker], atau mencabutnya bila sudah
/// terbungkus. Tanpa seleksi, penandanya tetap disisipkan dan kursor
/// mendarat di antaranya.
private func wrap(_ text: String, _ selection: NSRange, _ marker: String) -> MarkdownEdit {
    let ns = NSMutableString(string: text)
    let start = selection.location
    let end = selection.location + selection.length
    let len = (marker as NSString).length

    func char(at index: Int) -> unichar? {
        guard index >= 0, index < ns.length else { return nil }
        return ns.character(at: index)
    }

    let star: unichar = 42 // '*'
    // Satu bintang di kiri-kanan seleksi belum tentu penanda miring — bisa
    // jadi separuh dari penanda tebal yang mengapitnya.
    let neighbours = marker != "*" || (char(at: start - 2) != star && char(at: end + 1) != star)
    let wrappedOutside = neighbours && start >= len && end + len <= ns.length
        && ns.substring(with: NSRange(location: start - len, length: len)) == marker
        && ns.substring(with: NSRange(location: end, length: len)) == marker

    if wrappedOutside {
        ns.deleteCharacters(in: NSRange(location: end, length: len))
        ns.deleteCharacters(in: NSRange(location: start - len, length: len))
        return MarkdownEdit(text: ns as String, selection: NSRange(location: start - len, length: end - start))
    }

    let selected = ns.substring(with: NSRange(location: start, length: end - start))
    let selectedNS = selected as NSString
    let wrappedInside = selectedNS.length >= len * 2
        && selectedNS.substring(to: len) == marker
        && selectedNS.substring(from: selectedNS.length - len) == marker
    if wrappedInside {
        let inner = selectedNS.substring(with: NSRange(location: len, length: selectedNS.length - len * 2))
        ns.replaceCharacters(in: NSRange(location: start, length: end - start), with: inner)
        return MarkdownEdit(text: ns as String, selection: NSRange(location: start, length: (inner as NSString).length))
    }

    ns.replaceCharacters(in: NSRange(location: start, length: end - start), with: marker + selected + marker)
    let newSelection = start == end
        ? NSRange(location: start + len, length: 0)
        : NSRange(location: start + len, length: end - start)
    return MarkdownEdit(text: ns as String, selection: newSelection)
}

/// Kutipan selalu ditulis terluar — tidak dilewati saat memutuskan mencabutnya.
private func quoteLines(_ text: String, _ selection: NSRange) -> MarkdownEdit {
    let span = lineSpan(text, selection)
    let ns = text as NSString
    let lines = ns.substring(with: span).components(separatedBy: "\n")
    let allQuoted = lines.allSatisfy { containsMatch(quoteLeadRegex, $0) }
    let updated = lines.map { line -> String in
        if allQuoted {
            replaceFirst(quoteLeadRegex, in: line, with: "")
        } else if containsMatch(quoteLeadRegex, line) {
            line
        } else {
            "> " + line
        }
    }
    return replaceSpan(text, selection, span, updated.joined(separator: "\n"))
}

/// Menjadikan baris terpilih judul bertingkat [level]. Tingkat yang sudah
/// sama dicabut; tingkat yang berbeda **diganti**, bukan ditumpuk.
private func heading(_ text: String, _ selection: NSRange, level: Int) -> MarkdownEdit {
    let prefix = String(repeating: "#", count: level) + " "
    let span = lineSpan(text, selection)
    let ns = text as NSString
    let lines = ns.substring(with: span).components(separatedBy: "\n")
    let hasAll = lines.allSatisfy { line in
        let lead = leadOf(line)
        return nsDrop(line, (lead as NSString).length).hasPrefix(prefix)
    }
    let updated = lines.map { line -> String in
        let lead = leadOf(line)
        let rest = replaceFirst(anyHeadingRegex, in: nsDrop(line, (lead as NSString).length), with: "")
        return hasAll ? lead + rest : lead + prefix + rest
    }
    return replaceSpan(text, selection, span, updated.joined(separator: "\n"))
}

/// Menambahkan [prefix] ke setiap baris terpilih, atau mencabutnya bila
/// **semua** baris sudah memilikinya.
private func prefixLines(_ text: String, _ selection: NSRange, prefix: String, alternates: [String] = []) -> MarkdownEdit {
    let span = lineSpan(text, selection)
    let ns = text as NSString
    let lines = ns.substring(with: span).components(separatedBy: "\n")
    let all = [prefix] + alternates
    let hasAll = lines.allSatisfy { line in
        let lead = leadOf(line)
        let rest = nsDrop(line, (lead as NSString).length)
        return all.contains { rest.hasPrefix($0) }
    }
    let updated = lines.map { line -> String in
        let lead = leadOf(line)
        let rest = nsDrop(line, (lead as NSString).length)
        if hasAll {
            if let matched = all.first(where: { rest.hasPrefix($0) }) {
                return lead + nsDrop(rest, (matched as NSString).length)
            }
            return lead + rest
        }
        return lead + prefix + rest
    }
    return replaceSpan(text, selection, span, updated.joined(separator: "\n"))
}

/// Menomori baris terpilih, atau mencabut nomornya bila semuanya sudah bernomor.
private func numberLines(_ text: String, _ selection: NSRange) -> MarkdownEdit {
    let span = lineSpan(text, selection)
    let ns = text as NSString
    let lines = ns.substring(with: span).components(separatedBy: "\n")
    let hasAll = lines.allSatisfy { line in
        let lead = leadOf(line)
        return containsMatch(numberPrefixRegex, nsDrop(line, (lead as NSString).length))
    }
    let updated = lines.enumerated().map { index, line -> String in
        let lead = leadOf(line)
        let rest = nsDrop(line, (lead as NSString).length)
        if hasAll {
            return lead + replaceFirst(numberPrefixRegex, in: rest, with: "")
        }
        return "\(lead)\(index + 1). \(rest)"
    }
    return replaceSpan(text, selection, span, updated.joined(separator: "\n"))
}
