package com.fivepad.app.ui.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class MarkdownActionsTest {
    private fun selected(text: String) = TextFieldValue(text, TextRange(0, text.length))

    @Test fun inlineFormatsRoundTripTheSelectedText() {
        listOf(MarkdownAction.BOLD, MarkdownAction.ITALIC, MarkdownAction.STRIKE, MarkdownAction.CODE).forEach { action ->
            val formatted = applyMarkdown(selected("café 👋"), action)
            assertEquals(action.syntax + "café 👋" + action.syntax, formatted.text)
            assertEquals("café 👋", applyMarkdown(formatted, action).text)
        }
    }

    @Test fun headingsListsQuotesAndFencesRoundTripMultipleLines() {
        listOf(MarkdownAction.HEADER, MarkdownAction.SUB_HEADER, MarkdownAction.LIST,
            MarkdownAction.ORDERED_LIST, MarkdownAction.TODO, MarkdownAction.QUOTE, MarkdownAction.CODE_BLOCK).forEach { action ->
            val formatted = applyMarkdown(selected("first\nsecond"), action)
            assertEquals(action.name, "first\nsecond", applyMarkdown(formatted, action).text)
        }
    }

    @Test fun headingsCanBeAddedInsideQuotes() {
        assertEquals("> ## title", applyMarkdown(selected("> title"), MarkdownAction.SUB_HEADER).text)
    }

    @Test fun enterContinuesListsAndExitsAnEmptyItem() {
        listOf("- item" to "- item\n- ", "3. item" to "3. item\n4. ", "- [x] done" to "- [x] done\n- [ ] ").forEach { (before, expected) ->
            val result = continueListOnNewline(TextFieldValue(before, TextRange(before.length)), TextFieldValue("$before\n", TextRange(before.length + 1)))
            assertEquals(expected, result?.text)
        }
        val empty = "- [ ] "
        assertEquals("", continueListOnNewline(TextFieldValue(empty, TextRange(empty.length)), TextFieldValue("$empty\n", TextRange(empty.length + 1)))?.text)
    }

    @Test fun linksPreserveSelectedLabelsAndCanBeRemoved() {
        val linked = insertLink(selected("Example"), "Example", "https://example.com")
        assertEquals("[Example](https://example.com)", linked.text)
        assertEquals("Example", unlink(selected(linked.text)).text)
    }
}
