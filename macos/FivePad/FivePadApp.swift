import SwiftUI

@main
struct FivePadApp: App {
    /// Gelap adalah bawaannya, sama seperti Android. Pilihannya milik aplikasi,
    /// bukan mengikuti sistem — lihat FR-6.7 untuk alasannya.
    @AppStorage("theme") private var theme = ThemeMode.dark.rawValue
    private let store: Store
    private let menuBarController: MenuBarController

    init() {
        FivePadFont.registerIfNeeded()
        do {
            let store = Store(queue: try FivePadDatabase.open())
            self.store = store
            self.menuBarController = MenuBarController(store: store) {
                let raw = UserDefaults.standard.string(forKey: "theme") ?? ThemeMode.dark.rawValue
                return FivePadColors.of(ThemeMode(rawValue: raw) ?? .dark)
            }
            Snapshot.runIfRequested(store: store)
        } catch {
            fatalError("FivePad could not open its local database: \(error)")
        }
    }

    private var colors: FivePadColors {
        FivePadColors.of(ThemeMode(rawValue: theme) ?? .dark)
    }

    var body: some Scene {
        WindowGroup("FivePad", id: "main") {
            ContentView(store: store, menuBarController: menuBarController)
                .environment(\.fivePad, colors)
                // Komponen bawaan — popover, pemilih tanggal — ikut tema yang
                // dipilih di aplikasi, bukan tema sistem.
                .preferredColorScheme(colors.isLight ? .light : .dark)
        }
        .defaultSize(width: 900, height: 620)
        .windowResizability(.contentMinSize)
        .commands { FivePadCommands(store: store, menuBarController: menuBarController) }

        // Isi yang sama dengan lembar Pengaturan di dalam aplikasi (ikon roda
        // gigi) — satu sumber kebenaran, bukan dua yang bisa menyimpang.
        Settings {
            SettingsView(store: store, menuBarController: menuBarController, context: .scene)
                .environment(\.fivePad, colors)
                .preferredColorScheme(colors.isLight ? .light : .dark)
        }
    }
}

private struct FivePadCommands: Commands {
    @AppStorage("selectedSlot") private var slot = 1
    /// FR-1.5: `⌘\` kembali ke slot yang sebelumnya aktif — menekannya dua
    /// kali membawa kembali ke tempat semula, seperti sakelar biasa.
    @AppStorage("previousSlot") private var previousSlot = 1
    let store: Store
    let menuBarController: MenuBarController

    var body: some Commands {
        CommandGroup(after: .appInfo) {
            Button("Toggle Quick Panel") { menuBarController.togglePanel() }
                .keyboardShortcut("p", modifiers: [.command, .option])
        }

        CommandMenu("Notes") {
            slotButton(1)
            slotButton(2)
            slotButton(3)
            slotButton(4)
            slotButton(5)
            Divider()
            Button("Switch to Previous Note", action: switchToPrevious)
                .keyboardShortcut("\\", modifiers: .command)
        }
    }

    private func slotButton(_ newSlot: Int) -> some View {
        Button("Open Note \(newSlot)") {
            guard newSlot != slot else { return }
            store.saveDraft(slot: slot)
            previousSlot = slot
            slot = newSlot
        }
        .keyboardShortcut(KeyEquivalent(Character(String(newSlot))), modifiers: .command)
    }

    private func switchToPrevious() {
        guard previousSlot != slot else { return }
        store.saveDraft(slot: slot)
        let target = previousSlot
        previousSlot = slot
        slot = target
    }
}
