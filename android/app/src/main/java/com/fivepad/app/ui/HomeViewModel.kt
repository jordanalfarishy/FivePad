package com.fivepad.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fivepad.app.FivePadApplication
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.data.Note
import com.fivepad.app.data.Todo
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.reminder.Reminders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Satu bagian daftar tugas. [group] null berarti kumpulan tugas tanpa grup. */
data class TaskSection(
    val group: TodoGroup?,
    val todos: List<Todo>,
)

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val groups: List<TodoGroup> = emptyList(),
    /** Teks yang sedang diketik per slot; inilah sumber kebenaran bagi editor. */
    val drafts: Map<Int, String> = emptyMap(),
) {
    val doneCount: Int get() = todos.count { it.done }
    val totalCount: Int get() = todos.size

    fun labelFor(slot: Int): String = notes.firstOrNull { it.slot == slot }?.label.orEmpty()
    fun draftFor(slot: Int): String = drafts[slot].orEmpty()

    /**
     * Tugas tanpa grup tampil lebih dulu, lalu grup sesuai urutannya.
     * Grup kosong tetap ditampilkan (FR-2.16) — kalau disembunyikan, grup yang
     * baru dibuat akan langsung hilang dan terasa seperti gagal tersimpan.
     */
    val sections: List<TaskSection>
        get() {
            val byGroup = todos.groupBy { it.groupId }
            return buildList {
                byGroup[null]?.let { add(TaskSection(null, it)) }
                groups.forEach { g -> add(TaskSection(g, byGroup[g.id].orEmpty())) }
            }
        }
}

class HomeViewModel(
    private val repo: FivePadRepository,
    private val app: Application,
) : ViewModel() {

    private val drafts = MutableStateFlow<Map<Int, String>>(emptyMap())

    val uiState = combine(
        repo.observeNotes(),
        repo.observeTodos(),
        repo.observeGroups(),
        drafts,
    ) { notes, todos, groups, typed ->
        val merged = typed.toMutableMap()
        notes.forEach { note -> merged.putIfAbsent(note.slot, note.body) }
        HomeUiState(notes = notes, todos = todos, groups = groups, drafts = merged)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private var autosaveJob: Job? = null

    /**
     * FR-1.4: simpan 400 ms setelah pengguna berhenti mengetik. Job sebelumnya
     * dibatalkan setiap ketikan, jadi mengetik terus-menerus tidak menulis ke disk.
     */
    fun onBodyChanged(slot: Int, body: String) {
        val clipped = body.take(Note.MAX_BODY_LENGTH)
        drafts.update { it + (slot to clipped) }
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            repo.saveBody(slot, clipped)
        }
    }

    fun onLabelChanged(slot: Int, label: String) {
        viewModelScope.launch { repo.saveLabel(slot, label) }
    }

    /** Dipanggil dari ON_STOP, supaya proses yang dimatikan sistem tidak membawa ketikan. */
    fun flushPendingSaves() {
        autosaveJob?.cancel()
        val snapshot = drafts.value
        viewModelScope.launch {
            snapshot.forEach { (slot, body) -> repo.saveBody(slot, body) }
        }
    }

    fun addTodo(text: String, groupId: String?) = viewModelScope.launch {
        repo.addTodo(text, groupId)
    }

    /**
     * Menyelesaikan tugas juga mematikan pengingatnya, dan membatalkan centang
     * menghidupkannya lagi bila jatuh temponya belum lewat. Tanpa ini, tugas
     * yang sudah selesai tetap berdering — gangguan yang membuat orang mematikan
     * notifikasi aplikasi sepenuhnya.
     */
    fun setTodoDone(id: String, done: Boolean) = viewModelScope.launch {
        repo.setTodoDone(id, done)
        if (done) {
            Reminders.cancel(app, id)
        } else {
            val todo = repo.findTodo(id)
            val due = todo?.dueAt
            if (todo != null && due != null && due > System.currentTimeMillis()) {
                Reminders.schedule(app, id, todo.text, due)
            }
        }
    }

    fun setTodoDue(id: String, dueAt: Long?) = viewModelScope.launch {
        repo.setTodoDue(id, dueAt)
        val todo = repo.findTodo(id)
        if (dueAt != null && todo != null && !todo.done) {
            Reminders.schedule(app, id, todo.text, dueAt)
        } else {
            Reminders.cancel(app, id)
        }
    }

    fun setTodoText(id: String, text: String) = viewModelScope.launch {
        repo.setTodoText(id, text)
        // Teks tugas ikut terbawa ke dalam notifikasi, jadi alarm dijadwalkan
        // ulang supaya isinya tidak basi saat berbunyi nanti.
        val todo = repo.findTodo(id)
        val due = todo?.dueAt
        if (todo != null && due != null && !todo.done && due > System.currentTimeMillis()) {
            Reminders.schedule(app, id, todo.text, due)
        }
    }

    fun deleteTodo(id: String) = viewModelScope.launch {
        repo.deleteTodo(id)
        Reminders.cancel(app, id)
    }

    fun restoreTodo(id: String) = viewModelScope.launch {
        repo.restoreTodo(id)
        val todo = repo.findTodo(id)
        val due = todo?.dueAt
        if (todo != null && due != null && !todo.done && due > System.currentTimeMillis()) {
            Reminders.schedule(app, id, todo.text, due)
        }
    }

    fun addGroup(name: String) = viewModelScope.launch { repo.addGroup(name) }

    fun renameGroup(id: String, name: String) = viewModelScope.launch { repo.renameGroup(id, name) }

    fun deleteGroup(id: String) = viewModelScope.launch { repo.deleteGroup(id) }

    companion object {
        const val AUTOSAVE_DELAY_MS = 400L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FivePadApplication
                HomeViewModel(app.repository, app)
            }
        }
    }
}
