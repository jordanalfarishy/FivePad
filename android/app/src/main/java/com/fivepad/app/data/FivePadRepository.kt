package com.fivepad.app.data

import kotlinx.coroutines.flow.Flow

class FivePadRepository(
    private val notes: NoteDao,
    private val todos: TodoDao,
) {
    fun observeNotes(): Flow<List<Note>> = notes.observeAll()

    fun observeTodos(): Flow<List<Todo>> = todos.observeActive()

    suspend fun saveBody(slot: Int, body: String) =
        notes.updateBody(slot, body.take(Note.MAX_BODY_LENGTH), now())

    suspend fun saveLabel(slot: Int, label: String) =
        notes.updateLabel(slot, label.take(Note.MAX_LABEL_LENGTH), now())

    /**
     * Tugas baru selalu mendarat di akhir daftar. Jarak [POSITION_GAP] menyisakan
     * ruang di antara dua tugas untuk penyisipan nanti tanpa menyentuh baris lain.
     */
    suspend fun addTodo(text: String) {
        val trimmed = text.trim().take(Todo.MAX_TEXT_LENGTH)
        if (trimmed.isEmpty()) return
        todos.insert(Todo(text = trimmed, position = todos.maxPosition() + POSITION_GAP))
    }

    suspend fun setTodoDone(id: String, done: Boolean) = todos.setDone(id, done, now())

    suspend fun setTodoText(id: String, text: String) =
        todos.setText(id, text.trim().take(Todo.MAX_TEXT_LENGTH), now())

    suspend fun deleteTodo(id: String) = todos.softDelete(id, now())

    suspend fun restoreTodo(id: String) = todos.restore(id, now())

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val POSITION_GAP = 1024.0
    }
}
