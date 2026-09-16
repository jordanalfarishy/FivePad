import SwiftUI

struct NotesPane: View {
    @Environment(\.fivePad) private var colors
    @Bindable var store: Store
    let slot: Int
    var autofocus = false
    @FocusState private var editorFocused: Bool

    private var note: Note? { store.notes.first { $0.slot == slot } }

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
                    .foregroundStyle(colors.slotAccents[slot - 1])

                Spacer()

                if bodyText.wrappedValue.count >= Note.bodyWarnLength {
                    Text("\(bodyText.wrappedValue.count.formatted()) / \(Note.maxBodyLength.formatted())")
                        .fivePadStyle(FivePadText.meta)
                        .monospacedDigit()
                        .foregroundStyle(colors.muted)
                }
            }
            .padding(.horizontal, Tokens.screenPadding)
            .frame(height: Tokens.titleRowHeight + Tokens.space3)
            .background(colors.bar)

            TextEditor(text: bodyText)
                .font(.system(size: Tokens.bodyTextSize))
                .lineSpacing(5)
                .scrollContentBackground(.hidden)
                .foregroundStyle(colors.ink)
                .padding(.horizontal, Tokens.screenPadding - 5)
                .padding(.vertical, Tokens.space3)
                .background(colors.background)
                .accessibilityLabel("Note \(slot) editor")
                .focused($editorFocused)
        }
        .onDisappear { store.saveDraft(slot: slot) }
        .onChange(of: slot) { oldSlot, _ in store.saveDraft(slot: oldSlot) }
        .onAppear {
            if autofocus { Task { @MainActor in editorFocused = true } }
        }
        .onChange(of: slot) {
            if autofocus { editorFocused = true }
        }
        .overlay {
            if note == nil {
                ProgressView().controlSize(.small)
            }
        }
    }
}
