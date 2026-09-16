import SwiftUI

/// Petak empat kolom, dikelompokkan menurut apa yang tersentuh — satu baris
/// penuh (judul, lalu daftar, lalu blok) atau sepotong teks terpilih
/// (penekanan). Kelompoknya tidak berlabel; jaraknya sudah mengatakan hal
/// yang sama tanpa memakan tinggi lembar. Padanan `TextFormatSheet` di Android.
private let formatGroups: [[MarkdownAction]] = [
    [.header, .subHeader],
    [.bold, .italic, .strike, .link, .code],
    [.list, .orderedList, .todo],
    [.quote],
]

struct FormatSheet: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.dismiss) private var dismiss
    @Binding var markdownView: Bool
    let accent: Color
    let onAction: (MarkdownAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Format")
                    .fivePadStyle(FivePadText.headerName)
                    .foregroundStyle(colors.muted)
                Spacer()
                Text("Markdown")
                    .fivePadStyle(FivePadText.message)
                    .foregroundStyle(colors.ink)
                Toggle("", isOn: $markdownView)
                    .toggleStyle(.switch)
                    .tint(accent)
                    .labelsHidden()
            }
            .padding(.horizontal, Tokens.space5)
            .padding(.vertical, Tokens.space2)

            Rectangle().fill(colors.hairline).frame(height: 1)

            VStack(alignment: .leading, spacing: Tokens.space3) {
                ForEach(Array(formatGroups.enumerated()), id: \.offset) { _, group in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: Tokens.space2) {
                            ForEach(group) { action in
                                FormatTile(action: action) {
                                    // Menutup setelah satu tindakan — lembar yang tetap
                                    // terbuka menutupi teks yang barusan diubahnya.
                                    onAction(action)
                                    dismiss()
                                }
                            }
                        }
                    }
                }
            }
            .padding(Tokens.space3)
        }
        .padding(.bottom, Tokens.space4)
        .background(colors.background)
    }
}

private struct FormatTile: View {
    @Environment(\.fivePad) private var colors
    let action: MarkdownAction
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: Tokens.space1) {
                Text(action.syntax)
                    .font(.system(size: Tokens.bodyTextSize, design: .monospaced))
                    .foregroundStyle(colors.ink)
                    .lineLimit(1)
                Text(action.label)
                    .fivePadStyle(FivePadText.description)
                    .foregroundStyle(colors.muted)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .frame(height: 28)
            }
            .frame(width: 78)
            .padding(.vertical, Tokens.space2)
            .padding(.horizontal, Tokens.space1)
            .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
            .overlay(RoundedRectangle(cornerRadius: Tokens.radiusMd).strokeBorder(colors.fieldBorder, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
