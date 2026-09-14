package com.fivepad.app.ui.markdown

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import com.fivepad.app.ui.theme.Tokens
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Warna yang dipakai tampilan biasa. Sebagian ikut slot yang sedang aktif, jadi
 * paletnya dirakit ulang tiap kali slot berpindah.
 */
@Immutable
class MarkdownPalette(
    val ink: Color,
    /** Aksen slot: pita kiri kutipan dan blok kode. */
    val accent: Color,
    /** Isian kutipan — aksen 12% di atas latar, persis seperti di Figma. */
    val quoteFill: Color,
    /** Isian blok kode: hitam di tema gelap, putih di tema terang. */
    val codeFill: Color,
    val link: Color,
    val checkboxFill: Color,
    val checkboxStroke: Color,
    /** Teks tugas yang sudah dicentang, dan alamat yang disembunyikan. */
    val muted: Color,
    val baseSize: TextUnit,
)

/** Blok berlatar penuh selebar kolom teks — kutipan atau kode. */
@Immutable
class MarkdownBlock(
    /** Rentang di teks **tampil**, bukan teks sumber. */
    val start: Int,
    val end: Int,
    val code: Boolean,
)

/** Tautan di teks tampil — labelnya terlihat, alamatnya tidak. */
@Immutable
class MarkdownLink(
    /** Rentang label di teks **tampil**. */
    val start: Int,
    val end: Int,
    val url: String,
)

/** Kotak centang yang digambar di awal baris tugas. */
@Immutable
class MarkdownBox(
    /** Awal isi baris di teks **tampil** — dipakai mencari barisnya di tata letak. */
    val transformed: Int,
    /** Rentang penanda `- [ ]` di teks **sumber**. */
    val markerStart: Int,
    val markerEnd: Int,
    /** Indentasi dan tanda poin yang harus dipertahankan saat status dibalik. */
    val lead: String,
    val checked: Boolean,
)

/** Hasil satu kali penataan: teks tampil, pemetaan offset, dan apa yang harus digambar. */
class MarkdownRender(
    val annotated: AnnotatedString,
    val mapping: OffsetMapping,
    val blocks: List<MarkdownBlock>,
    val boxes: List<MarkdownBox>,
    val links: List<MarkdownLink>,
)

/**
 * Menata Markdown untuk tampilan biasa: **penandanya disembunyikan**, bukan
 * sekadar diredupkan.
 *
 * Menyembunyikan penanda berarti teks tampil tidak lagi sepanjang teks sumber,
 * dan setiap posisi kursor harus diterjemahkan antara keduanya. Terjemahan
 * itulah sumber klasik bug kursor meloncat, jadi pemetaannya dibangun sekali
 * sebagai tabel penuh — satu entri per karakter, ke dua arah — bukan dihitung
 * ulang dengan aritmetika offset di setiap panggilan.
 *
 * Yang tidak bisa dinyatakan sebagai gaya teks — latar penuh kutipan, pita
 * aksen, dan kotak centang bundar — tidak dipalsukan dengan karakter Unicode.
 * Semua itu digambar terpisah di belakang teks, dan [last] adalah yang
 * memberitahu penggambarnya di mana.
 */
class MarkdownVisualTransformation(
    private val palette: MarkdownPalette,
) : VisualTransformation {

    /**
     * Hasil penataan terakhir.
     *
     * Bukan state Compose: nilainya hanya dibaca pada fase menggambar, yang
     * selalu berjalan setelah [filter] pada frame yang sama. Menjadikannya
     * state justru akan menulis state dari dalam fase tata letak.
     */
    var last: MarkdownRender? = null
        private set

    private var cachedSource: String? = null

    override fun filter(text: AnnotatedString): TransformedText {
        // filter() dipanggil pada setiap recomposition, termasuk saat hanya
        // kursor yang bergerak.
        if (text.text != cachedSource || last == null) {
            last = renderMarkdown(text.text, palette)
            cachedSource = text.text
        }
        val render = last!!
        return TransformedText(render.annotated, render.mapping)
    }
}

private val HEADING = Regex("""^(#{1,6})\s+""")
private val CHECKBOX = Regex("""^(\s*)([-*+])\s+\[([ xX]?)]\s*""")
private val BULLET = Regex("""^(\s*)([-*+])\s+""")
private val ORDERED = Regex("""^(\s*)\d+\.\s+""")
private val QUOTE = Regex("""^\s*>\s?""")
private val FENCE = Regex("""^\s*```""")
private val FENCE_INLINE = Regex("""^(\s*```\s*)(.*?)(\s*```\s*)$""")

private val BOLD_ITALIC = Regex("""\*\*\*([^*\n]+)\*\*\*""")
private val BOLD = Regex("""\*\*([^*\n]+)\*\*""")
private val ITALIC_STAR = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
private val ITALIC_UNDER = Regex("""(?<![\w_])_([^_\n]+)_(?![\w_])""")
private val STRIKE = Regex("""~~([^~\n]+)~~""")
private val CODE = Regex("""`([^`\n]+)`""")
private val LINK = Regex("""\[([^\]\n]*)]\(([^)\n]*)\)""")

/** Sisipan atau penghapusan pada teks sumber. Rentangnya tidak boleh tumpang tindih. */
private class Edit(
    val start: Int,
    val end: Int,
    val replacement: String = "",
    /**
     * Petakan offset tampil **tepat setelah** pengganti ini kembali ke [start],
     * bukan ke karakter sumber berikutnya.
     *
     * Dipakai pemisah baris. Pemisah itu karakter terakhir pada barisnya, jadi
     * ketukan di ruang kosong sebelah kanan baris menghasilkan offset sesudahnya
     * — dan tanpa pengalihan ini, offset itu berarti awal baris berikutnya:
     * kursor mendarat satu baris di bawah jari. Yang dikorbankan adalah ketukan
     * tepat di tepi paling kiri baris berikutnya, yang kini berarti ujung baris
     * di atasnya. Itu jauh lebih jarang, dan jaraknya satu baris juga.
     */
    val tieToStart: Boolean = false,
)

private class SpanAt(val style: SpanStyle, val start: Int, val end: Int)
private class ParaAt(val style: ParagraphStyle, val start: Int, val end: Int)
private class PendingLink(val start: Int, val end: Int, val url: String)

private class PendingBox(
    val contentStart: Int,
    val markerStart: Int,
    val markerEnd: Int,
    val lead: String,
    val checked: Boolean,
)

/**
 * Pengganti karakter baris baru di teks tampil.
 *
 * Setiap baris jadi satu paragraf tersendiri — hanya lewat paragraf-lah tinggi
 * baris judul dan indentasi kutipan bisa diatur per baris — dan paragraf sudah
 * memutus barisnya sendiri. Karakter "\n" yang tertinggal di dalamnya akan
 * menghasilkan satu baris kosong tambahan di bawah setiap baris, jadi ia
 * ditukar dengan karakter selebar nol.
 *
 * Ditukar, bukan dihapus: kalau dihapus, akhir satu baris dan awal baris
 * berikutnya menempati offset tampil yang sama, dan kursor tidak akan pernah
 * bisa berdiri di ujung baris — termasuk untuk menghapus pemisah barisnya.
 */
private const val LINE_BREAK = "​"

fun renderMarkdown(raw: String, palette: MarkdownPalette): MarkdownRender {
    val edits = ArrayList<Edit>()
    val spans = ArrayList<SpanAt>()
    val paras = ArrayList<ParaAt>()
    val quoteRuns = ArrayList<IntRange>()
    val codeRuns = ArrayList<IntRange>()
    val pending = ArrayList<PendingBox>()
    val pendingLinks = ArrayList<PendingLink>()

    val mono = SpanStyle(fontFamily = FontFamily.Monospace)
    val base = palette.baseSize.value
    val lines = raw.split("\n")

    var offset = 0
    var inFence = false
    var fenceContentStart = -1
    var quoteStart = -1
    var quoteEnd = -1

    fun closeQuoteRun() {
        if (quoteStart >= 0 && quoteEnd > quoteStart) quoteRuns += quoteStart until quoteEnd
        quoteStart = -1
    }

    /** Membuang baris beserta pemisah barisnya, supaya tidak ada baris kosong tertinggal. */
    fun dropLine(start: Int, end: Int) {
        edits += Edit(start, (end + 1).coerceAtMost(raw.length))
    }

    for ((index, line) in lines.withIndex()) {
        val start = offset
        val end = start + line.length
        offset = end + 1
        val last = index == lines.size - 1
        // Rentang paragraf mencakup pemisah barisnya sendiri: di sanalah kursor
        // berdiri saat berada di ujung baris.
        val paraEnd = if (last) end else end + 1

        // Baris kosong di paling akhir tidak punya pemisah sesudahnya, jadi
        // tidak punya apa pun untuk ditempati — kursor sesudah Enter akan
        // tertinggal di ujung baris sebelumnya. Pemisah terakhirnya digandakan:
        // satu menutup baris sebelumnya, satu lagi menjadi baris barunya.
        if (!last) {
            val tail = index == lines.size - 2 && lines.last().isEmpty()
            edits += if (tail) {
                Edit(end, end + 1, LINE_BREAK + LINE_BREAK)
            } else {
                Edit(end, end + 1, LINE_BREAK, tieToStart = true)
            }
        }

        if (inFence) {
            if (FENCE.containsMatchIn(line)) {
                if (start > fenceContentStart) codeRuns += fenceContentStart until (start - 1)
                dropLine(start, end)
                inFence = false
            } else {
                spans += SpanAt(mono, start, end)
                paras += ParaAt(blockStyle(palette), start, paraEnd)
            }
            continue
        }

        // Bentuk satu baris — ``` isi ``` — bukan Markdown baku, tapi itulah
        // yang digambar di berkas desain.
        val inlineFence = FENCE_INLINE.find(line)?.takeIf { it.groupValues[2].isNotEmpty() }
        if (inlineFence != null) {
            closeQuoteRun()
            val open = start + inlineFence.groupValues[1].length
            val close = open + inlineFence.groupValues[2].length
            edits += Edit(start, open)
            edits += Edit(close, end)
            spans += SpanAt(mono, open, close)
            paras += ParaAt(blockStyle(palette), start, paraEnd)
            codeRuns += open until close
            continue
        }

        if (FENCE.containsMatchIn(line)) {
            closeQuoteRun()
            dropLine(start, end)
            fenceContentStart = (end + 1).coerceAtMost(raw.length)
            inFence = true
            continue
        }

        // Kutipan dikupas lebih dulu dan terpisah dari yang lain, karena isi
        // kutipan boleh punya strukturnya sendiri: `> ## Judul` adalah subjudul
        // **di dalam** kutipan, bukan salah satu di antara keduanya.
        var contentStart = start
        var indent = 0f
        var lineHeight = base * LINE_FACTOR
        val quote = QUOTE.find(line)
        if (quote != null) {
            contentStart = start + quote.value.length
            edits += Edit(start, contentStart)
            indent += BLOCK_INDENT_DP
            if (quoteStart < 0) quoteStart = contentStart
            quoteEnd = end
        } else {
            closeQuoteRun()
        }

        val rest = line.substring(contentStart - start)
        val heading = HEADING.find(rest)
        val checkbox = if (heading == null) CHECKBOX.find(rest) else null
        var hangingIndent = indent

        when {
            heading != null -> {
                val markerEnd = contentStart + heading.value.length
                edits += Edit(contentStart, markerEnd)
                // Headings have proportional leading, with room above and below the glyphs.
                val (size, height) = when (heading.groupValues[1].length) {
                    1 -> 26f to 36f
                    2 -> 20f to 32f
                    else -> 18f to 30f
                }
                lineHeight = height
                spans += SpanAt(
                    SpanStyle(fontSize = (size / base).em, fontWeight = FontWeight.Bold),
                    markerEnd,
                    end,
                )
                contentStart = markerEnd
            }

            checkbox != null -> {
                val markerEnd = contentStart + checkbox.value.length
                edits += Edit(contentStart, markerEnd)
                val checked = checkbox.groupValues[3].lowercase() == "x"
                // Ruang kotak centang disediakan lewat indentasi paragraf, bukan
                // lewat karakter pengganti: 24 dp kotak + 8 dp jeda, persis
                // seperti baris tugas di Figma, dan lebarnya tidak ikut berubah
                // bersama hurufnya.
                indent += BOX_INDENT_DP
                hangingIndent = indent
                if (checked) {
                    spans += SpanAt(
                        SpanStyle(
                            color = palette.muted,
                            textDecoration = TextDecoration.LineThrough,
                        ),
                        markerEnd,
                        end,
                    )
                }
                pending += PendingBox(
                    contentStart = markerEnd,
                    markerStart = contentStart,
                    markerEnd = markerEnd,
                    lead = checkbox.groupValues[1] + checkbox.groupValues[2],
                    checked = checked,
                )
                contentStart = markerEnd
            }

            else -> {
                val bullet = BULLET.find(rest)
                val ordered = if (bullet == null) ORDERED.find(rest) else null
                if (bullet != null) {
                    val markerEnd = contentStart + bullet.value.length
                    // Tanda hubung diganti bulatan, bukan disembunyikan: poinnya
                    // memang harus terlihat.
                    edits += Edit(contentStart, markerEnd, bullet.groupValues[1] + "• ")
                    indent += BULLET_INDENT_DP
                    hangingIndent = indent + BULLET_HANG_DP
                    contentStart = markerEnd
                } else if (ordered != null) {
                    // Nomornya tetap apa adanya — Figma menampilkannya, dan
                    // nomor yang ditulis pengguna adalah nomor yang ia maksud.
                    indent += ORDERED_INDENT_DP
                    hangingIndent = indent + ORDERED_HANG_DP
                    contentStart += ordered.value.length
                }
            }
        }

        paras += ParaAt(
            ParagraphStyle(
                lineHeight = lineHeight.sp,
                lineHeightStyle = NOTE_LINE_HEIGHT_STYLE,
                textIndent = TextIndent(indent.sp, hangingIndent.sp),
            ),
            start,
            paraEnd,
        )

        styleInline(line, start, contentStart, palette, edits, spans, pendingLinks)
    }

    closeQuoteRun()
    // Pagar yang belum ditutup tetap dianggap blok kode sampai akhir catatan:
    // itu keadaan normal sepersekian detik setelah pagar pembuka diketik.
    if (inFence && fenceContentStart in 0 until raw.length) {
        codeRuns += fenceContentStart until raw.length
    }

    val (transformed, mapping) = applyEdits(raw, edits)

    val annotated = buildAnnotatedString {
        append(transformed)
        if (transformed.isNotEmpty()) {
            addStyle(SpanStyle(color = palette.ink), 0, transformed.length)
        }
        // Pemisah terakhir yang digandakan: bagian keduanya milik baris kosong
        // di bawahnya, jadi paragraf di atasnya tidak boleh ikut menelannya.
        val tailBreak = raw.endsWith("\n") && transformed.isNotEmpty()
        val limit = if (tailBreak) transformed.length - 1 else transformed.length
        for (p in paras) {
            val s = mapping.originalToTransformed(p.start)
            val e = mapping.originalToTransformed(p.end).coerceAtMost(limit)
            if (e > s) addStyle(p.style, s, e)
        }
        if (tailBreak) {
            addStyle(
                ParagraphStyle(lineHeight = (base * LINE_FACTOR).sp, lineHeightStyle = NOTE_LINE_HEIGHT_STYLE),
                transformed.length - 1,
                transformed.length,
            )
        }
        for (sp in spans) {
            val s = mapping.originalToTransformed(sp.start)
            val e = mapping.originalToTransformed(sp.end)
            if (e > s) addStyle(sp.style, s, e)
        }
    }

    fun blocksOf(runs: List<IntRange>, code: Boolean) = runs.mapNotNull { run ->
        val s = mapping.originalToTransformed(run.first.coerceIn(0, raw.length))
        val e = mapping.originalToTransformed((run.last + 1).coerceIn(0, raw.length))
        if (e > s) MarkdownBlock(s, e, code) else null
    }

    return MarkdownRender(
        annotated = annotated,
        mapping = mapping,
        blocks = blocksOf(quoteRuns, code = false) + blocksOf(codeRuns, code = true),
        links = pendingLinks.mapNotNull {
            val a = mapping.originalToTransformed(it.start)
            val b = mapping.originalToTransformed(it.end)
            if (b > a) MarkdownLink(a, b, it.url) else null
        },
        boxes = pending.map {
            MarkdownBox(
                transformed = mapping.originalToTransformed(it.contentStart),
                markerStart = it.markerStart,
                markerEnd = it.markerEnd,
                lead = it.lead,
                checked = it.checked,
            )
        },
    )
}

private fun blockStyle(palette: MarkdownPalette) = ParagraphStyle(
    lineHeight = (palette.baseSize.value * LINE_FACTOR).sp,
    lineHeightStyle = NOTE_LINE_HEIGHT_STYLE,
    textIndent = TextIndent(BLOCK_INDENT_DP.sp, BLOCK_INDENT_DP.sp),
)

/** Keep each separate Markdown paragraph as spacious as wrapped/source lines. */
private val NOTE_LINE_HEIGHT_STYLE = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Proportional,
    trim = LineHeightStyle.Trim.None,
)
private val LINE_FACTOR = Tokens.bodyLineHeight.value / Tokens.bodyTextSize.value

/** Jeda sebelum isi kutipan dan blok kode — 12 dp, node Figma 17:485. */
private const val BLOCK_INDENT_DP = 12f

/** Kotak centang 24 dp ditambah jeda 8 dp, sama seperti baris tugas — node 17:467. */
private const val BOX_INDENT_DP = 32f

/** Bulatan poin menggantung di kiri; sambungan barisnya lurus di bawah teks. */
private const val BULLET_INDENT_DP = 8f
private const val BULLET_HANG_DP = 16f
private const val ORDERED_INDENT_DP = 6f
private const val ORDERED_HANG_DP = 18f

/**
 * Penataan sebaris.
 *
 * Setiap kecocokan menandai rentangnya sebagai sudah terpakai, dan pola
 * berikutnya melewati rentang yang sudah ditandai. Tanpa itu, `**` akan
 * tertangkap dua kali — sekali sebagai tebal, sekali sebagai miring — dan
 * penandanya akan dihapus dua kali dari teks yang sama.
 */
private fun styleInline(
    line: String,
    lineStart: Int,
    contentStart: Int,
    palette: MarkdownPalette,
    edits: MutableList<Edit>,
    spans: MutableList<SpanAt>,
    links: MutableList<PendingLink>,
) {
    if (line.isEmpty()) return
    val claimed = BooleanArray(line.length)
    val from = contentStart - lineStart

    fun claim(range: IntRange): Boolean {
        if (range.first < from) return false
        for (i in range) if (claimed[i]) return false
        for (i in range) claimed[i] = true
        return true
    }

    fun emphasise(regex: Regex, markerLen: Int, style: SpanStyle) {
        for (m in regex.findAll(line)) {
            if (!claim(m.range)) continue
            val s = lineStart + m.range.first
            val e = lineStart + m.range.last + 1
            edits += Edit(s, s + markerLen)
            edits += Edit(e - markerLen, e)
            spans += SpanAt(style, s + markerLen, e - markerLen)
        }
    }

    // Tautan lebih dulu: label di dalamnya tidak boleh ikut ditafsirkan, dan
    // alamatnya kerap memuat garis bawah dan tanda bintang.
    for (m in LINK.findAll(line)) {
        if (!claim(m.range)) continue
        val s = lineStart + m.range.first
        val e = lineStart + m.range.last + 1
        val labelLen = m.groupValues[1].length
        edits += Edit(s, s + 1)
        edits += Edit(s + 1 + labelLen, e)
        spans += SpanAt(
            SpanStyle(color = palette.link, textDecoration = TextDecoration.Underline),
            s + 1,
            s + 1 + labelLen,
        )
        links += PendingLink(s + 1, s + 1 + labelLen, m.groupValues[2])
    }

    emphasise(CODE, 1, SpanStyle(fontFamily = FontFamily.Monospace))
    // Tiga bintang lebih dulu. Dibiarkan ke pola tebal, `***x***` tertangkap
    // mulai bintang kedua — tebal dengan sebutir bintang tersisa di tiap ujung.
    emphasise(
        BOLD_ITALIC,
        3,
        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
    )
    emphasise(BOLD, 2, SpanStyle(fontWeight = FontWeight.Bold))
    emphasise(STRIKE, 2, SpanStyle(textDecoration = TextDecoration.LineThrough))
    emphasise(ITALIC_STAR, 1, SpanStyle(fontStyle = FontStyle.Italic))
    emphasise(ITALIC_UNDER, 1, SpanStyle(fontStyle = FontStyle.Italic))
}

/**
 * Menjalankan [edits] atas [source], sekaligus membangun pemetaan offsetnya.
 *
 * Pemetaannya dua tabel penuh. Untuk catatan sepanjang puluhan ribu karakter
 * itu dua larik bilangan bulat — murah, dan hanya dibangun ulang saat teksnya
 * benar-benar berubah.
 */
private fun applyEdits(source: String, edits: List<Edit>): Pair<String, OffsetMapping> {
    val sorted = edits.filter { it.start in 0..it.end && it.end <= source.length }
        .sortedBy { it.start }
    val sb = StringBuilder(source.length)
    val forward = IntArray(source.length + 1)
    val back = ArrayList<Int>(source.length + 1)
    val ties = ArrayList<IntArray>()
    var cursor = 0

    for (edit in sorted) {
        // Rentang yang tumpang tindih dilewati, bukan dipaksakan: menerapkannya
        // setengah akan merusak pemetaannya, dan pemetaan yang rusak berarti
        // kursor meleset di seluruh sisa catatan.
        if (edit.start < cursor) continue
        while (cursor < edit.start) {
            forward[cursor] = sb.length
            back += cursor
            sb.append(source[cursor])
            cursor++
        }
        val tStart = sb.length
        for (c in edit.replacement) {
            back += edit.start
            sb.append(c)
        }
        if (edit.tieToStart) ties += intArrayOf(sb.length, edit.start)
        for (k in edit.start until edit.end) forward[k] = tStart
        cursor = edit.end
    }
    while (cursor < source.length) {
        forward[cursor] = sb.length
        back += cursor
        sb.append(source[cursor])
        cursor++
    }
    forward[source.length] = sb.length
    back += source.length
    for (tie in ties) if (tie[0] < back.size) back[tie[0]] = tie[1]

    val transformed = sb.toString()
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            forward[offset.coerceIn(0, source.length)]

        override fun transformedToOriginal(offset: Int): Int =
            back[offset.coerceIn(0, transformed.length)]
    }
    return transformed to mapping
}

/** Mengembalikan teks dengan satu kotak centang dibalik statusnya. */
fun toggleBox(text: String, box: MarkdownBox): String {
    if (box.markerEnd > text.length) return text
    val marker = "${box.lead} [${if (box.checked) " " else "x"}] "
    return text.substring(0, box.markerStart) + marker + text.substring(box.markerEnd)
}
