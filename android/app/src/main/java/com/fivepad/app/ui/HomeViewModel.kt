package com.fivepad.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fivepad.app.FivePadApplication
import com.fivepad.app.data.AppPreferences
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.data.Note
import com.fivepad.app.data.Todo
import com.fivepad.app.data.Recurrence
import com.fivepad.app.data.ThemeMode
import com.fivepad.app.data.TodoGroup
import com.fivepad.app.reminder.Reminders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Slot yang baru dikosongkan, beserta revisi yang bisa mengembalikannya. */
data class ClearedSlot(val slot: Int, val revisionId: String)

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
    /** Nama slot yang sedang diketik. Alasannya sama seperti [drafts]. */
    val labelDrafts: Map<Int, String> = emptyMap(),
) {
    val doneCount: Int get() = todos.count { it.done }
    val totalCount: Int get() = todos.size

    fun labelFor(slot: Int): String =
        labelDrafts[slot] ?: notes.firstOrNull { it.slot == slot }?.label.orEmpty()
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
    private val preferences: AppPreferences,
    private val app: Application,
) : ViewModel() {

    /** Tema yang sedang aktif. Gelap adalah bawaannya (FR-6.7). */
    val theme: StateFlow<ThemeMode> = preferences.theme

    fun setTheme(mode: ThemeMode) = preferences.setTheme(mode)

    private val drafts = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val labelDrafts = MutableStateFlow<Map<Int, String>>(emptyMap())

    val uiState = combine(
        repo.observeNotes(),
        repo.observeTodos(),
        repo.observeGroups(),
        drafts,
        labelDrafts,
    ) { notes, todos, groups, typed, typedLabels ->
        val merged = typed.toMutableMap()
        notes.forEach { note -> merged.putIfAbsent(note.slot, note.body) }
        HomeUiState(
            notes = notes,
            todos = todos,
            groups = groups,
            drafts = merged,
            labelDrafts = typedLabels,
        )
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

    /**
     * Nama slot melewati draft lebih dulu, sama seperti isi catatan.
     *
     * Dikirim langsung ke basis data, gemanya kembali beberapa bingkai
     * kemudian — dan beberapa nilai lama sempat beredar di jalan. Kolom teks
     * yang menerima nilai-nilai itu sebagai "perubahan dari luar" akan
     * memindahkan kursornya di tengah orang mengetik: "judul" keluar sebagai
     * "udulj". Draft di sini diperbarui seketika, jadi gemanya hanya satu dan
     * selalu sama dengan yang baru saja dikirim.
     */
    fun onLabelChanged(slot: Int, label: String) {
        labelDrafts.update { it + (slot to label) }
        labelSaveJob?.cancel()
        labelSaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            repo.saveLabel(slot, label)
        }
    }

    private var labelSaveJob: Job? = null

    /**
     * Revisi hasil pengosongan slot yang masih bisa diurungkan, atau null.
     * Dipegang di ViewModel, bukan di composable, supaya jendela urungkan tidak
     * hilang hanya karena pengguna menggeser ke slot lain dan kembali.
     */
    private val _clearedTodos = MutableStateFlow<List<String>?>(null)
    val clearedTodos: StateFlow<List<String>?> = _clearedTodos

    private val _clearedSlot = MutableStateFlow<ClearedSlot?>(null)
    val clearedSlot: StateFlow<ClearedSlot?> = _clearedSlot

    /** FR-1.11. Mengosongkan slot setelah isinya disimpan sebagai revisi. */
    fun clearSlot(slot: Int) = viewModelScope.launch {
        autosaveJob?.cancel()
        // Draft di memori harus ikut dikosongkan. Tanpa ini editor masih
        // memegang teks lama, dan autosave berikutnya akan menuliskannya
        // kembali ke basis data — pengosongan yang membatalkan dirinya sendiri.
        val id = repo.clearSlot(slot) ?: return@launch
        drafts.update { it + (slot to "") }
        _clearedSlot.value = ClearedSlot(slot, id)
    }

    fun undoClearSlot() = viewModelScope.launch {
        val cleared = _clearedSlot.value ?: return@launch
        _clearedSlot.value = null
        val note = repo.restoreRevision(cleared.revisionId) ?: return@launch
        drafts.update { it + (note.slot to note.body) }
    }

    fun dismissClearedSlot() {
        _clearedSlot.value = null
    }

    /** Dipanggil dari ON_STOP, supaya proses yang dimatikan sistem tidak membawa ketikan. */
    fun flushPendingSaves() {
        autosaveJob?.cancel()
        val snapshot = drafts.value
        viewModelScope.launch {
            snapshot.forEach { (slot, body) -> repo.saveBody(slot, body) }
        }
    }

    /**
     * Jatuh tempo ikut ditetapkan saat tugas dibuat, bukan lewat langkah kedua:
     * lembar tambah-tugas sudah menanyakannya sekalian, jadi pengingatnya harus
     * terpasang sejak tugas itu ada.
     */
    fun addTodo(text: String, groupId: String?, dueAt: Long?, recurrence: Recurrence) = viewModelScope.launch {
        val todo = repo.addTodo(text, groupId, dueAt, recurrence) ?: return@launch
        if (dueAt != null && dueAt > System.currentTimeMillis()) {
            Reminders.schedule(app, todo.id, todo.text, dueAt)
        }
    }

    /**
     * Menyelesaikan tugas juga mematikan pengingatnya, dan membatalkan centang
     * menghidupkannya lagi bila jatuh temponya belum lewat. Tanpa ini, tugas
     * yang sudah selesai tetap berdering — gangguan yang membuat orang mematikan
     * notifikasi aplikasi sepenuhnya.
     */
    fun setTodoDone(id: String, done: Boolean) = viewModelScope.launch {
        repo.setTodoDone(id, done)
        refreshReminder(id)
    }

    fun editTodo(id: String, text: String, dueAt: Long?, recurrence: Recurrence) = viewModelScope.launch {
        repo.editTodo(id, text, dueAt, recurrence)
        refreshReminder(id)
    }

    private suspend fun refreshReminder(id: String) {
        Reminders.cancel(app, id)
        val todo = repo.findTodo(id) ?: return
        val due = todo.dueAt ?: return
        if (!todo.done && todo.deletedAt == null && due > System.currentTimeMillis()) {
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

    /**
     * Menjatuhkan tugas di antara dua tetangga, entah di grup yang sama atau di
     * grup lain. Yang dikirim adalah posisi kedua tetangganya, bukan indeks:
     * indeks milik daftar yang sedang tampil, sedangkan posisi milik basis data
     * dan tetap bermakna walau daftarnya berubah di sela-sela.
     */
    fun moveTodoToSection(id: String, groupId: String?, before: Double?, after: Double?) {
        viewModelScope.launch { repo.moveTodoToGroup(id, groupId, before, after) }
    }

    fun moveGroup(groups: List<TodoGroup>, from: Int, to: Int) {
        if (from == to) return
        val moving = groups.getOrNull(from) ?: return
        val reordered = groups.toMutableList().apply {
            removeAt(from)
            add(to.coerceIn(0, size), moving)
        }
        val index = reordered.indexOfFirst { it.id == moving.id }
        viewModelScope.launch {
            repo.moveGroup(
                id = moving.id,
                before = reordered.getOrNull(index - 1)?.position,
                after = reordered.getOrNull(index + 1)?.position,
            )
        }
    }

    /** FR-2.9. Mengembalikan id yang dihapus lewat [clearedTodos] untuk diurungkan. */
    fun clearCompleted() = viewModelScope.launch {
        val ids = repo.clearCompleted()
        ids.forEach { Reminders.cancel(app, it) }
        if (ids.isNotEmpty()) _clearedTodos.value = ids
    }

    fun undoClearCompleted() = viewModelScope.launch {
        val ids = _clearedTodos.value ?: return@launch
        _clearedTodos.value = null
        repo.restoreTodos(ids)
    }

    fun dismissClearedTodos() {
        _clearedTodos.value = null
    }

    fun addGroup(name: String) = viewModelScope.launch { repo.addGroup(name) }

    fun renameGroup(id: String, name: String) = viewModelScope.launch { repo.renameGroup(id, name) }

    fun deleteGroup(id: String) = viewModelScope.launch { repo.deleteGroup(id) }

    companion object {
        const val AUTOSAVE_DELAY_MS = 400L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FivePadApplication
                HomeViewModel(app.repository, app.preferences, app)
            }
        }
    }
}
