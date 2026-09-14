import SwiftUI

@main
struct FivePadApp: App {
    /// Gelap adalah bawaannya, sama seperti Android. Pilihannya milik aplikasi,
    /// bukan mengikuti sistem — lihat FR-6.7 untuk alasannya.
    @AppStorage("theme") private var theme = ThemeMode.dark.rawValue
    private let store: Store
    private let menuBarController: MenuBarController

    init() {
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

        Settings {
            MacSettingsView(menuBarController: menuBarController)
                .environment(\.fivePad, colors)
                .preferredColorScheme(colors.isLight ? .light : .dark)
        }
    }
}

private struct MacSettingsView: View {
    @AppStorage("theme") private var theme = ThemeMode.dark.rawValue
    @AppStorage(MenuBarController.shortcutPreference) private var shortcut = MenuBarController.Shortcut.optionSpace.rawValue
    @State private var shortcutError: String?
    let menuBarController: MenuBarController

    var body: some View {
        Form {
            Picker("Appearance", selection: $theme) {
                Text("Dark").tag(ThemeMode.dark.rawValue)
                Text("Light").tag(ThemeMode.light.rawValue)
            }
            .pickerStyle(.segmented)

            Picker("Quick panel shortcut", selection: $shortcut) {
                ForEach(MenuBarController.Shortcut.allCases) { option in
                    Text(option.title).tag(option.rawValue)
                }
            }

            if let shortcutError {
                Label(shortcutError, systemImage: "exclamationmark.triangle.fill")
                    .foregroundStyle(.orange)
                    .font(.caption)
            } else {
                Text("The shortcut opens or closes the menu-bar panel from any app.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(24)
        .frame(width: 380)
        .onAppear { shortcutError = menuBarController.shortcutError }
        .onChange(of: shortcut) {
            menuBarController.registerShortcut()
            shortcutError = menuBarController.shortcutError
        }
    }
}

private struct FivePadCommands: Commands {
    @AppStorage("selectedSlot") private var slot = 1
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
        }
    }

    private func slotButton(_ newSlot: Int) -> some View {
        Button("Open Note \(newSlot)") {
            store.saveDraft(slot: slot)
            slot = newSlot
        }
        .keyboardShortcut(KeyEquivalent(Character(String(newSlot))), modifiers: .command)
    }
}
