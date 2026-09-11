import SwiftUI

/// Pita 4 pt di bawah nama catatan, dalam warna slot yang sedang terbuka.
///
/// Setiap slot punya pola isian sendiri, bukan hanya warna sendiri — kembaran
/// `SlotStripe.kt` di Android, termasuk alasannya: sekitar satu dari dua belas
/// pria mengalami buta warna merah-hijau, dan bagi mereka slot 1 dan slot 3
/// adalah dua rona lumpur yang nyaris sama.
///
/// Polanya dibentuk dua nada dari warna yang sama, bukan warna lawan celah
/// kosong. Celah kosong berarti latar halaman ikut jadi bagian pola, dan pita
/// itu terbaca rusak, bukan bertekstur.
struct SlotStripe: View {
    let slot: Int
    let colour: Color

    /// Nada kedua: aksen dicampur 38% putih. Dihitung sendiri, bukan lewat
    /// `Color.mix`, yang baru ada sejak macOS 15 — dan target kita 14.
    private var tint: Color { colour.lightened(by: 0.38) }

    var body: some View {
        Canvas { context, size in
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(colour))
            switch slot {
            case 1: crosshatch(&context, size, stroke: 1.2, period: 9)
            case 2: diamonds(&context, size, period: 4)
            case 3: diagonals(&context, size, on: 3, period: 8, leansRight: true)
            case 4: verticals(&context, size, on: 2, period: 5)
            default: diagonals(&context, size, on: 3, period: 8, leansRight: false)
            }
        }
        .frame(height: Tokens.stripeHeight)
    }

    /// Goresan miring, kemiringannya tepat 45° karena setinggi pitanya.
    private func diagonals(
        _ context: inout GraphicsContext,
        _ size: CGSize,
        on: CGFloat,
        period: CGFloat,
        leansRight: Bool,
    ) {
        let h = size.height
        var x = -h
        while x < size.width {
            var path = Path()
            if leansRight {
                path.move(to: CGPoint(x: x, y: h))
                path.addLine(to: CGPoint(x: x + on, y: h))
                path.addLine(to: CGPoint(x: x + on + h, y: 0))
                path.addLine(to: CGPoint(x: x + h, y: 0))
            } else {
                path.move(to: CGPoint(x: x, y: 0))
                path.addLine(to: CGPoint(x: x + on, y: 0))
                path.addLine(to: CGPoint(x: x + on + h, y: h))
                path.addLine(to: CGPoint(x: x + h, y: h))
            }
            path.closeSubpath()
            context.fill(path, with: .color(tint))
            x += period
        }
    }

    /// Kisi belah ketupat — sebagian besar tetap warna aksen, kisinya hanya
    /// garis di atasnya. Kebalikan dari `diamonds`.
    private func crosshatch(
        _ context: inout GraphicsContext,
        _ size: CGSize,
        stroke: CGFloat,
        period: CGFloat,
    ) {
        diagonals(&context, size, on: stroke, period: period, leansRight: true)
        diagonals(&context, size, on: stroke, period: period, leansRight: false)
    }

    /// Belah ketupat bersinggungan ujung — papan catur diputar 45°.
    private func diamonds(_ context: inout GraphicsContext, _ size: CGSize, period: CGFloat) {
        let h = size.height
        let half = period / 2
        var cx: CGFloat = 0
        while cx - half < size.width {
            var path = Path()
            path.move(to: CGPoint(x: cx - half, y: h / 2))
            path.addLine(to: CGPoint(x: cx, y: 0))
            path.addLine(to: CGPoint(x: cx + half, y: h / 2))
            path.addLine(to: CGPoint(x: cx, y: h))
            path.closeSubpath()
            context.fill(path, with: .color(tint))
            cx += period
        }
    }

    /// Satu-satunya pola tanpa kemiringan sama sekali.
    private func verticals(
        _ context: inout GraphicsContext,
        _ size: CGSize,
        on: CGFloat,
        period: CGFloat,
    ) {
        var x: CGFloat = 0
        while x < size.width {
            let w = min(on, size.width - x)
            context.fill(Path(CGRect(x: x, y: 0, width: w, height: size.height)), with: .color(tint))
            x += period
        }
    }
}
