import SwiftUI

@main
struct FivePadApp: App {
    /// Gelap adalah bawaannya, sama seperti Android. Pilihannya milik aplikasi,
    /// bukan mengikuti sistem — lihat FR-6.7 untuk alasannya.
    @AppStorage("theme") private var theme = ThemeMode.dark.rawValue

    init() { Snapshot.runIfRequested() }

    private var colors: FivePadColors {
        FivePadColors.of(ThemeMode(rawValue: theme) ?? .dark)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.fivePad, colors)
                // Komponen bawaan — popover, pemilih tanggal — ikut tema yang
                // dipilih di aplikasi, bukan tema sistem.
                .preferredColorScheme(colors.isLight ? .light : .dark)
        }
        .defaultSize(width: 900, height: 620)
        .windowResizability(.contentMinSize)
    }
}
