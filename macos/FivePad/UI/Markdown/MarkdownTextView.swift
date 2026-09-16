import AppKit
import SwiftUI

/// Menggambar latar penuh-lebar di belakang blok kutipan dan kode, plus pita
/// aksen 4 pt di tepi kirinya — padanan `drawBehind` yang disinkronkan ke tata
/// letak teks di Android, dipindah ke lapisan `NSLayoutManager` di sini.
final class MarkdownLayoutManager: NSLayoutManager {
    var blocks: [MarkdownBlockRange] = []
    var quoteColor: NSColor = .clear
    var codeColor: NSColor = .clear
    var accentColor: NSColor = .clear

    override func drawBackground(forGlyphRange glyphsToShow: NSRange, at origin: NSPoint) {
        super.drawBackground(forGlyphRange: glyphsToShow, at: origin)
        guard let container = textContainers.first, !blocks.isEmpty else { return }
        let width = container.containerSize.width

        for block in blocks {
            let glyphRange = self.glyphRange(forCharacterRange: block.range, actualCharacterRange: nil)
            let intersection = NSIntersectionRange(glyphRange, glyphsToShow)
            guard intersection.length > 0 || (block.range.length == 0 && glyphsToShow.length > 0) else { continue }
            let fill = block.isCode ? codeColor : quoteColor
            enumerateLineFragments(forGlyphRange: intersection.length > 0 ? intersection : glyphsToShow) { _, usedRect, _, _, _ in
                var fillRect = usedRect
                fillRect.origin.x = origin.x
                fillRect.origin.y += origin.y
                fillRect.size.width = width
                fill.setFill()
                NSBezierPath(rect: fillRect).fill()
                var barRect = fillRect
                barRect.size.width = 4
                self.accentColor.setFill()
                NSBezierPath(rect: barRect).fill()
            }
        }
    }
}

/// `NSTextView` yang menambahkan tiga hal di atas perilaku bawaan: Enter yang
/// melanjutkan daftar, penekanan yang menutup sendiri saat mengetik menembus
/// penanda penutupnya, dan ketukan pada kotak centang/tautan. Semuanya
/// beroperasi langsung pada `textStorage`, dengan rentang seminimal mungkin
/// supaya riwayat undo tetap granular.
final class MarkdownNSTextView: NSTextView {
    var checkboxes: [MarkdownCheckboxRange] = []
    var links: [MarkdownLinkRange] = []
    var onCheckboxToggle: ((NSRange) -> Void)?
    var onLinkOpen: ((String) -> Void)?

    private var mouseDownCharIndex: Int?

    override func mouseDown(with event: NSEvent) {
        mouseDownCharIndex = charIndex(for: event)
        super.mouseDown(with: event)
    }

    /// Diknsumsi hanya pada angkat-jari, bukan turun-jari (FR-1.18) — gestur
    /// yang ternyata sebuah seret-pilih harus tetap berjalan normal.
    override func mouseUp(with event: NSEvent) {
        super.mouseUp(with: event)
        let upIndex = charIndex(for: event)
        guard let downIndex = mouseDownCharIndex, downIndex == upIndex, let index = upIndex else { return }
        if let box = checkboxes.first(where: { NSLocationInRange(index, padded($0.statusRange)) }) {
            onCheckboxToggle?(box.statusRange)
            return
        }
        if let hit = links.first(where: { NSLocationInRange(index, $0.labelRange) }) {
            onLinkOpen?(hit.url)
        }
    }

    private func padded(_ range: NSRange) -> NSRange {
        NSRange(location: max(range.location - 1, 0), length: range.length + 2)
    }

    private func charIndex(for event: NSEvent) -> Int? {
        guard let lm = layoutManager, let tc = textContainer, lm.numberOfGlyphs > 0 else { return nil }
        let point = convert(event.locationInWindow, from: nil)
        let containerPoint = NSPoint(x: point.x - textContainerOrigin.x, y: point.y - textContainerOrigin.y)
        let glyphIndex = lm.glyphIndex(for: containerPoint, in: tc)
        guard glyphIndex < lm.numberOfGlyphs else { return nil }
        let rect = lm.boundingRect(forGlyphRange: NSRange(location: glyphIndex, length: 1), in: tc)
        guard rect.contains(containerPoint) else { return nil }
        return lm.characterIndexForGlyph(at: glyphIndex)
    }

    override func insertNewline(_ sender: Any?) {
        let range = selectedRange()
        if range.length == 0 {
            if let edit = closeEmphasisOnBreak(string, caret: range.location, typed: "\n") {
                apply(edit)
                return
            }
            if let edit = continueListOnNewline(string, caret: range.location) {
                apply(edit)
                return
            }
        }
        super.insertNewline(sender)
    }

    override func insertText(_ insertString: Any, replacementRange: NSRange) {
        if replacementRange.location == NSNotFound, selectedRange().length == 0,
           let typed = insertString as? String, typed == " ",
           let edit = closeEmphasisOnBreak(string, caret: selectedRange().location, typed: " ") {
            apply(edit)
            return
        }
        super.insertText(insertString, replacementRange: replacementRange)
    }

    /// Menerapkan sunting sekecil mungkin lewat `shouldChangeText`/`didChangeText`
    /// — bukan mengganti seluruh isi — supaya undo tetap granular dan tata
    /// letak tidak perlu dihitung ulang lebih dari yang berubah.
    func apply(_ edit: MarkdownEdit) {
        let old = string as NSString
        let new = edit.text as NSString
        let maxCommon = min(old.length, new.length)
        var prefix = 0
        while prefix < maxCommon, old.character(at: prefix) == new.character(at: prefix) { prefix += 1 }
        var suffix = 0
        let maxSuffix = maxCommon - prefix
        while suffix < maxSuffix,
              old.character(at: old.length - 1 - suffix) == new.character(at: new.length - 1 - suffix) {
            suffix += 1
        }
        let oldRange = NSRange(location: prefix, length: old.length - prefix - suffix)
        let replacement = new.substring(with: NSRange(location: prefix, length: new.length - prefix - suffix))
        guard shouldChangeText(in: oldRange, replacementString: replacement) else { return }
        textStorage?.replaceCharacters(in: oldRange, with: replacement)
        didChangeText()
        setSelectedRange(edit.selection)
        scrollRangeToVisible(edit.selection)
    }
}

/// Editor catatan bermarkah langsung — padanan `NotesPane`'s `BasicTextField`
/// + `MarkdownVisualTransformation` di Android, dibangun di atas `NSTextView`
/// mentah karena SwiftUI's `TextEditor` tidak memberi kait yang dibutuhkan
/// untuk mencegat Enter, mendeteksi ketukan pada kotak centang/tautan, atau
/// menggambar latar blok penuh-lebar.
///
/// Beda dari Android: penanda Markdown **diredupkan, bukan disembunyikan** —
/// lihat catatan panjang di `MarkdownScanner` untuk alasannya. Ini berarti
/// teks tampil selalu sama persis dengan teks sumber, karakter demi karakter,
/// jadi posisi kursor tidak pernah perlu dipetakan ulang antara keduanya.
struct MarkdownTextView: NSViewRepresentable {
    @Binding var text: String
    /// `true` = tampilan Markdown mentah (monospace, tanpa gaya). `false` =
    /// tampilan biasa yang diformat.
    var sourceMode: Bool
    var palette: MarkdownPalette
    /// Tindakan dari lembar format atau lembar tautan, diterapkan pada
    /// seleksi aktif lalu dianggap selesai lewat `onActionHandled`.
    var pendingOperation: MarkdownPendingOperation?
    var onActionHandled: () -> Void = {}
    var onLinkOpen: (String) -> Void = { _ in }
    /// Dipanggil saat tindakan Tautan dipilih tapi seleksinya **bukan**
    /// tautan utuh — pemanggil membuka `LinkSheet` dengan label ini terisi
    /// lebih dulu, lalu kembali lewat `pendingOperation = .insertLink(...)`.
    var onRequestLink: (String) -> Void = { _ in }
    /// Dinaikkan pemanggil setiap kali editor harus mengambil fokus —
    /// padanan `onChange(of: slot) { editorFocused = true }` yang lama.
    var focusSignal: Int = 0

    func makeCoordinator() -> Coordinator { Coordinator(text: $text) }

    func makeNSView(context: Context) -> NSScrollView {
        let layoutManager = MarkdownLayoutManager()
        let textContainer = NSTextContainer(size: NSSize(width: 0, height: CGFloat.greatestFiniteMagnitude))
        textContainer.widthTracksTextView = true
        layoutManager.addTextContainer(textContainer)
        let textStorage = NSTextStorage()
        textStorage.addLayoutManager(layoutManager)

        let textView = MarkdownNSTextView(frame: .zero, textContainer: textContainer)
        textView.delegate = context.coordinator
        textView.isEditable = true
        textView.isSelectable = true
        textView.isRichText = true
        textView.allowsUndo = true
        textView.isAutomaticQuoteSubstitutionEnabled = false
        textView.isAutomaticDashSubstitutionEnabled = false
        textView.isAutomaticTextReplacementEnabled = false
        textView.isAutomaticLinkDetectionEnabled = false
        textView.textContainerInset = NSSize(width: 0, height: Tokens.space3)
        textView.textContainer?.lineFragmentPadding = 0
        textView.drawsBackground = false
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = false
        textView.autoresizingMask = [NSView.AutoresizingMask.width]
        textView.onCheckboxToggle = { [weak textView, coordinator = context.coordinator] statusRange in
            guard let textView else { return }
            let newText = toggleCheckbox(textView.string, statusRange)
            coordinator.applyEdit(MarkdownEdit(text: newText, selection: textView.selectedRange()), on: textView)
        }

        let scrollView = NSScrollView()
        scrollView.hasVerticalScroller = true
        scrollView.hasHorizontalScroller = false
        scrollView.autohidesScrollers = true
        scrollView.drawsBackground = false
        scrollView.documentView = textView

        context.coordinator.hostedTextView = textView
        context.coordinator.sync(text: text, sourceMode: sourceMode, palette: palette)
        return scrollView
    }

    func updateNSView(_ scrollView: NSScrollView, context: Context) {
        guard let textView = context.coordinator.hostedTextView else { return }
        textView.onLinkOpen = onLinkOpen
        context.coordinator.sync(text: text, sourceMode: sourceMode, palette: palette)

        if let op = pendingOperation {
            switch op {
            case .action(let action) where action == .link:
                let selection = textView.selectedRange()
                if selectedLink(textView.string, selection) != nil {
                    context.coordinator.applyEdit(unlink(textView.string, selection), on: textView)
                } else {
                    let ns = textView.string as NSString
                    let label = selection.length > 0 ? ns.substring(with: selection) : ""
                    onRequestLink(label)
                }
            case .action(let action):
                let edit = applyMarkdown(textView.string, textView.selectedRange(), action)
                context.coordinator.applyEdit(edit, on: textView)
            case .insertLink(let label, let url):
                let edit = insertLink(textView.string, textView.selectedRange(), label: label, url: url)
                context.coordinator.applyEdit(edit, on: textView)
            }
            DispatchQueue.main.async { onActionHandled() }
        }

        if focusSignal != context.coordinator.lastFocusSignal {
            context.coordinator.lastFocusSignal = focusSignal
            DispatchQueue.main.async { textView.window?.makeFirstResponder(textView) }
        }
    }

    @MainActor
    final class Coordinator: NSObject, NSTextViewDelegate {
        private let textBinding: Binding<String>
        private var lastPushed = ""
        private var lastSourceMode = false
        private var currentPalette: MarkdownPalette?
        weak var hostedTextView: MarkdownNSTextView?
        var lastFocusSignal = 0

        init(text: Binding<String>) { textBinding = text }

        func textDidChange(_ notification: Notification) {
            guard let tv = notification.object as? MarkdownNSTextView, let palette = currentPalette else { return }
            lastPushed = tv.string
            textBinding.wrappedValue = tv.string
            restyle(tv, sourceMode: lastSourceMode, palette: palette, preserveSelection: true)
        }

        /// Hanya menyentuh isi bila teksnya benar-benar berubah dari luar,
        /// atau mode/temanya berganti — bukan gema dari `textDidChange`
        /// sendiri. Padanan penjaga `echoed` di Android, dipindah satu lapis
        /// ke batas SwiftUI<->AppKit karena `updateNSView` bisa terpicu oleh
        /// render ulang yang tidak terkait sama sekali.
        func sync(text: String, sourceMode: Bool, palette: MarkdownPalette) {
            guard let tv = hostedTextView else { return }
            let externalChange = text != lastPushed
            let modeChanged = sourceMode != lastSourceMode || currentPalette == nil
            currentPalette = palette
            lastSourceMode = sourceMode
            guard externalChange || modeChanged else { return }
            lastPushed = text
            if externalChange {
                tv.string = text
                restyle(tv, sourceMode: sourceMode, palette: palette, preserveSelection: false)
            } else {
                restyle(tv, sourceMode: sourceMode, palette: palette, preserveSelection: true)
            }
        }

        func applyEdit(_ edit: MarkdownEdit, on tv: MarkdownNSTextView) {
            guard currentPalette != nil else { return }
            // Lewat jalur perubahan NSTextView biasa agar satu aksi format
            // tetap satu langkah Undo. Mengganti `tv.string` langsung akan
            // membuang riwayat undo editor.
            tv.apply(edit)
        }

        private func restyle(_ tv: MarkdownNSTextView, sourceMode: Bool, palette: MarkdownPalette, preserveSelection: Bool) {
            let selection = tv.selectedRange()
            let full = NSRange(location: 0, length: (tv.string as NSString).length)

            if sourceMode {
                let attr = NSMutableAttributedString(string: tv.string)
                let style = NSMutableParagraphStyle()
                style.minimumLineHeight = Tokens.bodyLineHeight
                style.maximumLineHeight = Tokens.bodyLineHeight
                attr.addAttributes(
                    [
                        .font: NSFont.monospacedSystemFont(ofSize: Tokens.bodyTextSize, weight: .regular),
                        .foregroundColor: palette.ink,
                        .paragraphStyle: style,
                    ],
                    range: full,
                )
                tv.textStorage?.setAttributedString(attr)
                tv.checkboxes = []
                tv.links = []
                (tv.layoutManager as? MarkdownLayoutManager)?.blocks = []
            } else {
                let scan = MarkdownScanner.scan(tv.string, palette: palette)
                tv.textStorage?.setAttributedString(scan.attributed)
                tv.checkboxes = scan.checkboxes
                tv.links = scan.links
                if let lm = tv.layoutManager as? MarkdownLayoutManager {
                    lm.blocks = scan.blocks
                    lm.quoteColor = palette.quoteFill
                    lm.codeColor = palette.codeFill
                    lm.accentColor = palette.accent
                }
            }

            if preserveSelection {
                let length = (tv.string as NSString).length
                let location = min(selection.location, length)
                let len = min(selection.length, max(length - location, 0))
                tv.setSelectedRange(NSRange(location: location, length: len))
            }
            tv.needsDisplay = true
        }
    }
}
