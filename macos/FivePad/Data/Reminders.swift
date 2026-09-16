import Foundation
import UserNotifications

/// Pengingat dijadwalkan di perangkat lewat `UNUserNotificationCenter`, bukan
/// dikirim dari server (FR-2.11) — konsekuensinya disengaja: pengingat tetap
/// menyala tanpa koneksi, dan isi tugas tidak pernah meninggalkan perangkat.
///
/// Berbeda dari `AlarmManager` di Android, notifikasi terjadwal macOS bertahan
/// sendiri melewati mulai ulang aplikasi tanpa perlu didaftarkan ulang saat
/// boot — jadi tidak ada padanan `BootReceiver` yang dibutuhkan di sini.
enum Reminders {
    /// Meminta izin notifikasi. Dipanggil **setelah** tugas tersimpan, bukan
    /// saat tanggalnya dipilih — lihat pemanggil di lapisan UI (FR-2.11).
    static func ensureAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        case .denied:
            return false
        @unknown default:
            return false
        }
    }

    static func schedule(id: String, text: String, dueAt: Int64) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [id])
        guard dueAt > Date.nowMillis else { return }

        let content = UNMutableNotificationContent()
        content.title = NSLocalizedString("Task reminder", comment: "Local notification title for a task due date")
        content.body = text
        content.sound = .default

        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: Date(millis: dueAt),
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }

    /// `id.hashCode()` di Android menjaga satu notifikasi per tugas;
    /// identifier string yang sama di sini memainkan peran yang sama —
    /// menjadwalkan ulang tugas yang sama menggantikan permintaan lama,
    /// bukan menumpuk.
    static func cancel(id: String) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [id])
        center.removeDeliveredNotifications(withIdentifiers: [id])
    }
}
