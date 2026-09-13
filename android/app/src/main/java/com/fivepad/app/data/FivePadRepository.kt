package com.fivepad.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FivePadRepository(private val db: FivePadDatabase) {

    private val notes = db.notes()
    private val todos = db.todos()
    private val groups = db.todoGroups()
    private val revisions = db.noteRevisions()

    fun observeNotes(): Flow<List<Note>> = notes.observeAll()

    fun observeTodos(): Flow<List<Todo>> = todos.observeActive()

    fun observeGroups(): Flow<List<TodoGroup>> = groups.observeActive()

    suspend fun allNotes(): List<Note> = notes.all()

    fun observeHistory(slot: Int) = revisions.observe(slot, now() - NoteRevision.RETENTION_MS)

    suspend fun findRevision(id: String) = revisions.find(id)

    suspend fun deleteHistory(slot: Int) = revisions.deleteForSlot(slot)

    suspend fun saveBody(slot: Int, body: String) = db.withTransaction {
        val old = notes.find(slot) ?: return@withTransaction
        val clipped = body.take(Note.MAX_BODY_LENGTH)
        if (old.body == clipped) return@withTransaction
        val latest = revisions.latest(slot)
        val removedText = clipped.isEmpty() || old.body.length - clipped.length >= 128
        if (old.body.isNotEmpty() && (removedText || latest == null || now() - latest.createdAt >= 5 * 60_000L)) {
            snapshot(slot, old.body)
        }
        notes.updateBody(slot, clipped, now())
    }

    suspend fun saveLabel(slot: Int, label: String) = db.withTransaction {
        val clipped = label.take(Note.MAX_LABEL_LENGTH)
        if (notes.find(slot)?.label != clipped) notes.updateLabel(slot, clipped, now())
    }

    private suspend fun snapshot(slot: Int, body: String): String {
        val revision = NoteRevision(slot = slot, body = body)
        revisions.insert(revision)
        revisions.trim(slot, NoteRevision.KEEP_PER_SLOT)
        revisions.purgeOlderThan(now() - NoteRevision.RETENTION_MS)
        return revision.id
    }

    /** Validate the whole import before changing any of the five existing slots. */
    suspend fun replaceNotes(imported: List<Note>) = db.withTransaction {
        require(imported.map { it.slot }.sorted() == (1..Note.SLOT_COUNT).toList())
        require(imported.all { it.body.length <= Note.MAX_BODY_LENGTH && it.label.length <= Note.MAX_LABEL_LENGTH })
        imported.forEach { note ->
            val old = notes.find(note.slot) ?: error("Missing slot")
            if (old.body != note.body) snapshot(note.slot, old.body)
            notes.updateBody(note.slot, note.body, now())
            notes.updateLabel(note.slot, note.label, now())
        }
    }

    /**
     * Tugas baru selalu mendarat di akhir grupnya. Jarak [POSITION_GAP] menyisakan
     * ruang di antara dua tugas untuk penyisipan nanti tanpa menyentuh baris lain.
     */
    suspend fun addTodo(
        text: String,
        groupId: String? = null,
        dueAt: Long? = null,
        recurrence: Recurrence = Recurrence.NONE,
    ): Todo? {
        val trimmed = text.trim().take(Todo.MAX_TEXT_LENGTH)
        if (trimmed.isEmpty()) return null
        val todo = Todo(
            text = trimmed,
            position = todos.maxPosition() + POSITION_GAP,
            groupId = groupId,
            dueAt = dueAt,
            recurrence = if (dueAt == null) Recurrence.NONE else recurrence,
            recurrenceAnchorAt = dueAt.takeIf { recurrence != Recurrence.NONE },
        )
        todos.insert(todo)
        return todo
    }

    /**
     * FR-1.11: mengosongkan slot, menyimpan isinya lebih dulu sebagai revisi.
     *
     * Satu transaksi. Kalau penyimpanan revisi dan pengosongan bisa terpisah,
     * ada satu celah waktu di mana isi catatan sudah hilang tapi salinannya
     * belum ada — dan celah itulah yang akan ditemui orang saat proses dimatikan
     * sistem. Mengembalikan id revisinya, supaya pengurungan tidak perlu menebak
     * revisi mana yang barusan dibuat.
     */
    suspend fun clearSlot(slot: Int): String? = db.withTransaction {
        val body = notes.find(slot)?.body.orEmpty()
        if (body.isEmpty()) return@withTransaction null

        val revisionId = snapshot(slot, body)
        notes.updateBody(slot, "", now())
        revisionId
    }

    /** Save the displaced text too, so restoring history is itself reversible. */
    suspend fun restoreRevision(id: String): Note? = db.withTransaction {
        val revision = revisions.find(id) ?: return@withTransaction null
        restoreBody(revision.slot, revision.body)
    }

    suspend fun restoreBody(slot: Int, body: String): Note? = db.withTransaction {
        require(body.length <= Note.MAX_BODY_LENGTH)
        val current = notes.find(slot) ?: return@withTransaction null
        if (current.body != body) snapshot(slot, current.body)
        notes.updateBody(slot, body, now())
        notes.find(slot)
    }

    suspend fun setTodoDone(id: String, done: Boolean) = db.withTransaction {
        val todo = todos.find(id)?.takeIf { it.deletedAt == null } ?: return@withTransaction
        todos.update(todo.withCompletion(done, now()))
    }

    suspend fun editTodo(id: String, text: String, dueAt: Long?, recurrence: Recurrence) = db.withTransaction {
        val todo = todos.find(id)?.takeIf { it.deletedAt == null } ?: return@withTransaction
        val trimmed = text.trim().take(Todo.MAX_TEXT_LENGTH)
        if (trimmed.isEmpty()) return@withTransaction
        val repeat = if (dueAt == null) Recurrence.NONE else recurrence
        val anchor = if (repeat == Recurrence.NONE) null
            else if (dueAt == todo.dueAt && repeat == todo.recurrence) todo.recurrenceAnchorAt ?: dueAt
            else dueAt
        val stamp = now()
        todos.update(todo.copy(
            text = trimmed, dueAt = dueAt, recurrence = repeat, recurrenceAnchorAt = anchor,
            updatedAt = stamp, clientUpdatedAt = stamp,
        ))
    }

    /**
     * FR-2.9: membuang seluruh tugas yang sudah selesai sekaligus.
     *
     * Mengembalikan id-nya, bukan jumlahnya: pengurungan harus tahu persis baris
     * mana yang dihapus, dan daftar bisa sudah berubah lagi saat tombol urungkan
     * ditekan lima detik kemudian.
     */
    suspend fun clearCompleted(): List<String> = db.withTransaction {
        val ids = todos.completedIds()
        val stamp = now()
        ids.forEach { todos.softDelete(it, stamp) }
        ids
    }

    suspend fun restoreTodos(ids: List<String>) = db.withTransaction {
        val stamp = now()
        ids.forEach { todos.restore(it, stamp) }
    }

    suspend fun setTodoGroup(id: String, groupId: String?) = todos.setGroup(id, groupId, now())

    suspend fun findTodo(id: String): Todo? = todos.find(id)

    suspend fun pendingReminders(now: Long): List<Todo> = todos.pendingReminders(now)

    /**
     * Menempatkan sebuah baris di antara dua tetangganya.
     *
     * Karena posisi bertipe pecahan, menyisipkan cukup mengambil nilai tengah —
     * hanya satu baris yang ditulis, bukan seluruh daftar. Itu penting saat
     * sinkronisasi masuk di M2: satu baris berubah berarti satu baris dikirim.
     */
    private fun between(before: Double?, after: Double?): Double = when {
        before == null && after == null -> POSITION_GAP
        before == null -> after!! - POSITION_GAP
        after == null -> before + POSITION_GAP
        else -> (before + after) / 2
    }

    suspend fun moveTodo(id: String, before: Double?, after: Double?) =
        todos.setPosition(id, between(before, after), now())

    /**
     * Memindahkan tugas ke grup lain sekaligus ke posisi barunya.
     *
     * Keduanya satu transaksi: kalau hanya grupnya yang tersimpan, tugas mendarat
     * di grup tujuan pada urutan lamanya — melompat ke tempat yang tidak dituju
     * siapa pun, dan dari layar tidak terlihat sebagai kegagalan.
     */
    suspend fun moveTodoToGroup(
        id: String,
        groupId: String?,
        before: Double?,
        after: Double?,
    ) = db.withTransaction {
        val stamp = now()
        todos.setGroup(id, groupId, stamp)
        todos.setPosition(id, between(before, after), stamp)
    }

    suspend fun moveGroup(id: String, before: Double?, after: Double?) =
        groups.setPosition(id, between(before, after), now())

    suspend fun deleteTodo(id: String) = todos.softDelete(id, now())

    suspend fun restoreTodo(id: String) = todos.restore(id, now())

    suspend fun addGroup(name: String): String? {
        val trimmed = name.trim().take(TodoGroup.MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return null
        val group = TodoGroup(name = trimmed, position = groups.maxPosition() + POSITION_GAP)
        groups.insert(group)
        return group.id
    }

    suspend fun renameGroup(id: String, name: String) =
        groups.rename(id, name.trim().take(TodoGroup.MAX_NAME_LENGTH), now())

    /**
     * FR-2.15: tugas di dalam grup **tidak** ikut terhapus, hanya kehilangan
     * grupnya. Keduanya dijalankan dalam satu transaksi supaya tidak pernah ada
     * keadaan di mana grup sudah hilang tapi tugasnya masih menunjuk ke sana —
     * keadaan itu akan membuat tugas menghilang dari layar tanpa pernah dihapus.
     */
    suspend fun deleteGroup(id: String) {
        val stamp = now()
        db.withTransaction {
            todos.detachFromGroup(id, stamp)
            groups.softDelete(id, stamp)
        }
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val POSITION_GAP = 1024.0
    }
}
