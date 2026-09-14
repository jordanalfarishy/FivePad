import Foundation
import GRDB

/// Satu dari lima slot catatan permanen.
///
/// Skema ini kembar identik dengan Room di Android — nama kolom, tipe, dan
/// batasannya. Itu bukan kerapian demi kerapian: §10 menuntut aturan
/// sinkronisasi yang sama persis di kedua sisi, dan aturan yang sama di atas
/// skema yang berbeda adalah aturan yang berbeda.
struct Note: Codable, FetchableRecord, PersistableRecord, Identifiable, Sendable {
    static let databaseTableName = "notes"

    var slot: Int
    var label: String = ""
    var body: String = ""
    var updatedAt: Int64 = Date.nowMillis
    var clientUpdatedAt: Int64 = Date.nowMillis
    var deviceId: String?

    var id: Int { slot }

    static let slotCount = 5
    static let maxLabelLength = 24
    static let maxBodyLength = 50_000
    static let bodyWarnLength = 45_000
}

struct Todo: Codable, FetchableRecord, PersistableRecord, Identifiable, Sendable {
    static let databaseTableName = "todos"

    var id: String = UUID().uuidString
    var text: String
    var done: Bool = false
    var position: Double
    var groupId: String?
    var dueAt: Int64?
    var recurrence: Recurrence = .none
    var recurrenceAnchorAt: Int64?
    var updatedAt: Int64 = Date.nowMillis
    var clientUpdatedAt: Int64 = Date.nowMillis
    var deviceId: String?
    var deletedAt: Int64?

    static let maxTextLength = 500
}

/// Nilai mentahnya sama dengan enum Room agar payload sinkronisasi nanti dapat
/// lewat tanpa translasi khusus platform.
enum Recurrence: String, Codable, DatabaseValueConvertible, CaseIterable, Sendable {
    case none = "NONE"
    case daily = "DAILY"
    case weekly = "WEEKLY"
    case monthly = "MONTHLY"
}

/// Grup tugas. Sengaja **tanpa** kolom induk, sama seperti Android: tanpa kolom
/// itu, grup di dalam grup tidak bisa diwakili sama sekali — bukan dilarang
/// oleh validasi yang bisa lupa dipanggil.
struct TodoGroup: Codable, FetchableRecord, PersistableRecord, Identifiable, Sendable {
    static let databaseTableName = "todo_groups"

    var id: String = UUID().uuidString
    var name: String
    var position: Double
    var updatedAt: Int64 = Date.nowMillis
    var clientUpdatedAt: Int64 = Date.nowMillis
    var deviceId: String?
    var deletedAt: Int64?

    static let maxNameLength = 40
}

/// Salinan isi slot sebelum dibuang — FR-1.11, §9 `note_revisions`.
struct NoteRevision: Codable, FetchableRecord, PersistableRecord, Identifiable, Sendable {
    static let databaseTableName = "note_revisions"

    var id: String = UUID().uuidString
    var slot: Int
    var body: String
    var createdAt: Int64 = Date.nowMillis

    static let keepPerSlot = 10
    static let retentionMillis: Int64 = 30 * 24 * 60 * 60 * 1000
}

extension Date {
    /// Milidetik sejak epoch — satuan waktu yang sama dengan Android, supaya
    /// stempel waktu dari kedua perangkat bisa dibandingkan langsung di M2.
    static var nowMillis: Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
}
