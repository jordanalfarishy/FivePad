import Foundation

/// Pengulangan kalender: mempertahankan waktu lokal dan tanggal-dalam-bulan
/// aslinya, bukan geser mengambang dari "sekarang". Padanan `Recurrence.kt`.
extension Recurrence {
    /// Kejadian berikutnya setelah [after], dihitung maju dari [anchorAt] dalam
    /// satuan penuh — bukan dari "sekarang" — supaya jam dan (untuk bulanan)
    /// tanggalnya tetap sama setiap kali.
    func nextDue(anchorAt: Int64, after: Int64, calendar: Calendar = .current) -> Int64? {
        guard self != .none else { return nil }
        let component: Calendar.Component
        switch self {
        case .daily: component = .day
        case .weekly: component = .weekOfYear
        case .monthly: component = .month
        case .none: return nil
        }

        let anchor = Date(millis: anchorAt)
        let threshold = Date(millis: after)
        var step = max(
            calendar.dateComponents([component], from: anchor, to: threshold).value(for: component) ?? 0,
            0,
        )
        while true {
            guard let candidate = calendar.date(byAdding: component, value: step, to: anchor) else { return nil }
            let candidateMillis = candidate.millis
            if candidateMillis > after { return candidateMillis }
            step += 1
        }
    }
}

extension Todo {
    /// Menyelesaikan tugas berulang justru memajukan jatuh temponya dan
    /// membiarkannya terbuka, bukan menandainya selesai — tugas berulang tidak
    /// pernah benar-benar sampai ke keadaan "selesai" untuk beristirahat.
    func withCompletion(done: Bool, now: Int64) -> Todo {
        let next: Int64? = if done, !self.done, let due = dueAt {
            recurrence.nextDue(anchorAt: recurrenceAnchorAt ?? due, after: max(now, due))
        } else {
            nil
        }
        var updated = self
        updated.done = next != nil ? false : done
        if let next { updated.dueAt = next }
        updated.updatedAt = now
        updated.clientUpdatedAt = now
        return updated
    }
}

extension Date {
    init(millis: Int64) {
        self.init(timeIntervalSince1970: Double(millis) / 1000)
    }

    var millis: Int64 { Int64(timeIntervalSince1970 * 1000) }
}
