package com.fivepad.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fivepad.app.backup.NoteBackupCodec
import com.fivepad.app.backup.NoteBackupStore
import com.fivepad.app.data.FivePadDatabase
import com.fivepad.app.data.FivePadRepository
import com.fivepad.app.data.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NotesPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun clearAndRestoreKeepBothLatestTypingAndDisplacedText() = runBlocking {
        val name = "notes-test-${UUID.randomUUID()}.db"
        val db = FivePadDatabase.build(context, name)
        try {
            val repo = FivePadRepository(db)
            assertEquals(5, repo.allNotes().size)
            repo.saveBody(1, "just typed")
            repo.saveBody(2, "second slot")
            repo.saveLabel(2, "My label")
            val revision = checkNotNull(repo.clearSlot(1))
            assertEquals("", repo.allNotes()[0].body)
            assertEquals("just typed", repo.observeHistory(1).first().first().body)
            repo.saveBody(1, "written after clear")
            repo.restoreRevision(revision)
            assertEquals("just typed", repo.allNotes()[0].body)
            assertTrue(repo.observeHistory(1).first().any { it.body == "written after clear" })
            assertEquals("second slot", repo.allNotes()[1].body)
            assertEquals("My label", repo.allNotes()[1].label)
            repeat(14) { repo.saveBody(1, "version $it"); repo.clearSlot(1) }
            assertEquals(10, repo.observeHistory(1).first().size)
            repo.deleteHistory(1)
            assertTrue(repo.observeHistory(1).first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun importIsValidatedBeforeAnySlotChangesAndRoundTripsMarkdown() = runBlocking {
        val name = "notes-test-${UUID.randomUUID()}.db"
        val db = FivePadDatabase.build(context, name)
        try {
            val repo = FivePadRepository(db)
            repo.saveBody(1, "original")
            val notes = (1..5).map { Note(it, "Slot $it", "# Hello $it\n- [ ] café 👋\n`code` \\\"quote\\\"") }
            val decoded = NoteBackupCodec.decode(NoteBackupCodec.encode(notes))
            assertEquals(notes.map { it.body }, decoded.map { it.body })
            assertEquals(notes.map { it.label }, decoded.map { it.label })
            try { repo.replaceNotes(decoded.dropLast(1)); fail("Incomplete import accepted") } catch (_: IllegalArgumentException) { }
            assertEquals("original", repo.allNotes()[0].body)
            val duplicate = decoded.toMutableList().apply { this[4] = this[0] }
            try { repo.replaceNotes(duplicate); fail("Duplicate slot accepted") } catch (_: IllegalArgumentException) { }
            val oversized = decoded.toMutableList().apply { this[4] = this[4].copy(body = "x".repeat(50_001)) }
            try { repo.replaceNotes(oversized); fail("Oversized import accepted") } catch (_: IllegalArgumentException) { }
            assertEquals("original", repo.allNotes()[0].body)
            repo.replaceNotes(decoded)
            assertEquals(notes.map { it.body }, repo.allNotes().map { it.body })
            assertTrue(repo.observeHistory(1).first().any { it.body == "original" })
            val invalidJson = NoteBackupCodec.encode(notes).replace("\"slot\": 5", "\"slot\": 1")
            try { NoteBackupCodec.decode(invalidJson); fail("Duplicate JSON accepted") } catch (_: IllegalArgumentException) { }
            val unsupported = NoteBackupCodec.encode(notes).replace("\"version\": 1", "\"version\": 99")
            try { NoteBackupCodec.decode(unsupported); fail("Unsupported format accepted") } catch (_: IllegalArgumentException) { }
        } finally { db.close(); context.deleteDatabase(name) }
    }


    @Test fun importAndUndoSurviveAnImmediateLifecycleFlush() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = context.applicationContext as FivePadApplication
        val name = "notes-test-${UUID.randomUUID()}.db"
        val db = FivePadDatabase.build(context, name)
        try {
            val repo = FivePadRepository(db)
            lateinit var vm: com.fivepad.app.ui.HomeViewModel
            instrumentation.runOnMainSync {
                vm = com.fivepad.app.ui.HomeViewModel(repo, app.preferences, app)
                vm.onBodyChanged(1, "latest typing")
                vm.onLabelChanged(1, "latest label")
                vm.clearSlot(1)
            }
            app.noteWrites.awaitIdle()
            instrumentation.runOnMainSync {
                vm.undoClearSlot()
                vm.flushPendingSaves()
            }
            app.noteWrites.awaitIdle()
            assertEquals("latest typing", repo.allNotes()[0].body)
            assertEquals("latest label", repo.allNotes()[0].label)
            val imported = (1..5).map { Note(it, "Imported $it", "New body $it") }
            instrumentation.runOnMainSync {
                vm.importNotes(imported)
                vm.flushPendingSaves()
            }
            app.noteWrites.awaitIdle()
            assertEquals(imported.map { it.body }, repo.allNotes().map { it.body })
            assertEquals(imported.map { it.label }, repo.allNotes().map { it.label })
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun dailyBackupsRetainSevenDaysAndCanBeReadAndDeleted() {
        val directory = File(context.cacheDir, "notes-test-${UUID.randomUUID()}")
        try {
            directory.mkdirs()
            val notes = (1..5).map { Note(it, body = "saved $it") }
            val json = NoteBackupCodec.encode(notes)
            repeat(9) { File(directory, "${LocalDate.now().minusDays(it + 1L)}.json").writeText(json) }
            val store = NoteBackupStore(directory)
            store.saveDaily(notes)
            assertEquals(7, store.list().size)
            assertEquals(LocalDate.now().toString(), store.list().first().nameWithoutExtension)
            assertEquals(notes.map { it.body }, store.read(store.list().first()).map { it.body })
            store.deleteAll()
            assertTrue(store.list().isEmpty())
        } finally { directory.deleteRecursively() }
    }
}
