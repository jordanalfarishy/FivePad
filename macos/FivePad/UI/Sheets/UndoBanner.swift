import SwiftUI

/// Baris urungkan 5 detik — dipakai baik oleh pengosongan slot (FR-1.11)
/// maupun penghapusan/pembersihan tugas (FR-2.5, FR-2.9). Padanan `UndoRow`
/// di Android; tidak ada sebelumnya di macOS, dibangun baru di sini.
struct UndoBanner: View {
    @Environment(\.fivePad) private var colors
    let message: String
    let onUndo: () -> Void

    var body: some View {
        HStack {
            Text(message)
                .fivePadStyle(FivePadText.message)
                .foregroundStyle(colors.ink)
            Spacer()
            Button("Undo", action: onUndo)
                .buttonStyle(.plain)
                .foregroundStyle(colors.accent)
        }
        .padding(.horizontal, Tokens.space4)
        .padding(.vertical, Tokens.space3)
        .background(colors.row)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.radiusMd))
        .overlay(RoundedRectangle(cornerRadius: Tokens.radiusMd).strokeBorder(colors.fieldBorder, lineWidth: 1))
        .padding(Tokens.space3)
        .shadow(radius: 8)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}
