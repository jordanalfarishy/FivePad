import Foundation

/// Format dan konversi tanggal jatuh tempo. Padanan `DueDates.kt`.
enum DueDates {
    static func isOverdue(_ millis: Int64) -> Bool { millis < Date.nowMillis }

    /// "d MMM, HH:mm" untuk tahun berjalan, "d MMM yyyy, HH:mm" untuk tahun
    /// lain — pola tetap 24 jam, sama seperti Android, bukan mengikuti
    /// format jam 12/24 perangkat.
    static func format(_ millis: Int64, locale: Locale = .current, calendar: Calendar = .current) -> String {
        let date = Date(millis: millis)
        let sameYear = calendar.component(.year, from: date) == calendar.component(.year, from: Date())
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = calendar
        formatter.dateFormat = sameYear ? "d MMM, HH:mm" : "d MMM yyyy, HH:mm"
        return formatter.string(from: date)
    }

    /// Waktu setempat (jam, menit) yang ditampilkan pemilih waktu.
    static func components(_ millis: Int64, calendar: Calendar = .current) -> DateComponents {
        calendar.dateComponents([.hour, .minute], from: Date(millis: millis))
    }

    /// Menggabungkan tanggal terpilih (`DatePicker`, tengah malam setempat)
    /// dengan jam dan menit terpilih.
    static func combine(date: Date, hour: Int, minute: Int, calendar: Calendar = .current) -> Int64 {
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = hour
        components.minute = minute
        return (calendar.date(from: components) ?? date).millis
    }
}
