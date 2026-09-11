import AppKit
import SwiftUI

/// Merender jendela ke PNG tanpa menjalankan aplikasinya.
///
/// Ada karena verifikasi visual harus bisa diukur, bukan dikira-kira: di
/// Android setiap layar dipotret dari perangkat lalu warnanya disampel dan
/// dibandingkan dengan Figma. `screencapture` menuntut izin Screen Recording
/// yang tidak selalu ada, jadi rendernya dilakukan dari dalam — hasilnya justru
/// lebih bersih karena tidak ada bingkai jendela atau wallpaper yang ikut.
///
/// Dinyalakan lewat `FIVEPAD_SNAPSHOT=<folder>`; tanpa itu, kode ini tidak
/// pernah berjalan.
@MainActor
enum Snapshot {

    static func runIfRequested() {
        guard let folder = ProcessInfo.processInfo.environment["FIVEPAD_SNAPSHOT"] else { return }
        let sizes: [(String, CGSize)] = [
            ("wide", CGSize(width: 900, height: 620)),
            ("narrow", CGSize(width: 520, height: 620)),
        ]
        for mode in ThemeMode.allCases {
            for (name, size) in sizes {
                render(theme: mode, size: size, to: "\(folder)/\(mode.rawValue.lowercased())-\(name).png")
            }
        }
        exit(0)
    }

    private static func render(theme: ThemeMode, size: CGSize, to path: String) {
        let colors = FivePadColors.of(theme)
        let view = ContentView()
            .environment(\.fivePad, colors)
            .frame(width: size.width, height: size.height)

        let renderer = ImageRenderer(content: view)
        // 2× supaya piksel yang disampel sepadan dengan tangkapan layar
        // perangkat Android, yang juga di atas 1×.
        renderer.scale = 2
        guard let image = renderer.nsImage,
              let tiff = image.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiff),
              let png = bitmap.representation(using: .png, properties: [:])
        else {
            FileHandle.standardError.write(Data("snapshot failed: \(path)\n".utf8))
            return
        }
        try? png.write(to: URL(fileURLWithPath: path))
    }
}
