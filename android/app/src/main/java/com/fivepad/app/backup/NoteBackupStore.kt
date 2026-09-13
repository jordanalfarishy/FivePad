package com.fivepad.app.backup

import android.util.AtomicFile
import com.fivepad.app.data.Note
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** Portable notes-only format. Tasks are deliberately outside this archive. */
object NoteBackupCodec {
    const val MAX_BYTES = 2_000_000

    fun encode(notes: List<Note>): String {
        validate(notes)
        return JSONObject().put("format", "fivepad-notes").put("version", 1)
            .put("notes", JSONArray().apply {
                notes.sortedBy { it.slot }.forEach { note ->
                    put(JSONObject().put("slot", note.slot).put("label", note.label).put("body", note.body))
                }
            }).toString(2)
    }

    fun decode(json: String): List<Note> {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(json)
        require(root.getString("format") == "fivepad-notes" && root.get("version") == 1)
        val array = root.getJSONArray("notes")
        require(array.length() == Note.SLOT_COUNT)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val slot = item.get("slot")
            require(slot is Int)
            val label = item.get("label")
            val body = item.get("body")
            require(label is String && body is String)
            Note(slot = slot, label = label, body = body)
        }.also(::validate).sortedBy { it.slot }
    }

    private fun validate(notes: List<Note>) {
        require(notes.map { it.slot }.sorted() == (1..Note.SLOT_COUNT).toList())
        require(notes.all { it.label.length <= Note.MAX_LABEL_LENGTH && it.body.length <= Note.MAX_BODY_LENGTH })
    }
}

/** One atomic snapshot per active day, retaining the latest seven days. */
class NoteBackupStore(private val directory: File) {
    fun list(): List<File> = directory.listFiles { file -> file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.json")) }
        ?.sortedByDescending { it.name }.orEmpty()

    @Synchronized
    fun saveDaily(notes: List<Note>) {
        val bytes = NoteBackupCodec.encode(notes).toByteArray(Charsets.UTF_8)
        check(directory.isDirectory || directory.mkdirs())
        val atomic = AtomicFile(File(directory, "${LocalDate.now()}.json"))
        val output = atomic.startWrite()
        try {
            output.write(bytes)
            atomic.finishWrite(output)
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        }
        list().drop(7).forEach { check(it.delete()) }
    }

    fun read(file: File): List<Note> {
        require(file.parentFile?.canonicalFile == directory.canonicalFile)
        return NoteBackupCodec.decode(AtomicFile(file).readFully().toString(Charsets.UTF_8))
    }

    @Synchronized
    fun deleteAll() { list().forEach { AtomicFile(it).delete() } }
}
