import SwiftUI

/// Deretan lima titik penanda slot — node 3:86.
///
/// Sama seperti Android: lingkaran 24 pt, jarak 16 pt, yang aktif bercincin
/// tinta 2 pt **di luar** lingkaran sehingga kotak tata letaknya tetap 24 pt dan
/// cincinnya meluber ke celah. Menggambar cincin ke dalam akan memakan warna
/// slot justru pada titik yang paling perlu terlihat.
struct SlotDots: View {
    @Environment(\.fivePad) private var colors
    let active: Int
    let onSelect: (Int) -> Void

    var body: some View {
        HStack(spacing: Tokens.dotGap) {
            ForEach(1...Note.slotCount, id: \.self) { slot in
                dot(slot)
            }
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
}
