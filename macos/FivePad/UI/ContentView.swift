import SwiftUI

/// Jendela utama — FR-5.1.
///
/// Lebar: catatan dan tugas berdampingan, seperti yang diminta FR-5.1 dan
/// seperti yang wajar di desktop. Sempit: runtuh jadi satu kolom dengan tab
/// Catatan/Tugas di tepi bawah — persis layar Android. Bukan dua desain,
/// melainkan satu desain yang tahu lebar jendelanya.
struct ContentView: View {
    @Environment(\.fivePad) private var colors
    @Environment(\.openWindow) private var openWindow
    @AppStorage("selectedSlot") private var slot = 1
    @State private var tab = Tab.notes
    @State private var showSettings = false
    @Bindable var store: Store
    var menuBarController: MenuBarController?

    enum Tab { case notes, tasks }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                if showSettings {
                    SettingsView(
                        store: store,
                        menuBarController: menuBarController,
                        context: .inApp,
                        onBack: { showSettings = false },
                    )
                    .transition(.move(edge: .trailing))
                } else {
                    mainContent(sideBySide: geometry.size.width >= Tokens.sideBySideWidth)
                        .transition(.move(edge: .leading))
                }
            }
            .animation(.easeInOut(duration: 0.28), value: showSettings)
        }
        .frame(minWidth: Tokens.minWindow.width, minHeight: Tokens.minWindow.height)
        .onAppear {
            slot = min(max(slot, 1), Note.slotCount)
            menuBarController?.install()
            menuBarController?.openMainWindow = {
                openWindow(id: "main")
            }
        }
    }

    private func mainContent(sideBySide: Bool) -> some View {
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

    private var topBar: some View {
        ZStack {
            if tab == .notes {
                SlotDots(store: store, active: slot, onSelect: { slot = $0 })
            } else {
                Text("Tasks")
                    .fivePadStyle(FivePadText.screenTitle)
                    .foregroundStyle(colors.ink)
            }
            HStack {
                Button { showSettings = true } label: {
                    Image(systemName: "gearshape")
                        .foregroundStyle(colors.muted)
                        .frame(width: Tokens.topBarHeight, height: Tokens.topBarHeight)
                }
                .buttonStyle(.plain)
                .help("Settings")

                Spacer()

                Color.clear.frame(width: Tokens.topBarHeight, height: Tokens.topBarHeight)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Tokens.topBarHeight)
        .background(colors.bar)
    }

    private var notesPane: some View {
        NotesPane(store: store, slot: slot)
    }

    private var tasksPane: some View {
        TasksPane(store: store)
    }

    private var bottomNav: some View {
        HStack(spacing: 0) {
            navItem(.notes, title: "Notes", accent: colors.slotAccents[slot - 1], symbol: "note.text")
            navItem(
                .tasks,
                title: "Tasks",
                accent: colors.accent,
                symbol: "checklist",
                badge: store.totalCount > 0 ? "\(store.doneCount)/\(store.totalCount)" : nil,
            )
        }
        .frame(height: Tokens.navHeight)
        .background(colors.bar)
        .overlay(alignment: .top) { Rectangle().fill(colors.hairline).frame(height: 1) }
    }

    private func navItem(_ which: Tab, title: String, accent: Color, symbol: String, badge: String? = nil) -> some View {
        let selected = tab == which
        let tint = selected ? accent : colors.muted
        return Button { tab = which } label: {
            VStack(spacing: 2) {
                HStack(spacing: 4) {
                    Image(systemName: symbol).font(.system(size: 17))
                    if let badge {
                        Text(badge)
                            .fivePadStyle(FivePadText.meta)
                            .monospacedDigit()
                    }
                }
                Text(title).fivePadStyle(FivePadText.tab)
            }
            .foregroundStyle(tint)
            .frame(width: Tokens.pillWidth, height: Tokens.pillHeight)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .accessibilityLabel(badge.map { "\(title), \($0)" } ?? title)
    }
}
