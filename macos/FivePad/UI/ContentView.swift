import SwiftUI

/// Jendela utama — FR-5.1.
///
/// Lebar: catatan dan tugas berdampingan, seperti yang diminta FR-5.1 dan
/// seperti yang wajar di desktop. Sempit: runtuh jadi satu kolom dengan tab
/// Catatan/Tugas di tepi bawah — persis layar Android. Bukan dua desain,
/// melainkan satu desain yang tahu lebar jendelanya.
struct ContentView: View {
    @Environment(\.fivePad) private var colors
    @State private var slot = 1
    @State private var tab = Tab.notes

    enum Tab { case notes, tasks }

    var body: some View {
        GeometryReader { geometry in
            let sideBySide = geometry.size.width >= Tokens.sideBySideWidth

            VStack(spacing: 0) {
                topBar
                SlotStripe(slot: slot, colour: colors.slotAccents[slot - 1])

                if sideBySide {
                    HStack(spacing: 0) {
                        notesPane
                        Divider().overlay(colors.hairline)
                        tasksPane.frame(width: 375)
                    }
                } else {
                    if tab == .notes { notesPane } else { tasksPane }
                    bottomNav
                }
            }
            .background(colors.background)
        }
        .frame(minWidth: Tokens.minWindow.width, minHeight: Tokens.minWindow.height)
    }

    private var topBar: some View {
        ZStack {
            SlotDots(active: slot, onSelect: { slot = $0 })
        }
        .frame(maxWidth: .infinity)
        .frame(height: Tokens.topBarHeight)
        .background(colors.bar)
    }

    private var notesPane: some View {
        Text("Notes")
            .foregroundStyle(colors.ink)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var tasksPane: some View {
        Text("Tasks")
            .foregroundStyle(colors.ink)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var bottomNav: some View {
        HStack(spacing: 0) {
            navPill(.notes, accent: colors.slotAccents[slot - 1], symbol: "list.clipboard.fill")
            navPill(.tasks, accent: colors.accent, symbol: "list.bullet.rectangle.fill")
        }
        .frame(height: Tokens.navHeight)
        .background(colors.bar)
        .overlay(alignment: .top) { Rectangle().fill(colors.hairline).frame(height: 1) }
    }

    private func navPill(_ which: Tab, accent: Color, symbol: String) -> some View {
        let selected = tab == which
        let tint = selected ? accent : colors.ink
        return Button { tab = which } label: {
            Image(systemName: symbol)
                .font(.system(size: 17))
                .foregroundStyle(tint)
                .frame(width: Tokens.pillWidth, height: Tokens.pillHeight)
                .background(
                    RoundedRectangle(cornerRadius: Tokens.radiusPill)
                        .fill(selected ? accent.opacity(pillAlpha) : .clear),
                )
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
    }
}
