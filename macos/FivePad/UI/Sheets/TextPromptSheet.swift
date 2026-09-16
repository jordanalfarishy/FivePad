import SwiftUI

/// Satu kolom teks, Batal/Simpan — dipakai untuk membuat dan mengubah nama
/// grup. Padanan `TextPromptSheet` di Android.
struct TextPromptSheet: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    let title: String
    let placeholder: String
    let initialValue: String
    let maxLength: Int
    let confirmLabel: String
    let onConfirm: (String) -> Void

    @State private var value: String
    @FocusState private var focused: Bool

    init(
        title: String,
        placeholder: String = "",
        initialValue: String = "",
        maxLength: Int = 40,
        confirmLabel: String = "Save",
        onConfirm: @escaping (String) -> Void,
    ) {
        self.title = title
        self.placeholder = placeholder
        self.initialValue = initialValue
        self.maxLength = maxLength
        self.confirmLabel = confirmLabel
        self.onConfirm = onConfirm
        _value = State(initialValue: initialValue)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.space3) {
            Text(title)
                .fivePadStyle(FivePadText.headerName)
                .foregroundStyle(colors.ink)

            TextField(placeholder, text: $value)
                .textFieldStyle(.plain)
                .focused($focused)
                .onChange(of: value) { _, new in
                    if new.count > maxLength { value = String(new.prefix(maxLength)) }
                }
                .onSubmit(confirm)
                .padding(Tokens.space3)
                .background(RoundedRectangle(cornerRadius: Tokens.radiusSm).fill(colors.fieldSurface))
                .overlay(RoundedRectangle(cornerRadius: Tokens.radiusSm).strokeBorder(focused ? colors.accent : colors.fieldBorder, lineWidth: 1))

            HStack {
                Spacer()
                Button("Cancel") { dismiss() }
                    .buttonStyle(.plain)
                    .foregroundStyle(colors.muted)
                Button(confirmLabel, action: confirm)
                    .buttonStyle(.borderedProminent)
                    .tint(colors.accent)
                    .disabled(value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .padding(Tokens.space5)
        .frame(width: 340)
        .background(colors.background)
        .onAppear { focused = true }
    }

    private func confirm() {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        onConfirm(trimmed)
        dismiss()
    }
}
