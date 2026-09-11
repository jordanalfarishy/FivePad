package com.fivepad.app.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

/**
 * Menata teks Markdown **tanpa mengubah jumlah karakternya**.
 *
 * Penanda seperti `**` tetap terlihat, hanya diredupkan. Pilihan ini disengaja:
 * menyembunyikan penanda akan memaksa pemetaan offset antara teks mentah dan
 * teks tampil, dan pemetaan itulah sumber klasik bug kursor meloncat, seleksi
 * meleset, dan penghapusan yang memakan karakter salah. Dengan panjang yang
 * tetap sama, pemetaannya identitas dan seluruh kelas bug itu tidak pernah ada.
 */
class MarkdownVisualTransformation(
    private val ink: Color,
    private val baseSize: TextUnit,
) : VisualTransformation {

    // filter() dipanggil pada setiap recomposition, termasuk saat hanya kursor
    // yang bergerak. Menyimpan hasil terakhir menghindari pemindaian ulang teks
    // yang sama berkali-kali.
    private var cachedSource: String? = null
    private var cachedResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val styled = if (source == cachedSource) {
            cachedResult!!
        } else {
            buildMarkdownAnnotated(source, ink).also {
                cachedSource = source
                cachedResult = it
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

private val HEADING = Regex("""^(#{1,3})\s+""")
private val CHECKBOX = Regex("""^(\s*[-*+]\s+\[([ xX])]\s+)""")
private val BULLET = Regex("""^(\s*[-*+]\s+)""")
private val ORDERED = Regex("""^(\s*\d+\.\s+)""")
private val QUOTE = Regex("""^(\s*>\s?)""")
private val FENCE = Regex("""^\s*```""")

private val BOLD = Regex("""\*\*([^*\n]+)\*\*""")
private val ITALIC_STAR = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
private val ITALIC_UNDER = Regex("""(?<![\w_])_([^_\n]+)_(?![\w_])""")
private val CODE = Regex("""`([^`\n]+)`""")
private val LINK = Regex("""\[([^\]\n]+)]\(([^)\n]+)\)""")

fun buildMarkdownAnnotated(raw: String, ink: Color): AnnotatedString = buildAnnotatedString {
    append(raw)

    val faint = ink.copy(alpha = 0.4f)
    val muted = ink.copy(alpha = 0.72f)

    var offset = 0
    var inFence = false

    for (line in raw.split("\n")) {
        val start = offset
        val end = start + line.length

        if (FENCE.containsMatchIn(line)) {
            inFence = !inFence
            addStyle(SpanStyle(color = faint, fontFamily = FontFamily.Monospace), start, end)
            offset = end + 1
            continue
        }

        if (inFence) {
            addStyle(SpanStyle(color = muted, fontFamily = FontFamily.Monospace), start, end)
            offset = end + 1
            continue
        }

        var contentStart = start

        val heading = HEADING.find(line)
        val checkbox = CHECKBOX.find(line)
        val quote = QUOTE.find(line)

        when {
            heading != null -> {
                val markerEnd = start + heading.value.length
                // Judul: H1 paling besar, turun bertahap ke H3.
                val scale = when (heading.groupValues[1].length) {
                    1 -> 1.5f
                    2 -> 1.28f
                    else -> 1.13f
                }
                addStyle(SpanStyle(color = faint), start, markerEnd)
                addStyle(
                    SpanStyle(fontSize = scale.em, fontWeight = FontWeight.Bold),
                    markerEnd,
                    end,
                )
                contentStart = markerEnd
            }

            checkbox != null -> {
                val markerEnd = start + checkbox.groupValues[1].length
                val checked = checkbox.groupValues[2].lowercase() == "x"
                addStyle(SpanStyle(color = if (checked) faint else muted), start, markerEnd)
                if (checked) {
                    addStyle(
                        SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough),
                        markerEnd,
                        end,
                    )
                }
                contentStart = markerEnd
            }

            quote != null -> {
                val markerEnd = start + quote.value.length
                addStyle(SpanStyle(color = faint), start, markerEnd)
                addStyle(SpanStyle(color = muted, fontStyle = FontStyle.Italic), markerEnd, end)
                contentStart = markerEnd
            }

            else -> {
                // Daftar berpoin diperiksa setelah kotak centang, karena
                // "- [ ] " juga cocok dengan pola poin biasa.
                val bullet = BULLET.find(line) ?: ORDERED.find(line)
                if (bullet != null) {
                    val markerEnd = start + bullet.value.length
                    addStyle(SpanStyle(color = faint), start, markerEnd)
                    contentStart = markerEnd
                }
            }
        }

        styleInline(line, start, contentStart, faint)
        offset = end + 1
    }
}

/**
 * Penataan sebaris. Tebal diproses lebih dulu agar `**` tidak lebih dulu
 * tertangkap sebagai penanda miring.
 */
private fun AnnotatedString.Builder.styleInline(
    line: String,
    lineStart: Int,
    contentStart: Int,
    faint: Color,
) {
    fun emphasise(regex: Regex, markerLen: Int, style: SpanStyle) {
        for (m in regex.findAll(line)) {
            val s = lineStart + m.range.first
            val e = lineStart + m.range.last + 1
            if (s < contentStart) continue
            addStyle(SpanStyle(color = faint), s, s + markerLen)
            addStyle(style, s + markerLen, e - markerLen)
            addStyle(SpanStyle(color = faint), e - markerLen, e)
        }
    }

    emphasise(BOLD, 2, SpanStyle(fontWeight = FontWeight.Bold))
    emphasise(ITALIC_STAR, 1, SpanStyle(fontStyle = FontStyle.Italic))
    emphasise(ITALIC_UNDER, 1, SpanStyle(fontStyle = FontStyle.Italic))
    emphasise(CODE, 1, SpanStyle(fontFamily = FontFamily.Monospace))

    for (m in LINK.findAll(line)) {
        val s = lineStart + m.range.first
        val e = lineStart + m.range.last + 1
        if (s < contentStart) continue
        val textLen = m.groupValues[1].length
        addStyle(SpanStyle(color = faint), s, s + 1)
        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), s + 1, s + 1 + textLen)
        addStyle(SpanStyle(color = faint), s + 1 + textLen, e)
    }
}
