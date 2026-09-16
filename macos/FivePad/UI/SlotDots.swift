import AppKit
import SwiftUI
import UniformTypeIdentifiers

/// Deretan lima titik penanda slot — node 3:86.
///
/// Sama seperti Android: lingkaran 24 pt, jarak 16 pt, yang aktif bercincin
/// tinta 2 pt **di luar** lingkaran sehingga kotak tata letaknya tetap 24 pt dan
/// cincinnya meluber ke celah. Menggambar cincin ke dalam akan memakan warna
/// slot justru pada titik yang paling perlu terlihat.
///
/// Klik memilih slot; klik-kanan membuka tindakan slot (salin, tempel,
/// bagikan, ekspor, riwayat, kosongkan) — padanan tekan-lama titik di
/// Android, dipetakan ke idiom Mac untuk "tindakan lain pada butir ini".
struct SlotDots: View {
    @Environment(\.fivePad) private var colors
    @Bindable var store: Store
    let active: Int
    let onSelect: (Int) -> Void

    @State private var historySlot: Int?
    @State private var clearingSlot: Int?

    var body: some View {
        HStack(spacing: Tokens.dotGap) {
            ForEach(1...Note.slotCount, id: \.self) { slot in
                dot(slot)
                    .contextMenu { menu(for: slot) }
            }
        }
        .sheet(isPresented: Binding(get: { historySlot != nil }, set: { if !$0 { historySlot = nil } })) {
            if let slot = historySlot {
                NoteHistorySheet(store: store, slot: slot)
            }
        }
        .alert(
            "Clear this note?",
            isPresented: Binding(get: { clearingSlot != nil }, set: { if !$0 { clearingSlot = nil } }),
        ) {
            Button("Clear", role: .destructive) {
                if let slot = clearingSlot { store.clearSlot(slot) }
                clearingSlot = nil
            }
            Button("Cancel", role: .cancel) { clearingSlot = nil }
        } message: {
            Text("The current text is saved to History for 30 days.")
        }
    }

    private func dot(_ slot: Int) -> some View {
        let selected = slot == active
        return Circle()
            .fill(colors.slotAccents[slot - 1])
            .overlay(Circle().strokeBorder(colors.dotStroke, lineWidth: 1))
            .opacity(selected ? 1 : dotInactiveAlpha)
            .frame(width: Tokens.dot, height: Tokens.dot)
            .background {
                if selected {
                    Circle()
                        .fill(colors.dotRing)
                        .frame(
                            width: Tokens.dot + Tokens.dotRing * 2,
                            height: Tokens.dot + Tokens.dotRing * 2,
                        )
                }
            }
            .contentShape(Circle())
            .onTapGesture { onSelect(slot) }
            .accessibilityLabel(selected ? "Slot \(slot), active" : "Slot \(slot)")
    }

    @ViewBuilder
    private func menu(for slot: Int) -> some View {
        Button("Copy") { copy(slot) }
        Button("Paste (Append)") { paste(slot) }
        Button("Share…") { share(slot) }
        Button("Export as Markdown…") { export(slot) }
        Divider()
        Button("History…") { historySlot = slot }
        Divider()
        Button("Clear", role: .destructive) { clearingSlot = slot }
    }

    private func copy(_ slot: Int) {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(store.draft(slot: slot), forType: .string)
    }

    private func paste(_ slot: Int) {
        guard let clip = NSPasteboard.general.string(forType: .string), !clip.isEmpty else { return }
        let current = store.draft(slot: slot)
        let combined = current.isEmpty ? clip : current + "\n\n" + clip
        guard combined.count <= Note.maxBodyLength else { return }
        store.updateDraft(slot: slot, body: combined)
        store.saveDraft(slot: slot)
    }

    private func share(_ slot: Int) {
        let picker = NSSharingServicePicker(items: [store.draft(slot: slot)])
        if let view = NSApp.keyWindow?.contentView {
            picker.show(relativeTo: .zero, of: view, preferredEdge: .minY)
        }
    }

    private func export(_ slot: Int) {
        let panel = NSSavePanel()
        let label = store.label(slot: slot)
        panel.nameFieldStringValue = "\(label.isEmpty ? "Note \(slot)" : label).md"
        panel.allowedContentTypes = [.text]
        guard panel.runModal() == .OK, let url = panel.url else { return }
        try? store.draft(slot: slot).write(to: url, atomically: true, encoding: .utf8)
    }
}
