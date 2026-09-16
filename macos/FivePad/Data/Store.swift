import Foundation
import GRDB
import Observation

/// Kunci bagian "tanpa grup". Bukan string kosong, dan bukan nil: keduanya
/// bisa muncul sebagai id grup sungguhan kalau ada bug di tempat lain.
let ungroupedKey = "__ungrouped__"

/// Satu bagian daftar tugas. `group == nil` berarti tugas tanpa grup.
struct TaskSection: Identifiable, Sendable {
    let group: TodoGroup?
    let todos: [Todo]
    var id: String { group?.id ?? ungroupedKey }
}

/// Slot yang baru saja dikosongkan, dengan revisi yang menyimpan isi lamanya
/// — bahan untuk baris urungkan 5 detik (FR-1.11).
struct ClearedSlot: Equatable {
    let slot: Int
    let revisionId: String
}

/// Sumber kebenaran aplikasi macOS — padanan `FivePadRepository` +
/// `HomeViewModel` di Android, digabung karena di sini tidak ada daur hidup
/// ViewModel yang perlu dipisahkan.
@MainActor
@Observable
final class Store {

    /// Jarak posisi antar baris baru. Pecahan, jadi menyisipkan di tengah cukup
    /// mengambil nilai tengahnya — satu baris ditulis, bukan seluruh daftar.
    private nonisolated static let positionGap = 1024.0

    private let queue: DatabaseQueue
    private let backupStore: NoteBackupStore
    private var observers: [AnyDatabaseCancellable] = []
    private var pendingNoteSaves: [Int: Task<Void, Never>] = [:]
    private var pendingBackup: Task<Void, Never>?

    private(set) var notes: [Note] = []
    private(set) var todos: [Todo] = []
    private(set) var groups: [TodoGroup] = []

    /// Teks yang sedang diketik per slot; inilah sumber kebenaran bagi editor.
    var drafts: [Int: String] = [:]
    /// Label juga harus dimiliki editor. Membaca label langsung dari hasil
    /// pengamatan basis data dapat menggemakan nilai lama di tengah ketikan.
    private var labelDrafts: [Int: String] = [:]

    /// Slot yang baru dikosongkan, tersedia untuk diurungkan 5 detik (FR-1.11).
    private(set) var clearedSlot: ClearedSlot?
    private var clearedSlotUndoTask: Task<Void, Never>?

    /// Tugas yang baru dihapus (satu atau lewat "Clear completed"), tersedia
    /// untuk diurungkan 5 detik (FR-2.5, FR-2.9).
    private(set) var clearedTodos: [Todo] = []
    private var clearedTodosUndoTask: Task<Void, Never>?

    init(queue: DatabaseQueue, backupsDirectory: URL? = nil) {
        self.queue = queue
        self.backupStore = NoteBackupStore(directory: backupsDirectory ?? Self.defaultBackupsDirectory())
        observe()
    }

    private static func defaultBackupsDirectory() -> URL {
        let base = (try? FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true,
        )) ?? FileManager.default.temporaryDirectory
        return base.appendingPathComponent("FivePad", isDirectory: true).appendingPathComponent("Backups", isDirectory: true)
    }

    // MARK: - Turunan

    var doneCount: Int { todos.filter(\.done).count }
    var totalCount: Int { todos.count }

    func label(slot: Int) -> String { labelDrafts[slot] ?? notes.first { $0.slot == slot }?.label ?? "" }
    func draft(slot: Int) -> String { drafts[slot] ?? "" }

    func updateDraft(slot: Int, body: String) {
        let limited = String(body.prefix(Note.maxBodyLength))
        drafts[slot] = limited
        pendingNoteSaves[slot]?.cancel()
        pendingNoteSaves[slot] = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .milliseconds(400))
            guard !Task.isCancelled else { return }
            self?.saveDraft(slot: slot)
        }
    }

    /// Menyimpan draft slot, mencadangkan isi lama sebagai revisi bila perlu.
    ///
    /// Dicadangkan saat: isi barunya kosong, sekurangnya 128 karakter terbuang,
    /// belum ada revisi sama sekali, atau revisi terakhir sudah lewat lima
    /// menit — persis aturan pelambatan `saveBody` di Android, supaya riwayat
    /// tidak mencatat setiap keystroke tapi tetap menangkap perubahan besar.
    func saveDraft(slot: Int) {
        pendingNoteSaves[slot]?.cancel()
        pendingNoteSaves[slot] = nil
        guard let body = drafts[slot] else { return }
        let clipped = String(body.prefix(Note.maxBodyLength))
        let now = Date.nowMillis
        do {
            try queue.write { db in
                guard let old = try Note.fetchOne(db, key: slot), old.body != clipped else { return }
                let removedText = clipped.isEmpty || old.body.count - clipped.count >= 128
                let latest = try NoteRevision
                    .filter(Column("slot") == slot)
                    .order(Column("createdAt").desc)
                    .fetchOne(db)
                let shouldSnapshot = !old.body.isEmpty
                    && (removedText || latest == nil || now - (latest?.createdAt ?? 0) >= 5 * 60_000)
                if shouldSnapshot {
                    try Self.snapshot(db, slot: slot, body: old.body)
                }
                try db.execute(
                    sql: "UPDATE notes SET body = ?, updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
                    arguments: [clipped, now, now, slot],
                )
            }
        } catch {
            assertionFailure("saveDraft failed: \(error)")
        }
    }

    /// Menurunkan seluruh ketikan yang masih menunggu debounce ke disk.
    /// Dipanggil sebelum ekspor/impor dan saat aplikasi kehilangan aktivitas
    /// atau berhenti, sehingga jeda 400 ms tidak pernah menjadi jendela
    /// kehilangan data.
    func flushDrafts() {
        for slot in 1...Note.slotCount where drafts[slot] != nil {
            saveDraft(slot: slot)
        }
    }

    func updateLabel(slot: Int, label: String) {
        let value = String(label.prefix(Note.maxLabelLength))
        labelDrafts[slot] = value
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE notes SET label = ?, updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
                arguments: [value, now, now, slot],
            )
        }
    }

    // MARK: - Kosongkan slot dan urungkan (FR-1.11)

    func clearSlot(_ slot: Int) {
        // Simpan ketikan terbaru lebih dulu. Mengambil isi dari `notes` di sini
        // berisiko memakai hasil observasi yang tertinggal hingga satu putaran
        // run loop dan membuang karakter terakhir pengguna.
        saveDraft(slot: slot)
        let now = Date.nowMillis
        let revisionId: String?
        do {
            revisionId = try queue.write { db -> String? in
                guard let body = try Note.fetchOne(db, key: slot)?.body, !body.isEmpty else { return nil }
                let id = try Self.snapshot(db, slot: slot, body: body)
                try db.execute(
                    sql: "UPDATE notes SET body = '', updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
                    arguments: [now, now, slot],
                )
                return id
            }
        } catch {
            assertionFailure("clearSlot failed: \(error)")
            return
        }
        guard let revisionId else { return }
        drafts[slot] = ""
        clearedSlotUndoTask?.cancel()
        clearedSlot = ClearedSlot(slot: slot, revisionId: revisionId)
        clearedSlotUndoTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .seconds(5))
            guard !Task.isCancelled else { return }
            self?.clearedSlot = nil
        }
    }

    func undoClearSlot() {
        guard let cleared = clearedSlot else { return }
        clearedSlotUndoTask?.cancel()
        clearedSlot = nil
        restoreRevision(cleared.revisionId)
    }

    func dismissClearedSlot() {
        clearedSlotUndoTask?.cancel()
        clearedSlot = nil
    }

    // MARK: - Riwayat catatan (FR-3.11)

    func history(slot: Int) -> [NoteRevision] {
        let cutoff = Date.nowMillis - NoteRevision.retentionMillis
        return (try? queue.read { db in
            try NoteRevision
                .filter(Column("slot") == slot && Column("createdAt") >= cutoff)
                .order(Column("createdAt").desc)
                .fetchAll(db)
        }) ?? []
    }

    /// Memulihkan versi lama sebagai isi terkini. Isi yang tergusur ikut
    /// dicadangkan lebih dulu, jadi pemulihan itu sendiri bisa diurungkan.
    @discardableResult
    func restoreRevision(_ id: String) -> Note? {
        // Riwayat tidak boleh menimpa ketikan yang masih menunggu debounce;
        // simpan dulu agar versi yang tergusur ikut masuk riwayat.
        flushDrafts()
        do {
            let restored = try queue.write { db -> Note? in
                guard let revision = try NoteRevision.fetchOne(db, key: id) else { return nil }
                return try Self.restoreBody(db, slot: revision.slot, body: revision.body)
            }
            if let restored { drafts[restored.slot] = restored.body }
            return restored
        } catch {
            assertionFailure("restoreRevision failed: \(error)")
            return nil
        }
    }

    func deleteHistory(slot: Int) {
        do {
            try queue.write { db in
                _ = try NoteRevision.filter(Column("slot") == slot).deleteAll(db)
            }
        } catch {
            assertionFailure("deleteHistory failed: \(error)")
        }
    }

    @discardableResult
    private static func snapshot(_ db: Database, slot: Int, body: String) throws -> String {
        let revision = NoteRevision(slot: slot, body: body)
        try revision.insert(db)
        try trimRevisions(db, slot: slot)
        return revision.id
    }

    private static func trimRevisions(_ db: Database, slot: Int) throws {
        let stale = try NoteRevision
            .filter(Column("slot") == slot)
            .order(Column("createdAt").desc)
            .fetchAll(db)
            .dropFirst(NoteRevision.keepPerSlot)
        if !stale.isEmpty {
            try NoteRevision.deleteAll(db, keys: stale.map(\.id))
        }
        let cutoff = Date.nowMillis - NoteRevision.retentionMillis
        try NoteRevision.filter(Column("slot") == slot && Column("createdAt") < cutoff).deleteAll(db)
    }

    @discardableResult
    private static func restoreBody(_ db: Database, slot: Int, body: String) throws -> Note? {
        guard let current = try Note.fetchOne(db, key: slot) else { return nil }
        if current.body != body {
            try snapshot(db, slot: slot, body: current.body)
        }
        let now = Date.nowMillis
        try db.execute(
            sql: "UPDATE notes SET body = ?, updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
            arguments: [body, now, now, slot],
        )
        return try Note.fetchOne(db, key: slot)
    }

    // MARK: - Ekspor, impor, dan cadangan harian (FR-7.2-7.4)

    /// Menyalurkan draft yang belum tersimpan lebih dulu, supaya ekspor tidak
    /// pernah ketinggalan ketikan yang masih menunggu jeda 400 ms.
    func exportNotes() throws -> Data {
        flushDrafts()
        // ValueObservation dikirim asinkron ke main queue. Membaca `notes`
        // tepat setelah penulisan dapat mengekspor nilai sebelum penulisan;
        // baca snapshot transaksional langsung dari basis data sebagai gantinya.
        let current = try queue.read { db in
            try Note.order(Column("slot")).fetchAll(db)
        }
        return try NoteBackupCodec.encode(current)
    }

    /// Mengimpor lima catatan sekaligus. Isi lama tiap slot dicadangkan lebih
    /// dulu sebagai revisi, sama seperti tindakan tulis lain.
    func importNotes(_ data: Data) throws {
        let imported = try NoteBackupCodec.decode(data)
        flushDrafts()
        try queue.write { db in
            let now = Date.nowMillis
            for note in imported {
                guard let old = try Note.fetchOne(db, key: note.slot) else { continue }
                if old.body != note.body {
                    try Self.snapshot(db, slot: note.slot, body: old.body)
                }
                try db.execute(
                    sql: "UPDATE notes SET body = ?, label = ?, updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
                    arguments: [note.body, note.label, now, now, note.slot],
                )
            }
        }
        for note in imported {
            drafts[note.slot] = note.body
            labelDrafts[note.slot] = note.label
        }
    }

    func backupList() -> [URL] { backupStore.list() }
    func readBackup(_ url: URL) throws -> [Note] { try backupStore.read(url) }
    func deleteAllBackups() { backupStore.deleteAll() }

    private func scheduleBackup() {
        pendingBackup?.cancel()
        pendingBackup = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .seconds(1))
            guard !Task.isCancelled, let self else { return }
            try? self.backupStore.saveDaily(self.notes)
        }
    }

    /// Tugas tanpa grup lebih dulu, lalu grup sesuai urutannya. Grup kosong
    /// tetap tampil (FR-2.16) — kalau disembunyikan, grup yang baru dibuat
    /// langsung hilang dan terasa seperti gagal tersimpan.
    var sections: [TaskSection] {
        let byGroup = Dictionary(grouping: todos, by: { $0.groupId })
        var result: [TaskSection] = []
        if let ungrouped = byGroup[String?.none] ?? nil, !ungrouped.isEmpty {
            result.append(TaskSection(group: nil, todos: ungrouped))
        }
        for group in groups {
            result.append(TaskSection(group: group, todos: byGroup[group.id] ?? []))
        }
        return result
    }

    // MARK: - Pengamatan

    private func observe() {
        track(ValueObservation.tracking { db in
            try Note.order(Column("slot")).fetchAll(db)
        }) { [weak self] notes in
            guard let self else { return }
            self.notes = notes
            // Draft yang belum pernah disentuh diisi dari basis data; yang sudah
            // diketik tidak ditimpa, supaya ketikan tidak hilang saat baris
            // yang sama datang lagi dari pengamatan.
            for note in notes where self.drafts[note.slot] == nil {
                self.drafts[note.slot] = note.body
            }
            for note in notes where self.labelDrafts[note.slot] == nil {
                self.labelDrafts[note.slot] = note.label
            }
            self.scheduleBackup()
        }

        // Selesai turun ke bawah (FR-2.8), sisanya urutan manual. `id` menutup
        // kemungkinan seri: dua posisi bisa kebetulan bernilai sama, dan tanpa
        // pemecah seri urutannya jadi tak tentu antar pembukaan.
        track(ValueObservation.tracking { db in
            try Todo
                .filter(Column("deletedAt") == nil)
                .order(Column("done").asc, Column("position").asc, Column("id").asc)
                .fetchAll(db)
        }) { [weak self] in self?.todos = $0 }

        track(ValueObservation.tracking { db in
            try TodoGroup
                .filter(Column("deletedAt") == nil)
                .order(Column("position").asc, Column("id").asc)
                .fetchAll(db)
        }) { [weak self] in self?.groups = $0 }
    }

    private func track<T: Sendable>(
        _ observation: ValueObservation<ValueReducers.Fetch<T>>,
        onChange: @escaping @MainActor (T) -> Void,
    ) {
        observers.append(
            observation.start(
                in: queue,
                scheduling: .async(onQueue: .main),
                onError: { error in
                    // Pengamatan yang mati diam-diam berarti antarmuka membeku
                    // pada data lama tanpa ada yang tahu.
                    assertionFailure("observation failed: \(error)")
                },
                onChange: { value in MainActor.assumeIsolated { onChange(value) } },
            ),
        )
    }

    // MARK: - Tugas

    @discardableResult
    func addTodo(text: String, groupId: String? = nil, dueAt: Int64? = nil, recurrence: Recurrence = .none) -> Todo? {
        let value = String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(Todo.maxTextLength))
        guard !value.isEmpty else { return nil }
        let lastPosition = todos.filter { $0.groupId == groupId }.map(\.position).max() ?? 0
        let todo = Todo(
            text: value,
            position: lastPosition + Self.positionGap,
            groupId: groupId,
            dueAt: dueAt,
            recurrence: dueAt == nil ? .none : recurrence,
            recurrenceAnchorAt: recurrence != .none ? dueAt : nil,
        )
        write { db in try todo.insert(db) }
        if let dueAt, dueAt > Date.nowMillis {
            Reminders.schedule(id: todo.id, text: todo.text, dueAt: dueAt)
        }
        return todo
    }

    func toggleTodo(_ todo: Todo) {
        let updated = todo.withCompletion(done: !todo.done, now: Date.nowMillis)
        write { db in try updated.update(db) }
        refreshReminder(for: updated)
    }

    /// Menyunting teks, jatuh tempo, dan pengulangan sekaligus — lewat lembar
    /// yang sama, bukan dua langkah terpisah (FR-2.4).
    func editTodo(_ todo: Todo, text: String, dueAt: Int64?, recurrence: Recurrence) -> Todo? {
        let trimmed = String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(Todo.maxTextLength))
        guard !trimmed.isEmpty else { return nil }
        let repeatValue: Recurrence = dueAt == nil ? .none : recurrence
        let anchor: Int64? = if repeatValue == .none {
            nil
        } else if dueAt == todo.dueAt, repeatValue == todo.recurrence {
            todo.recurrenceAnchorAt ?? dueAt
        } else {
            dueAt
        }
        let now = Date.nowMillis
        let updated: Todo = {
            var t = todo
            t.text = trimmed
            t.dueAt = dueAt
            t.recurrence = repeatValue
            t.recurrenceAnchorAt = anchor
            t.updatedAt = now
            t.clientUpdatedAt = now
            return t
        }()
        write { db in try updated.update(db) }
        refreshReminder(for: updated)
        return updated
    }

    /// Menjadwalkan ulang setelah dialog izin sistem selesai. Penjadwalan
    /// pertama boleh terjadi sebelum izin tersedia dan ditolak diam-diam oleh
    /// sistem, jadi jalur pasca-izin ini wajib ada untuk pengingat pertama.
    func scheduleReminder(for todo: Todo) {
        refreshReminder(for: todo)
    }

    private func refreshReminder(for todo: Todo) {
        Reminders.cancel(id: todo.id)
        guard let due = todo.dueAt, !todo.done, todo.deletedAt == nil, due > Date.nowMillis else { return }
        Reminders.schedule(id: todo.id, text: todo.text, dueAt: due)
    }

    func deleteTodo(_ todo: Todo) {
        softDelete(todo)
        offerTodoUndo([todo])
    }

    /// FR-2.9: membuang seluruh tugas selesai sekaligus, dengan urungkan 5 detik.
    func clearCompleted() {
        let done = todos.filter(\.done)
        guard !done.isEmpty else { return }
        let now = Date.nowMillis
        do {
            try queue.write { db in
                for todo in done {
                    try db.execute(
                        sql: "UPDATE todos SET deletedAt = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                        arguments: [now, now, now, todo.id],
                    )
                }
            }
        } catch {
            assertionFailure("clearCompleted failed: \(error)")
            return
        }
        for todo in done { Reminders.cancel(id: todo.id) }
        offerTodoUndo(done)
    }

    private func softDelete(_ todo: Todo) {
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE todos SET deletedAt = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                arguments: [now, now, now, todo.id],
            )
        }
        Reminders.cancel(id: todo.id)
    }

    private func offerTodoUndo(_ items: [Todo]) {
        clearedTodosUndoTask?.cancel()
        clearedTodos = items
        clearedTodosUndoTask = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .seconds(5))
            guard !Task.isCancelled else { return }
            self?.clearedTodos = []
        }
    }

    func undoClearedTodos() {
        guard !clearedTodos.isEmpty else { return }
        clearedTodosUndoTask?.cancel()
        let items = clearedTodos
        clearedTodos = []
        let now = Date.nowMillis
        write { db in
            for item in items {
                try db.execute(
                    sql: "UPDATE todos SET deletedAt = NULL, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                    arguments: [now, now, item.id],
                )
            }
        }
        for item in items {
            guard let due = item.dueAt, due > now, !item.done else { continue }
            Reminders.schedule(id: item.id, text: item.text, dueAt: due)
        }
    }

    func dismissClearedTodos() {
        clearedTodosUndoTask?.cancel()
        clearedTodos = []
    }

    func moveTodoToSection(id: String, groupId: String?, before: Double?, after: Double?) {
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE todos SET groupId = ?, position = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                arguments: [groupId, Self.between(before, after), now, now, id],
            )
        }
    }

    // MARK: - Grup

    func addGroup(name: String) {
        let value = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(TodoGroup.maxNameLength))
        guard !value.isEmpty else { return }
        let group = TodoGroup(name: value, position: (groups.map(\.position).max() ?? 0) + Self.positionGap)
        write { db in try group.insert(db) }
    }

    func renameGroup(_ id: String, name: String) {
        let value = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(TodoGroup.maxNameLength))
        guard !value.isEmpty else { return }
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE todo_groups SET name = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                arguments: [value, now, now, id],
            )
        }
    }

    /// FR-2.15: tugas di dalam grup **tidak** ikut terhapus, hanya kehilangan
    /// grupnya — satu transaksi, supaya tidak ada celah waktu di mana grup
    /// sudah hilang tapi tugasnya masih menunjuk ke sana.
    func deleteGroup(_ id: String) {
        let now = Date.nowMillis
        do {
            try queue.write { db in
                try db.execute(
                    sql: "UPDATE todos SET groupId = NULL, updatedAt = ?, clientUpdatedAt = ? WHERE groupId = ?",
                    arguments: [now, now, id],
                )
                try db.execute(
                    sql: "UPDATE todo_groups SET deletedAt = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                    arguments: [now, now, now, id],
                )
            }
        } catch {
            assertionFailure("deleteGroup failed: \(error)")
        }
    }

    func moveGroup(_ id: String, before: Double?, after: Double?) {
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE todo_groups SET position = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                arguments: [Self.between(before, after), now, now, id],
            )
        }
    }

    /// Menempatkan sebuah baris di antara dua tetangganya — nilai tengahnya,
    /// supaya menyisipkan cukup menulis satu baris, bukan seluruh daftar.
    /// `nonisolated`: fungsi murni tanpa keadaan aktor, dan dipanggil dari
    /// dalam penutupan `@Sendable` yang dikirim ke `queue.write`.
    private nonisolated static func between(_ before: Double?, _ after: Double?) -> Double {
        switch (before, after) {
        case (nil, nil): positionGap
        case (nil, let a?): a - positionGap
        case (let b?, nil): b + positionGap
        case (let b?, let a?): (b + a) / 2
        }
    }

    private func write(_ updates: @escaping @Sendable (Database) throws -> Void) {
        do { try queue.write(updates) } catch { assertionFailure("write failed: \(error)") }
    }
}
