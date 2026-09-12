package com.fivepad.app.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Tindakan penyuntingan teks pada catatan.
 *
 * Daftarnya persis yang ada di menu FiveNotes dan di berkas desain — tidak
 * lebih. Tindakan yang tidak terlihat di sana (huruf tebal-miring sekaligus,
 * penyorot, geser indentasi) sengaja tidak ada: setiap baris tambahan di menu
 * ini menambah tinggi lembar yang harus dibuka di atas catatan yang sedang
 * disunting.
 *
 * [syntax] adalah penanda Markdown yang ditulis tindakan itu, dan ikut tampil
 * di petaknya. Itu bukan hiasan: aplikasi ini menyimpan Markdown mentah, dan
 * tampilan mentahnya bisa dibuka kapan saja lewat sakelar di lembar yang sama.
 * Lebih baik penandanya dikenali di tempat ia dipakai.
 */
enum class MarkdownAction(val syntax: String) {
    HEADER("#"),
    SUB_HEADER("##"),
    BOLD("**"),
    ITALIC("*"),
    STRIKE("~~"),
    LIST("-"),
    ORDERED_LIST("1."),
    TODO("- [ ]"),
    QUOTE(">"),
    CODE("`"),
    CODE_BLOCK("```"),
    LINK("[ ]( )"),
}

/**
 * Menerapkan [action] pada [value], menghormati seleksi yang sedang aktif.
 *
 * Semua tindakan bersifat **membalik**: menerapkannya pada teks yang sudah
 * memakainya justru mencabutnya kembali. Tanpa itu, satu ketukan tak sengaja
 * hanya bisa dibatalkan dengan menghapus penanda secara manual — dan sejak
 * tampilan biasa menyembunyikan penandanya, penanda itu bahkan tidak terlihat
 * untuk dihapus.
 */
fun applyMarkdown(value: TextFieldValue, action: MarkdownAction): TextFieldValue = when (action) {
    MarkdownAction.BOLD,
    MarkdownAction.ITALIC,
    MarkdownAction.STRIKE,
    MarkdownAction.CODE,
    -> wrap(value, action.syntax)

    MarkdownAction.HEADER -> heading(value, 1)
    MarkdownAction.SUB_HEADER -> heading(value, 2)
    MarkdownAction.QUOTE -> prefixLines(value, "> ")
    MarkdownAction.LIST -> prefixLines(value, "- ", alternates = listOf("* ", "+ "))
    MarkdownAction.ORDERED_LIST -> numberLines(value)
    MarkdownAction.TODO -> prefixLines(value, "- [ ] ", alternates = listOf("- [x] ", "- [] "))
    MarkdownAction.CODE_BLOCK -> fence(value)
    // Tautan tidak pernah sampai ke sini: alamatnya ditanyakan lebih dulu.
    MarkdownAction.LINK -> value
}

/**
 * Awalan kutipan pada satu baris.
 *
 * Tindakan baris lain menyisipkan penandanya **setelah** awalan ini, bukan
 * sebelum: `> ## Judul` adalah subjudul di dalam kutipan, sedangkan `## > Judul`
 * bukan apa-apa. Ini yang membuat kutipan bisa memuat subjudul, daftar, dan
 * tugas seperti baris biasa.
 */
private val QUOTE_LEAD = Regex("""^\s*>\s?""")

private fun leadOf(line: String): String = QUOTE_LEAD.find(line)?.value.orEmpty()

/**
 * Membungkus seleksi dengan [marker], atau mencabutnya bila sudah terbungkus.
 *
 * Tanpa seleksi, penandanya tetap disisipkan dan kursor mendarat di antaranya —
 * jadi tombolnya berguna sebelum mengetik, bukan hanya sesudah.
 */
private fun wrap(value: TextFieldValue, marker: String): TextFieldValue {
    val text = value.text
    val start = value.selection.min
    val end = value.selection.max

    // Satu bintang di kiri-kanan seleksi belum tentu penanda miring: bisa jadi
    // itu separuh dari penanda tebal yang mengapitnya. Mencabutnya akan
    // mengubah tebal menjadi miring, bukan menambahkan miring pada yang tebal.
    val len = marker.length
    val neighbours = marker != "*" ||
        (text.getOrNull(start - 2) != '*' && text.getOrNull(end + 1) != '*')
    val wrappedOutside = neighbours && start >= len && end + len <= text.length &&
        text.regionMatches(start - len, marker, 0, len) &&
        text.regionMatches(end, marker, 0, len)
    if (wrappedOutside) {
        return TextFieldValue(
            text = text.removeRange(end, end + len).removeRange(start - len, start),
            selection = TextRange(start - len, end - len),
        )
    }

    val selected = text.substring(start, end)
    val wrappedInside = selected.length >= len * 2 &&
        selected.startsWith(marker) && selected.endsWith(marker)
    if (wrappedInside) {
        val inner = selected.substring(len, selected.length - len)
        return TextFieldValue(
            text = text.replaceRange(start, end, inner),
            selection = TextRange(start, start + inner.length),
        )
    }

    return TextFieldValue(
        text = text.replaceRange(start, end, marker + selected + marker),
        selection = if (start == end) {
            TextRange(start + len)
        } else {
            TextRange(start + len, end + len)
        },
    )
}

/** Rentang baris — dari awal baris pertama sampai akhir baris terakhir — yang disentuh seleksi. */
private fun lineSpan(text: String, selection: TextRange): IntRange {
    val start = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0))
        .let { if (it < 0 || selection.min == 0) 0 else it + 1 }
    val end = text.indexOf('\n', selection.max).let { if (it < 0) text.length else it }
    return start..end
}

private val ANY_HEADING = Regex("""^#{1,6}\s+""")

/**
 * Menjadikan baris terpilih judul bertingkat [level].
 *
 * Tingkat yang sudah sama dicabut; tingkat yang berbeda **diganti**, bukan
 * ditumpuk. "Subjudul" pada baris yang sudah berupa judul besar berarti
 * menurunkan tingkatnya — bukan membuat `###`, yang tidak ada di menu ini dan
 * tidak akan pernah bisa dibatalkan dari sana.
 */
private fun heading(value: TextFieldValue, level: Int): TextFieldValue {
    val prefix = "#".repeat(level) + " "
    val text = value.text
    val span = lineSpan(text, value.selection)
    val lines = text.substring(span.first, span.last).split("\n")

    val hasAll = lines.all { it.drop(leadOf(it).length).startsWith(prefix) }
    val updated = lines.map { line ->
        val lead = leadOf(line)
        val rest = line.drop(lead.length).replaceFirst(ANY_HEADING, "")
        if (hasAll) lead + rest else lead + prefix + rest
    }
    return replaceSpan(value, span, updated.joinToString("\n"))
}

/**
 * Menambahkan [prefix] ke setiap baris terpilih, atau mencabutnya bila **semua**
 * baris sudah memilikinya.
 *
 * "Semua", bukan "salah satu": pada seleksi campur — sebagian sudah berpoin,
 * sebagian belum — yang diharapkan orang adalah sisanya ikut berpoin, bukan
 * yang sudah berpoin justru kehilangan poinnya.
 */
private fun prefixLines(
    value: TextFieldValue,
    prefix: String,
    alternates: List<String> = emptyList(),
): TextFieldValue {
    val text = value.text
    val span = lineSpan(text, value.selection)
    val lines = text.substring(span.first, span.last).split("\n")
    val all = listOf(prefix) + alternates

    val hasAll = lines.all { line -> all.any { line.drop(leadOf(line).length).startsWith(it) } }
    val updated = lines.map { line ->
        val lead = leadOf(line)
        val rest = line.drop(lead.length)
        if (hasAll) {
            lead + (all.firstOrNull { rest.startsWith(it) }?.let { rest.drop(it.length) } ?: rest)
        } else {
            lead + prefix + rest
        }
    }
    return replaceSpan(value, span, updated.joinToString("\n"))
}

/** Menomori baris terpilih, atau mencabut nomornya bila semuanya sudah bernomor. */
private fun numberLines(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val span = lineSpan(text, value.selection)
    val lines = text.substring(span.first, span.last).split("\n")

    val hasAll = lines.all { NUMBER_PREFIX.containsMatchIn(it.drop(leadOf(it).length)) }
    val updated = lines.mapIndexed { index, line ->
        val lead = leadOf(line)
        val rest = line.drop(lead.length)
        if (hasAll) {
            lead + rest.replaceFirst(NUMBER_PREFIX, "")
        } else {
            "$lead${index + 1}. $rest"
        }
    }
    return replaceSpan(value, span, updated.joinToString("\n"))
}

/** Memagari baris terpilih dengan ``` di atas dan di bawah, atau melepas pagarnya. */
private fun fence(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val span = lineSpan(text, value.selection)
    val lines = text.substring(span.first, span.last).split("\n")

    val fenced = lines.size >= 2 && lines.first().trimStart().startsWith("```") &&
        lines.last().trimStart().startsWith("```")
    val updated = if (fenced) {
        lines.subList(1, lines.size - 1)
    } else {
        listOf("```") + lines + listOf("```")
    }
    return replaceSpan(value, span, updated.joinToString("\n"))
}

private val WHOLE_LINK = Regex("""^\[([^\]\n]*)]\(([^)\n]*)\)$""")

/**
 * Tautan yang sedang terseleksi, sebagai pasangan label dan alamat.
 *
 * Dipakai untuk dua hal: memberitahu bahwa menekan "Tautan" kali ini berarti
 * **membongkar** tautan yang sudah ada, dan mengisi lebih dulu kolom pada
 * lembar alamat saat yang terseleksi ternyata sebuah tautan utuh.
 */
fun selectedLink(value: TextFieldValue): Pair<String, String>? {
    val selected = value.text.substring(value.selection.min, value.selection.max)
    val m = WHOLE_LINK.find(selected) ?: return null
    return m.groupValues[1] to m.groupValues[2]
}

/** Membongkar tautan yang terseleksi kembali menjadi labelnya saja. */
fun unlink(value: TextFieldValue): TextFieldValue {
    val label = selectedLink(value)?.first ?: return value
    return TextFieldValue(
        text = value.text.replaceRange(value.selection.min, value.selection.max, label),
        selection = TextRange(value.selection.min, value.selection.min + label.length),
    )
}

/**
 * Menyisipkan tautan dengan label dan alamat yang sudah ditentukan.
 *
 * Alamatnya ditanyakan lebih dulu lewat lembar tersendiri, tidak ditulis
 * langsung ke catatan untuk disunting di tempat. Di tampilan biasa, `](alamat)`
 * disembunyikan begitu polanya lengkap — jadi alamat yang disisipkan sebagai
 * teks contoh akan lenyap dari layar pada saat yang sama ia harus diketik.
 */
fun insertLink(value: TextFieldValue, label: String, url: String): TextFieldValue {
    val inserted = "[$label]($url)"
    val start = value.selection.min
    return TextFieldValue(
        text = value.text.replaceRange(start, value.selection.max, inserted),
        selection = TextRange(start + inserted.length),
    )
}

/**
 * Mengganti satu rentang baris, lalu menyeleksi seluruh hasilnya.
 *
 * Menyeleksi ulang, bukan menaruh kursor di ujung: tindakan baris sering
 * dipakai beruntun — jadikan daftar lalu jadikan kutipan — dan seleksi yang
 * hilang setelah tindakan pertama memaksa memilih ulang untuk tindakan kedua.
 */
private fun replaceSpan(value: TextFieldValue, span: IntRange, replacement: String): TextFieldValue =
    TextFieldValue(
        text = value.text.replaceRange(span.first, span.last, replacement),
        selection = TextRange(span.first, span.first + replacement.length),
    )

// --- Melanjutkan daftar saat Enter ------------------------------------------

private val NUMBER_PREFIX = Regex("""^\d+\.\s""")
private val TODO_LINE = Regex("""^(\s*)([-*+])\s+\[[ xX]?]\s*(.*)$""")
private val ORDERED_LINE = Regex("""^(\s*)(\d+)\.\s+(.*)$""")
private val BULLET_LINE = Regex("""^(\s*)([-*+])\s+(.*)$""")

/**
 * Melanjutkan daftar, daftar bernomor, atau daftar tugas ke baris berikutnya.
 *
 * Dipanggil dari `onValueChange` dengan nilai sebelum dan sesudah perubahan;
 * mengembalikan `null` bila perubahannya bukan penekanan Enter, sehingga
 * pemanggilnya cukup memakai nilai aslinya.
 *
 * Menekan Enter pada butir yang masih kosong **mencabut** penandanya alih-alih
 * membuat butir kosong berikutnya. Itulah cara orang mengakhiri daftar; tanpa
 * itu, satu-satunya jalan keluar adalah menghapus penanda yang — di tampilan
 * biasa — bahkan tidak terlihat.
 */
fun continueListOnNewline(before: TextFieldValue, after: TextFieldValue): TextFieldValue? {
    val caret = after.selection.start
    val removed = before.selection.max - before.selection.min

    // Perubahannya harus benar-benar satu baris baru yang diketik di titik
    // sisip, bukan tempelan, bukan penghapusan, bukan penulisan ulang oleh IME.
    if (!after.selection.collapsed) return null
    if (caret != before.selection.min + 1) return null
    if (after.text.length != before.text.length - removed + 1) return null
    if (caret < 1 || after.text[caret - 1] != '\n') return null

    val lineStart = after.text.lastIndexOf('\n', caret - 2).let { if (it < 0) 0 else it + 1 }
    if (lineStart > caret - 1) return null
    val line = after.text.substring(lineStart, caret - 1)

    val todo = TODO_LINE.find(line)
    val ordered = if (todo == null) ORDERED_LINE.find(line) else null
    val bullet = if (todo == null && ordered == null) BULLET_LINE.find(line) else null

    val (prefix, content) = when {
        // Butir tugas baru selalu lahir belum tercentang, apa pun status butir
        // di atasnya.
        todo != null -> todo.groupValues[1] + todo.groupValues[2] + " [ ] " to todo.groupValues[3]
        ordered != null ->
            ordered.groupValues[1] + (ordered.groupValues[2].toIntOrNull()?.plus(1) ?: 1) + ". " to
                ordered.groupValues[3]
        bullet != null -> bullet.groupValues[1] + bullet.groupValues[2] + " " to bullet.groupValues[3]
        else -> return null
    }

    if (content.isBlank()) {
        // Penandanya dicabut bersama baris baru yang barusan dibuat, jadi kursor
        // tinggal di baris yang sama — sekarang kosong dan bukan lagi bagian
        // dari daftar.
        return TextFieldValue(
            text = after.text.removeRange(lineStart, caret),
            selection = TextRange(lineStart),
        )
    }

    return TextFieldValue(
        text = after.text.substring(0, caret) + prefix + after.text.substring(caret),
        selection = TextRange(caret + prefix.length),
    )
}


/** Penanda penekanan sebaris yang ditutup oleh spasi atau baris baru. */
private val EMPHASIS_MARKERS = listOf("***", "**", "~~", "*")

/**
 * Menutup penekanan saat pengguna mengetik spasi atau menekan Enter tepat di
 * dalam penanda penutupnya.
 *
 * Menerapkan Tebal tanpa menyeleksi apa pun lalu mengetik berarti mengetik di
 * antara sepasang penanda, dan tanpa aturan ini penanda itu tidak pernah
 * ditutup — seluruh sisa kalimat ikut menebal. Spasi adalah tempat berhenti
 * yang paling masuk akal: yang dimaksud orang hampir selalu satu kata. Untuk
 * lebih dari satu kata, seleksi dulu lalu terapkan.
 *
 * Kode sengaja tidak ikut: `kode sebaris` justru kerap memuat spasi.
 */
fun closeEmphasisOnBreak(before: TextFieldValue, after: TextFieldValue): TextFieldValue? {
    val at = before.selection.min
    if (!before.selection.collapsed) return null
    if (after.text.length != before.text.length + 1) return null
    if (after.selection.start != at + 1) return null
    val typed = after.text.getOrNull(at) ?: return null
    if (typed != ' ' && typed != '\n') return null

    val marker = EMPHASIS_MARKERS.firstOrNull { before.text.startsWith(it, at) } ?: return null
    // Penanda itu harus benar-benar penutup: pasangannya ada di baris yang sama,
    // sebelum kursor.
    val lineStart = before.text.lastIndexOf('\n', (at - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    if (!before.text.substring(lineStart, at).contains(marker)) return null

    val past = at + marker.length
    return TextFieldValue(
        text = before.text.substring(0, past) + typed + before.text.substring(past),
        selection = TextRange(past + 1),
    )
}
