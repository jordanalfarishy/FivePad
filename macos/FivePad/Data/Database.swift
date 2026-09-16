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

        // Kembar Room v4. Antarmuka pengingat Mac menyusul, tetapi skema lokal
        // sudah aman menerima record Android sejak milestone pertama.
        migrator.registerMigration("v4_todo_recurrence") { db in
            try db.alter(table: "todos") { t in
                t.add(column: "recurrence", .text).notNull().defaults(to: Recurrence.none.rawValue)
                t.add(column: "recurrenceAnchorAt", .integer)
            }
        }

        // P1: tepat lima slot ditegakkan oleh SQLite, bukan hanya oleh UI.
        // SQLite tidak dapat menambahkan CHECK ke tabel yang sudah ada, jadi
        // tabel dibangun ulang sekali sambil mempertahankan seluruh datanya.
        migrator.registerMigration("v5_note_slot_constraint") { db in
            try db.execute(sql: """
                CREATE TABLE notes_checked (
                    slot INTEGER PRIMARY KEY CHECK (slot BETWEEN 1 AND 5),
                    label TEXT NOT NULL DEFAULT '',
                    body TEXT NOT NULL DEFAULT '',
                    updatedAt INTEGER NOT NULL,
                    clientUpdatedAt INTEGER NOT NULL,
                    deviceId TEXT
                );
                INSERT INTO notes_checked (slot, label, body, updatedAt, clientUpdatedAt, deviceId)
                    SELECT slot, label, body, updatedAt, clientUpdatedAt, deviceId FROM notes;
                DROP TABLE notes;
                ALTER TABLE notes_checked RENAME TO notes;
                """)
        }

        return migrator
    }
}
