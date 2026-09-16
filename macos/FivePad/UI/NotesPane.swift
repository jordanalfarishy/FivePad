import AppKit
import SwiftUI

struct NotesPane: View {
    @Environment(\.fivePad) private var colors
    @Bindable var store: Store
    let slot: Int
    var autofocus = false

    /// Berlaku untuk kelima slot sekaligus — ini soal cara membaca, bukan isi
    /// catatannya — sama seperti `AppPreferences.markdownView` di Android.
    @AppStorage("markdownView") private var markdownView = false

    @State private var pendingOperation: MarkdownPendingOperation?
    @State private var showFormatSheet = false
    @State private var showLinkSheet = false
    @State private var linkInitialLabel = ""
    @State private var focusSignal = 0

    private var note: Note? { store.notes.first { $0.slot == slot } }
    private var accent: Color { colors.slotAccents[slot - 1] }

    private var label: Binding<String> {
        Binding(
            get: { store.label(slot: slot) },
            set: { store.updateLabel(slot: slot, label: $0) },
        )
    }

    private var bodyText: Binding<String> {
        Binding(
            get: { store.draft(slot: slot) },
            set: { store.updateDraft(slot: slot, body: $0) },
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: Tokens.space3) {
                TextField("Note \(slot)", text: label)
                    .textFieldStyle(.plain)
                    .multilineTextAlignment(.center)
                    .fivePadStyle(FivePadText.headerName)
                    .foregroundStyle(accent)

                Spacer()

                if bodyText.wrappedValue.count >= Note.bodyWarnLength {
                    Text("\(bodyText.wrappedValue.count.formatted()) / \(Note.maxBodyLength.formatted())")
                        .fivePadStyle(FivePadText.meta)
                        .monospacedDigit()
                        .foregroundStyle(colors.muted)
                }

                Button { showFormatSheet = true } label: {
                    Image(systemName: "textformat")
                        .foregroundStyle(colors.muted)
                }
                .buttonStyle(.plain)
                .help("Format")
            }
            .padding(.horizontal, Tokens.screenPadding)
            .frame(height: Tokens.titleRowHeight + Tokens.space3)
            .background(colors.bar)

            MarkdownTextView(
                text: bodyText,
                sourceMode: markdownView,
                palette: MarkdownPalette(colors: colors, slotAccent: NSColor(accent)),
                pendingOperation: pendingOperation,
                onActionHandled: { pendingOperation = nil },
                onLinkOpen: openLink,
                onRequestLink: { initial in
                    linkInitialLabel = initial
                    showLinkSheet = true
                },
                focusSignal: focusSignal,
            )
            .padding(.horizontal, Tokens.screenPadding)
            .background(colors.background)
            .accessibilityLabel("Note \(slot) editor")
        }
        .onDisappear { store.saveDraft(slot: slot) }
        .onChange(of: slot) { oldSlot, _ in
            store.saveDraft(slot: oldSlot)
            if autofocus { focusSignal += 1 }
        }
        .onAppear {
            if autofocus { focusSignal += 1 }
        }
        .overlay {
            if note == nil {
                ProgressView().controlSize(.small)
            }
        }
        .overlay(alignment: .bottom) {
            if store.clearedSlot?.slot == slot {
                UndoBanner(message: "Note cleared", onUndo: store.undoClearSlot)
            }
        }
        .animation(.default, value: store.clearedSlot)
        .sheet(isPresented: $showFormatSheet) {
            FormatSheet(markdownView: $markdownView, accent: accent) { action in
                pendingOperation = .action(action)
            }
        }
        .sheet(isPresented: $showLinkSheet) {
            LinkSheet(initialLabel: linkInitialLabel, accent: accent) { label, url in
                pendingOperation = .insertLink(label: label, url: url)
            }
        }
    }

    private func openLink(_ raw: String) {
        let withScheme = raw.contains("://") ? raw : "https://\(raw)"
        guard let url = URL(string: withScheme) else { return }
        NSWorkspace.shared.open(url)
    }
}
