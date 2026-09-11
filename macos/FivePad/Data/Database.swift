import Foundation
import GRDB

/// Basis data lokal FivePad di macOS.
///
/// Skemanya kembar identik dengan Room v3 di Android, sampai nama kolom dan
/// urutan indeksnya. Migrasi diberi nama sesuai versi Room supaya keduanya bisa
/// dibaca berdampingan saat aturan sinkronisasi M2 ditulis.
enum FivePadDatabase {

    static func open() throws -> DatabaseQueue {
        let folder = try FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true,
        ).appendingPathComponent("FivePad", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)

        var config = Configuration()
        // Kunci asing ditegakkan basis data, bukan oleh kode pemanggil yang
        // bisa lupa memeriksanya.
        config.foreignKeysEnabled = true

        let queue = try DatabaseQueue(path: folder.appendingPathComponent("fivepad.sqlite").path, configuration: config)
        try migrator.migrate(queue)
        return queue
    }

    /// Basis data di memori, untuk pratinjau dan uji.
    static func inMemory() throws -> DatabaseQueue {
        let queue = try DatabaseQueue()
        try migrator.migrate(queue)
        return queue
    }

    private static var migrator: DatabaseMigrator {
        var migrator = DatabaseMigrator()

        migrator.registerMigration("v1_notes_and_todos") { db in
            try db.create(table: "notes") { t in
                t.primaryKey("slot", .integer)
                t.column("label", .text).notNull().defaults(to: "")
                t.column("body", .text).notNull().defaults(to: "")
                t.column("updatedAt", .integer).notNull()
                t.column("clientUpdatedAt", .integer).notNull()
                t.column("deviceId", .text)
            }

            try db.create(table: "todos") { t in
                t.primaryKey("id", .text)
                t.column("text", .text).notNull()
                t.column("done", .boolean).notNull().defaults(to: false)
                t.column("position", .double).notNull()
                t.column("dueAt", .integer)
                t.column("updatedAt", .integer).notNull()
                t.column("clientUpdatedAt", .integer).notNull()
                t.column("deviceId", .text)
                t.column("deletedAt", .integer)
            }

            // Satu-satunya tempat baris `notes` pernah dibuat. Tidak ada insert
            // lain di mana pun, sehingga jumlah slot terkunci di lima secara
            // struktural — bukan oleh aturan yang bisa dilewati.
            let now = Date.nowMillis
            for slot in 1...Note.slotCount {
                try Note(slot: slot, updatedAt: now, clientUpdatedAt: now).insert(db)
            }
        }

        migrator.registerMigration("v2_todo_groups") { db in
            try db.alter(table: "todos") { t in
                t.add(column: "groupId", .text)
            }
            try db.create(table: "todo_groups") { t in
                t.primaryKey("id", .text)
                t.column("name", .text).notNull()
                t.column("position", .double).notNull()
                t.column("updatedAt", .integer).notNull()
                t.column("clientUpdatedAt", .integer).notNull()
                t.column("deviceId", .text)
                t.column("deletedAt", .integer)
            }
        }

        migrator.registerMigration("v3_note_revisions") { db in
            try db.create(table: "note_revisions") { t in
                t.primaryKey("id", .text)
                t.column("slot", .integer).notNull()
                t.column("body", .text).notNull()
                t.column("createdAt", .integer).notNull()
            }
        }

        return migrator
    }
}
