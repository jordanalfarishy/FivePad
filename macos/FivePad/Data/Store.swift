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

/// Sumber kebenaran aplikasi macOS — padanan `FivePadRepository` +
/// `HomeViewModel` di Android, digabung karena di sini tidak ada daur hidup
/// ViewModel yang perlu dipisahkan.
@MainActor
@Observable
final class Store {

    /// Jarak posisi antar baris baru. Pecahan, jadi menyisipkan di tengah cukup
    /// mengambil nilai tengahnya — satu baris ditulis, bukan seluruh daftar.
    private static let positionGap = 1024.0

    private let queue: DatabaseQueue
    private var observers: [AnyDatabaseCancellable] = []
    private var pendingNoteSaves: [Int: Task<Void, Never>] = [:]

    private(set) var notes: [Note] = []
    private(set) var todos: [Todo] = []
    private(set) var groups: [TodoGroup] = []

    /// Teks yang sedang diketik per slot; inilah sumber kebenaran bagi editor.
    var drafts: [Int: String] = [:]

    init(queue: DatabaseQueue) {
        self.queue = queue
        observe()
    }

    // MARK: - Turunan

    var doneCount: Int { todos.filter(\.done).count }
    var totalCount: Int { todos.count }

    func label(slot: Int) -> String { notes.first { $0.slot == slot }?.label ?? "" }
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

    func saveDraft(slot: Int) {
        pendingNoteSaves[slot]?.cancel()
        pendingNoteSaves[slot] = nil
        guard let body = drafts[slot] else { return }
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE notes SET body = ?, updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
                arguments: [body, now, now, slot],
            )
        }
    }

    func updateLabel(slot: Int, label: String) {
        let value = String(label.prefix(Note.maxLabelLength))
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE notes SET label = ?, updatedAt = ?, clientUpdatedAt = ? WHERE slot = ?",
                arguments: [value, now, now, slot],
            )
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

    // MARK: - Tulis

    func addTodo(text: String, groupId: String? = nil) {
        let value = String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(Todo.maxTextLength))
        guard !value.isEmpty else { return }
        let lastPosition = todos.filter { $0.groupId == groupId }.map(\.position).max() ?? 0
        let todo = Todo(text: value, position: lastPosition + Self.positionGap, groupId: groupId)
        write { db in try todo.insert(db) }
    }

    func toggleTodo(_ todo: Todo) {
        var updated = todo
        updated.done.toggle()
        updated.updatedAt = Date.nowMillis
        updated.clientUpdatedAt = updated.updatedAt
        let record = updated
        write { db in try record.update(db) }
    }

    func deleteTodo(_ todo: Todo) {
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE todos SET deletedAt = ?, updatedAt = ?, clientUpdatedAt = ? WHERE id = ?",
                arguments: [now, now, now, todo.id],
            )
        }
    }

    func clearCompleted() {
        let now = Date.nowMillis
        write { db in
            try db.execute(
                sql: "UPDATE todos SET deletedAt = ?, updatedAt = ?, clientUpdatedAt = ? WHERE done = 1 AND deletedAt IS NULL",
                arguments: [now, now, now],
            )
        }
    }

    func addGroup(name: String) {
        let value = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(TodoGroup.maxNameLength))
        guard !value.isEmpty else { return }
        let group = TodoGroup(name: value, position: (groups.map(\.position).max() ?? 0) + Self.positionGap)
        write { db in try group.insert(db) }
    }

    private func write(_ updates: @escaping @Sendable (Database) throws -> Void) {
        do { try queue.write(updates) } catch { assertionFailure("write failed: \(error)") }
    }
}
