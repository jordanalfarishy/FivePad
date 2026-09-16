import AppKit
import SwiftUI

/// Bagaimana lembar ini dipresentasikan — dua tempat, satu sumber kebenaran
/// (lihat catatan di bawah), bukan dua implementasi yang bisa menyimpang.
enum SettingsContext {
    /// Lewat ikon roda gigi, disilang-pudarkan di dalam `ContentView` —
    /// menggambar bilah atasnya sendiri lengkap dengan panah kembali.
    case inApp
    /// Lewat menu ⌘, bawaan macOS — jendela `Settings{}` sudah punya bingkai
    /// dan tombol tutupnya sendiri, jadi tidak perlu bilah atas kedua.
    case scene
}

/// Padanan `SettingsScreen.kt`, dipakai di dua tempat sekaligus supaya
/// keduanya tidak pernah bisa menyimpang: lewat ikon roda gigi (menyilang-
/// pudar di dalam jendela utama, meniru Android) dan lewat ⌘, bawaan macOS.
/// Bagian "Menu Bar" adalah satu-satunya yang murni milik macOS — Android
/// tidak punya padanannya, dan itu wajar: Android tidak punya bilah menu.
struct SettingsView: View {
    @Environment(\.fivePad) private var colors
    @AppStorage("theme") private var theme = ThemeMode.dark.rawValue
    @AppStorage(MenuBarController.shortcutPreference) private var shortcut = MenuBarController.Shortcut.optionSpace.rawValue
    @Bindable var store: Store
    /// `nil` only for off-screen rendering (`Snapshot.swift`), which has no
    /// real status item to configure.
    let menuBarController: MenuBarController?
    var context: SettingsContext = .inApp
    var onBack: (() -> Void)?

    @State private var showBackups = false
    @State private var showLoginToast = false

    var body: some View {
        VStack(spacing: 0) {
            if context == .inApp { topBar }

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    section("App Settings") {
                        Menu {
                            Button("Dark") { theme = ThemeMode.dark.rawValue }
                            Button("Light") { theme = ThemeMode.light.rawValue }
                        } label: {
                            valueRow(label: "Theme", value: theme == ThemeMode.light.rawValue ? "Light" : "Dark")
                        }
                        .menuStyle(.borderlessButton)

                        Button(action: openLanguageSettings) {
                            valueRow(label: "Language", value: currentLanguageName)
                        }
                        .buttonStyle(.plain)
                    }

                    separator

                    section("Notes Backups") {
                        Button { showBackups = true } label: {
                            linkRow("Manage Backups")
                        }
                        .buttonStyle(.plain)
                    }

                    if let menuBarController {
                        separator
                        section("Menu Bar") {
                            Menu {
                                ForEach(MenuBarController.Shortcut.allCases) { option in
                                    Button(option.title) {
                                        shortcut = option.rawValue
                                        menuBarController.registerShortcut()
                                    }
                                }
                            } label: {
                                valueRow(label: "Quick Panel Shortcut", value: currentShortcutTitle)
                            }
                            .menuStyle(.borderlessButton)
                            if let error = menuBarController.shortcutError {
                                Text(error)
                                    .fivePadStyle(FivePadText.description)
                                    .foregroundStyle(.orange)
                                    .padding(.horizontal, Tokens.space4)
                            }
                        }
                    }

                    separator

                    section("App Version \(appVersion)") {
                        Button { open("https://github.com/jordanalfarishy/FivePad/releases") } label: {
                            linkRow("Check for Updates")
                        }
                        .buttonStyle(.plain)
                        Button { open("https://fivepad.app/terms") } label: {
                            linkRow("Terms & Conditions")
                        }
                        .buttonStyle(.plain)
                        Button { open("https://fivepad.app/privacy") } label: {
                            linkRow("Privacy Policy")
                        }
                        .buttonStyle(.plain)
                    }

                    loginSection
                }
                .padding(.vertical, Tokens.space3)
            }
        }
        .frame(minWidth: 420, idealWidth: 460, minHeight: context == .scene ? 480 : nil)
        .background(colors.background)
        .sheet(isPresented: $showBackups) { NoteBackupsSheet(store: store) }
    }

    // MARK: - Struktur bersama

    private var topBar: some View {
        HStack(spacing: 0) {
            Button { onBack?() } label: {
                Image(systemName: "chevron.left")
                    .frame(width: Tokens.topBarHeight, height: Tokens.topBarHeight)
            }
            .buttonStyle(.plain)
            .foregroundStyle(colors.ink)

            Text("Settings")
                .fivePadStyle(FivePadText.screenTitle)
                .foregroundStyle(colors.ink)

            Spacer()
            Color.clear.frame(width: Tokens.topBarHeight, height: Tokens.topBarHeight)
        }
        .frame(height: Tokens.topBarHeight)
        .background(colors.bar)
        .overlay(alignment: .bottom) { Rectangle().fill(colors.hairline).frame(height: 1) }
    }

    private func section(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: Tokens.space2) {
            Text(title.uppercased())
                .fivePadStyle(FivePadText.meta)
                .foregroundStyle(colors.muted)
                .padding(.horizontal, Tokens.space4)
            VStack(spacing: Tokens.space2) { content() }
                .padding(.horizontal, Tokens.space4)
        }
        .padding(.vertical, Tokens.space2)
    }

    private var separator: some View {
        Rectangle().fill(colors.separator).frame(height: 8)
    }

    private func valueRow(label: String, value: String) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).fivePadStyle(FivePadText.description).foregroundStyle(colors.muted)
                Text(value).fivePadStyle(FivePadText.message).foregroundStyle(colors.ink)
            }
            Spacer()
            Image(systemName: "chevron.right").font(.caption).foregroundStyle(colors.muted)
        }
        .padding(Tokens.space3)
        .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
        .overlay(RoundedRectangle(cornerRadius: Tokens.radiusMd).strokeBorder(colors.fieldBorder, lineWidth: 1))
    }

    private func linkRow(_ title: String) -> some View {
        HStack {
            Text(title).fivePadStyle(FivePadText.message).foregroundStyle(colors.ink)
            Spacer()
            Image(systemName: "chevron.right").font(.caption).foregroundStyle(colors.muted)
        }
        .padding(Tokens.space3)
        .background(RoundedRectangle(cornerRadius: Tokens.radiusMd).fill(colors.row))
        .overlay(RoundedRectangle(cornerRadius: Tokens.radiusMd).strokeBorder(colors.fieldBorder, lineWidth: 1))
    }

    /// Tombolnya tetap hidup meski halaman masuk baru datang di M2 — tombol
    /// mati yang tidak menjelaskan apa-apa membuat orang menyangka aplikasinya
    /// rusak; tombol yang menjawab "belum, tapi nanti" setidaknya menjawab.
    private var loginSection: some View {
        VStack(spacing: Tokens.space3) {
            Text("Create an account to sync your notes and tasks across devices.")
                .fivePadStyle(FivePadText.body)
                .foregroundStyle(colors.ink)
                .multilineTextAlignment(.center)
            Button {
                showLoginToast = true
                Task {
                    try? await Task.sleep(for: .seconds(3))
                    showLoginToast = false
                }
            } label: {
                Text("Log In")
                    .fivePadStyle(FivePadText.button)
                    .foregroundStyle(.white)
                    .padding(.horizontal, Tokens.space5)
                    .padding(.vertical, Tokens.space3)
                    .background(Capsule().fill(filledAccent))
            }
            .buttonStyle(.plain)

            if showLoginToast {
                Text("Coming soon — sync isn't available yet.")
                    .fivePadStyle(FivePadText.description)
                    .foregroundStyle(colors.muted)
                    .transition(.opacity)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, Tokens.space6)
        .animation(.default, value: showLoginToast)
    }

    // MARK: - Bantuan

    private var currentShortcutTitle: String {
        MenuBarController.Shortcut(rawValue: shortcut)?.title ?? MenuBarController.Shortcut.optionSpace.title
    }

    private var currentLanguageName: String {
        Locale.current.localizedString(forIdentifier: Locale.current.identifier) ?? Locale.current.identifier
    }

    private var appVersion: String {
        (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String).map { "v\($0)" } ?? ""
    }

    private func open(_ urlString: String) {
        guard let url = URL(string: urlString) else { return }
        NSWorkspace.shared.open(url)
    }

    private func openLanguageSettings() {
        if let url = URL(string: "x-apple.systempreferences:com.apple.Localization-Settings.extension") {
            NSWorkspace.shared.open(url)
        }
    }
}
