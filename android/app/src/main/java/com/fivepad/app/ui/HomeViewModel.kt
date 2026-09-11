package com.fivepad.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fivepad.app.FivePadApplication
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.data.Note
import com.fivepad.app.data.Todo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val todos: List<Todo> = emptyList(),
    /** Teks yang sedang diketik per slot; inilah sumber kebenaran bagi editor. */
    val drafts: Map<Int, String> = emptyMap(),
) {
    val doneCount: Int get() = todos.count { it.done }
    val totalCount: Int get() = todos.size

    fun labelFor(slot: Int): String = notes.firstOrNull { it.slot == slot }?.label.orEmpty()
    fun draftFor(slot: Int): String = drafts[slot].orEmpty()
}

class HomeViewModel(private val repo: FivePadRepository) : ViewModel() {

    private val drafts = MutableStateFlow<Map<Int, String>>(emptyMap())

    val uiState = combine(
        repo.observeNotes(),
        repo.observeTodos(),
        drafts,
    ) { notes, todos, typed ->
        // Slot yang belum pernah disentuh mengambil isinya dari basis data; begitu
        // pengguna mengetik, draft yang menang dan tidak lagi ditimpa oleh emisi Room.
        val merged = typed.toMutableMap()
        notes.forEach { note -> merged.putIfAbsent(note.slot, note.body) }
        HomeUiState(notes = notes, todos = todos, drafts = merged)
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

    /**
     * FR-1.4 juga menuntut simpan segera saat aplikasi masuk latar belakang.
     * Dipanggil dari ON_STOP, sehingga tidak ada ketikan yang hilang meski proses
     * langsung dimatikan sistem sesudahnya.
     */
    fun flushPendingSaves() {
        autosaveJob?.cancel()
        val snapshot = drafts.value
        viewModelScope.launch {
            snapshot.forEach { (slot, body) -> repo.saveBody(slot, body) }
        }
    }

    fun addTodo(text: String) = viewModelScope.launch { repo.addTodo(text) }

    fun setTodoDone(id: String, done: Boolean) = viewModelScope.launch { repo.setTodoDone(id, done) }

    fun setTodoText(id: String, text: String) = viewModelScope.launch { repo.setTodoText(id, text) }

    fun deleteTodo(id: String) = viewModelScope.launch { repo.deleteTodo(id) }

    fun restoreTodo(id: String) = viewModelScope.launch { repo.restoreTodo(id) }

    companion object {
        const val AUTOSAVE_DELAY_MS = 400L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FivePadApplication
                HomeViewModel(app.repository)
            }
        }
    }
}
