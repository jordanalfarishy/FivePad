import SwiftUI

/// Baris tombol bersama untuk titik masuk pengingat — dipakai di
/// `TaskEditorSheet` dan di dalam `ReminderEditor` sendiri untuk tombol waktu.
/// Padanan `ReminderActionButton` di Android.
struct ReminderActionButton: View {
    @Environment(\.fivePad) private var colors
    let title: String
    var description: String?
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: Tokens.space3) {
                Image(systemName: "bell")
                    .foregroundStyle(colors.muted)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .fivePadStyle(FivePadText.message)
                        .foregroundStyle(colors.ink)
                    if let description {
                        Text(description)
                            .fivePadStyle(FivePadText.description)
                            .foregroundStyle(colors.muted)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(colors.muted)
            }
            .padding(.horizontal, Tokens.space4)
            .frame(minHeight: Tokens.touchTarget + Tokens.space2)
            .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
        }
        .buttonStyle(.plain)
    }
}
