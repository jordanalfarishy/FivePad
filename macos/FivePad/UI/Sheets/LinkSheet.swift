import SwiftUI

/// Lembar alamat tautan. Ada karena tampilan biasa meredupkan `](alamat)`
/// alih-alih menyembunyikannya sepenuhnya (lihat `MarkdownScanner`), tapi
/// alasan aslinya tetap berlaku: label yang sedang terseleksi hampir selalu
/// label yang dimaksud, dan fokus mendarat di alamat — satu-satunya yang
/// belum bisa ditebak dari apa pun. Padanan `LinkSheet` di Android.
struct LinkSheet: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    let initialLabel: String
    let accent: Color
    let onConfirm: (String, String) -> Void

    @State private var label: String
    @State private var url = ""
    @FocusState private var urlFocused: Bool

    init(initialLabel: String, accent: Color, onConfirm: @escaping (String, String) -> Void) {
        self.initialLabel = initialLabel
        self.accent = accent
        self.onConfirm = onConfirm
        _label = State(initialValue: initialLabel)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.space3) {
            Text("Link")
                .fivePadStyle(FivePadText.headerName)
                .foregroundStyle(colors.ink)

            VStack(alignment: .leading, spacing: Tokens.space1) {
                Text("Text").fivePadStyle(FivePadText.description).foregroundStyle(colors.fieldSecondary)
                TextField("", text: $label)
                    .textFieldStyle(.plain)
                    .padding(Tokens.space3)
                    .background(RoundedRectangle(cornerRadius: Tokens.radiusSm).fill(colors.fieldSurface))
                    .overlay(RoundedRectangle(cornerRadius: Tokens.radiusSm).strokeBorder(colors.fieldBorder, lineWidth: 1))
            }

            VStack(alignment: .leading, spacing: Tokens.space1) {
                Text("Address").fivePadStyle(FivePadText.description).foregroundStyle(colors.fieldSecondary)
                TextField("https://", text: $url)
                    .textFieldStyle(.plain)
                    .focused($urlFocused)
                    .onSubmit(confirm)
                    .padding(Tokens.space3)
                    .background(RoundedRectangle(cornerRadius: Tokens.radiusSm).fill(colors.fieldSurface))
                    .overlay(RoundedRectangle(cornerRadius: Tokens.radiusSm).strokeBorder(urlFocused ? accent : colors.fieldBorder, lineWidth: 1))
            }

            HStack {
                Spacer()
                Button("Cancel") { dismiss() }
                    .buttonStyle(.plain)
                    .foregroundStyle(colors.muted)
                Button("Save", action: confirm)
                    .buttonStyle(.borderedProminent)
                    .tint(accent)
                    .disabled(url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .padding(Tokens.space5)
        .frame(width: 360)
        .background(colors.background)
        .onAppear { urlFocused = true }
    }

    private func confirm() {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        onConfirm(label, trimmed)
        dismiss()
    }
}
