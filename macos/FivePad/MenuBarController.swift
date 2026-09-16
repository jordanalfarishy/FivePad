import AppKit
import Carbon.HIToolbox
import SwiftUI

@MainActor
final class MenuBarController: NSObject, NSWindowDelegate {
    static let shortcutPreference = "menuBarShortcut"

    enum Shortcut: String, CaseIterable, Identifiable {
        case optionSpace
        case optionCommandSpace
        case controlOptionSpace
        case disabled

        var id: String { rawValue }

        var title: String {
            switch self {
            case .optionSpace: "⌥Space"
            case .optionCommandSpace: "⌥⌘Space"
            case .controlOptionSpace: "⌃⌥Space"
            case .disabled: "Disabled"
            }
        }

        var carbonModifiers: UInt32 {
            switch self {
            case .optionSpace: UInt32(optionKey)
            case .optionCommandSpace: UInt32(optionKey | cmdKey)
            case .controlOptionSpace: UInt32(controlKey | optionKey)
            case .disabled: 0
            }
        }
    }

    private let store: Store
    private let colors: () -> FivePadColors
    private var statusItem: NSStatusItem?
    private var panel: QuickPanel?
    private var hotKey: EventHotKeyRef?
    private var hotKeyHandler: EventHandlerRef?
    private var installed = false

    private(set) var shortcutError: String?
    var openMainWindow: (() -> Void)?

    init(store: Store, colors: @escaping () -> FivePadColors) {
        self.store = store
        self.colors = colors
        super.init()
    }

    func install() {
        guard !installed else { return }
        installed = true
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        if let button = item.button {
            button.image = NSImage(systemSymbolName: "square.stack.3d.up.fill", accessibilityDescription: "FivePad")
            button.imagePosition = .imageOnly
            button.target = self
            button.action = #selector(togglePanel)
            // Klik kiri membuka/menutup panel; klik kanan membuka menu ringkas
            // (FR-4.13) — dibedakan lewat jenis peristiwa saat ini, bukan dua
            // action terpisah, supaya kedua klik tetap satu status item.
            button.sendAction(on: [.leftMouseUp, .rightMouseUp])
        }
        statusItem = item
        registerShortcut()
    }

    func registerShortcut() {
        if let hotKey {
            UnregisterEventHotKey(hotKey)
            self.hotKey = nil
        }
        let raw = UserDefaults.standard.string(forKey: Self.shortcutPreference)
        let shortcut = Shortcut(rawValue: raw ?? "") ?? .optionSpace
        guard shortcut != .disabled else {
            shortcutError = nil
            return
        }

        installHotKeyHandlerIfNeeded()
        var reference: EventHotKeyRef?
        let identifier = EventHotKeyID(signature: fourCharacterCode("FVPD"), id: 1)
        let result = RegisterEventHotKey(
            UInt32(kVK_Space),
            shortcut.carbonModifiers,
            identifier,
            GetApplicationEventTarget(),
            0,
            &reference,
        )
        if result == noErr {
            hotKey = reference
            shortcutError = nil
        } else {
            shortcutError = "\(shortcut.title) is already used by macOS or another app."
        }
    }

    @objc func togglePanel() {
        if NSApp.currentEvent?.type == .rightMouseUp {
            showStatusMenu()
            return
        }
        if panel?.isVisible == true { closePanel() } else { showPanel() }
    }

    /// FR-4.13: kelima slot, Pengaturan, dan Keluar. Menu dipasang lalu
    /// dilepas lagi setelah tampil, supaya klik kiri berikutnya tetap
    /// membuka/menutup panel alih-alih menu ini.
    private func showStatusMenu() {
        let menu = NSMenu()
        for slotNumber in 1...Note.slotCount {
            let item = NSMenuItem(title: "Open Note \(slotNumber)", action: #selector(selectSlotFromMenu(_:)), keyEquivalent: "")
            item.tag = slotNumber
            item.target = self
            menu.addItem(item)
        }
        menu.addItem(.separator())
        let settings = NSMenuItem(title: "Settings…", action: #selector(openSettingsFromMenu), keyEquivalent: ",")
        settings.target = self
        menu.addItem(settings)
        menu.addItem(.separator())
        menu.addItem(NSMenuItem(title: "Quit FivePad", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))

        statusItem?.menu = menu
        statusItem?.button?.performClick(nil)
        DispatchQueue.main.async { [weak self] in self?.statusItem?.menu = nil }
    }

    @objc private func selectSlotFromMenu(_ sender: NSMenuItem) {
        closePanel()
        UserDefaults.standard.set(sender.tag, forKey: "selectedSlot")
        showPanel()
    }

    @objc private func openSettingsFromMenu() {
        NSApp.activate(ignoringOtherApps: true)
        NSApp.sendAction(Selector(("showSettingsWindow:")), to: nil, from: nil)
    }

    func closePanel() {
        let selected = UserDefaults.standard.integer(forKey: "selectedSlot")
        store.saveDraft(slot: min(max(selected == 0 ? 1 : selected, 1), Note.slotCount))
        panel?.orderOut(nil)
    }

    func windowDidResize(_ notification: Notification) {
        guard let panel else { return }
        UserDefaults.standard.set(panel.frame.width, forKey: "menuBarPanelWidth")
        UserDefaults.standard.set(panel.frame.height, forKey: "menuBarPanelHeight")
    }

    func windowDidResignKey(_ notification: Notification) { closePanel() }

    private func showPanel() {
        guard let button = statusItem?.button, let buttonWindow = button.window else { return }
        let panel = makePanelIfNeeded()
        let palette = colors()
        panel.contentViewController = NSHostingController(
            rootView: MenuBarPanel(
                store: store,
                openMainWindow: { [weak self] in
                    self?.closePanel()
                    NSApp.activate(ignoringOtherApps: true)
                    self?.openMainWindow?()
                },
                closePanel: { [weak self] in self?.closePanel() },
            )
            .environment(\.fivePad, palette)
            .preferredColorScheme(palette.isLight ? .light : .dark),
        )

        let statusRect = buttonWindow.convertToScreen(button.frame)
        let visible = (buttonWindow.screen ?? NSScreen.main)?.visibleFrame ?? statusRect
        var origin = CGPoint(
            x: statusRect.midX - panel.frame.width / 2,
            y: statusRect.minY - panel.frame.height - 6,
        )
        origin.x = min(max(origin.x, visible.minX + 8), visible.maxX - panel.frame.width - 8)
        origin.y = max(origin.y, visible.minY + 8)
        panel.setFrameOrigin(origin)
        NSApp.activate(ignoringOtherApps: true)
        panel.makeKeyAndOrderFront(nil)
    }

    private func makePanelIfNeeded() -> QuickPanel {
        if let panel { return panel }
        let defaults = UserDefaults.standard
        let width = max(defaults.double(forKey: "menuBarPanelWidth"), 360)
        let height = max(defaults.double(forKey: "menuBarPanelHeight"), 480)
        let panel = QuickPanel(
            contentRect: NSRect(x: 0, y: 0, width: width, height: height),
            styleMask: [.titled, .closable, .resizable, .fullSizeContentView, .nonactivatingPanel],
            backing: .buffered,
            defer: true,
        )
        panel.titleVisibility = .hidden
        panel.titlebarAppearsTransparent = true
        panel.titlebarSeparatorStyle = .none
        panel.standardWindowButton(.closeButton)?.isHidden = true
        panel.standardWindowButton(.miniaturizeButton)?.isHidden = true
        panel.standardWindowButton(.zoomButton)?.isHidden = true
        panel.isMovableByWindowBackground = false
        panel.hidesOnDeactivate = true
        panel.isReleasedWhenClosed = false
        panel.animationBehavior = .utilityWindow
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .transient]
        panel.level = .statusBar
        panel.minSize = NSSize(width: 360, height: 420)
        panel.delegate = self
        panel.onCancel = { [weak self] in self?.closePanel() }
        self.panel = panel
        return panel
    }

    private func installHotKeyHandlerIfNeeded() {
        guard hotKeyHandler == nil else { return }
        var eventType = EventTypeSpec(eventClass: OSType(kEventClassKeyboard), eventKind: UInt32(kEventHotKeyPressed))
        let context = Unmanaged.passUnretained(self).toOpaque()
        InstallEventHandler(
            GetApplicationEventTarget(),
            { _, event, context in
                guard let event, let context else { return OSStatus(eventNotHandledErr) }
                var identifier = EventHotKeyID()
                let status = GetEventParameter(
                    event,
                    EventParamName(kEventParamDirectObject),
                    EventParamType(typeEventHotKeyID),
                    nil,
                    MemoryLayout<EventHotKeyID>.size,
                    nil,
                    &identifier,
                )
                guard status == noErr, identifier.id == 1 else { return OSStatus(eventNotHandledErr) }
                let controller = Unmanaged<MenuBarController>.fromOpaque(context).takeUnretainedValue()
                MainActor.assumeIsolated { controller.togglePanel() }
                return noErr
            },
            1,
            &eventType,
            context,
            &hotKeyHandler,
        )
    }
}

private final class QuickPanel: NSPanel {
    var onCancel: (() -> Void)?
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { false }
    override func cancelOperation(_ sender: Any?) { onCancel?() }
}

private func fourCharacterCode(_ string: String) -> FourCharCode {
    string.utf8.prefix(4).reduce(0) { ($0 << 8) + FourCharCode($1) }
}
