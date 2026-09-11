package com.fivepad.app.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Tindakan penyuntingan teks, mengikuti menu FiveNotes.
 *
 * [syntax] adalah penanda Markdown yang ditulis tindakan itu, dan ikut tampil
 * di sisi kanan barisnya dalam lembar format. Itu bukan hiasan: aplikasi ini
 * menyimpan Markdown mentah, jadi cepat atau lambat pengguna akan melihat
 * penandanya. Lebih baik ia belajar namanya di tempat ia memakainya.
 */
enum class MarkdownAction(val syntax: String) {
    TODO("- [ ]"),
    HEADER("#"),
    BOLD("**"),
    ITALIC("*"),
    BOLD_ITALIC("***"),
    MARK("=="),
    STRIKE("~~"),
    QUOTE(">"),
    LIST("-"),
    ORDERED_LIST("1."),
    CODE("`"),
    CODE_BLOCK("```"),
    SHIFT_RIGHT("  →"),
    SHIFT_LEFT("← "),
}

/** Sebesar apa satu tingkat indentasi. Dua spasi, bukan tab — tab tidak punya lebar yang disepakati. */
private const val INDENT = "  "

/**
 * Menerapkan [action] pada [value], menghormati seleksi yang sedang aktif.
 *
 * Semua tindakan bersifat **membalik**: menerapkannya pada teks yang sudah
 * memakainya justru mencabutnya kembali. Tanpa itu, satu ketukan tak sengaja
 * hanya bisa dibatalkan dengan menghapus penanda secara manual — dan penanda
 * itu berada di dua tempat terpisah yang mudah luput satu.
 */
fun applyMarkdown(value: TextFieldValue, action: MarkdownAction): TextFieldValue = when (action) {
    MarkdownAction.BOLD,
    MarkdownAction.ITALIC,
    MarkdownAction.BOLD_ITALIC,
    MarkdownAction.MARK,
    MarkdownAction.STRIKE,
    MarkdownAction.CODE,
    -> wrap(value, action.syntax)

    MarkdownAction.TODO -> prefixLines(value, "- [ ] ", alternates = listOf("- [x] "))
    MarkdownAction.HEADER -> prefixLines(value, "# ")
    MarkdownAction.QUOTE -> prefixLines(value, "> ")
    MarkdownAction.LIST -> prefixLines(value, "- ", alternates = listOf("* ", "+ "))
    MarkdownAction.ORDERED_LIST -> numberLines(value)
    MarkdownAction.CODE_BLOCK -> fence(value)
    MarkdownAction.SHIFT_RIGHT -> shift(value, right = true)
    MarkdownAction.SHIFT_LEFT -> shift(value, right = false)
}

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
    val len = marker.length

    val wrappedOutside = start >= len && end + len <= text.length &&
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

    val hasAll = lines.all { line -> all.any { line.trimStart().startsWith(it) } }
    val updated = lines.map { line ->
        val indent = line.takeWhile { it == ' ' }
        val rest = line.drop(indent.length)
        if (hasAll) {
            indent + (all.firstOrNull { rest.startsWith(it) }?.let { rest.drop(it.length) } ?: rest)
        } else {
            indent + prefix + rest
        }
    }
    return replaceSpan(value, span, updated.joinToString("\n"))
}

/** Menomori baris terpilih, atau mencabut nomornya bila semuanya sudah bernomor. */
private fun numberLines(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val span = lineSpan(text, value.selection)
    val lines = text.substring(span.first, span.last).split("\n")
    val numbered = Regex("""^\d+\.\s""")

    val hasAll = lines.all { numbered.containsMatchIn(it.trimStart()) }
    val updated = lines.mapIndexed { index, line ->
        val indent = line.takeWhile { it == ' ' }
        val rest = line.drop(indent.length)
        if (hasAll) {
            indent + rest.replaceFirst(numbered, "")
        } else {
            "$indent${index + 1}. $rest"
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

/** Menggeser baris terpilih masuk atau keluar satu tingkat. */
private fun shift(value: TextFieldValue, right: Boolean): TextFieldValue {
    val text = value.text
    val span = lineSpan(text, value.selection)
    val updated = text.substring(span.first, span.last).split("\n").map { line ->
        if (right) {
            INDENT + line
        } else {
            // Menghapus sampai satu tingkat, tapi tidak lebih dari yang ada —
            // baris yang sudah mentok di kiri tidak boleh kehilangan isinya.
            val removable = line.takeWhile { it == ' ' }.length.coerceAtMost(INDENT.length)
            line.drop(removable)
        }
    }
    return replaceSpan(value, span, updated.joinToString("\n"))
}

/**
 * Mengganti satu rentang baris, lalu menyeleksi seluruh hasilnya.
 *
 * Menyeleksi ulang, bukan menaruh kursor di ujung: tindakan baris sering
 * dipakai beruntun — geser masuk lalu jadikan daftar — dan seleksi yang hilang
 * setelah tindakan pertama memaksa memilih ulang untuk tindakan kedua.
 */
private fun replaceSpan(value: TextFieldValue, span: IntRange, replacement: String): TextFieldValue =
    TextFieldValue(
        text = value.text.replaceRange(span.first, span.last, replacement),
        selection = TextRange(span.first, span.first + replacement.length),
    )
