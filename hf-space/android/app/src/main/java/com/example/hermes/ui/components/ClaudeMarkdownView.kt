package com.example.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

/**
 * Structured Markdown AST node for deterministic Compose rendering.
 */
sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class Table(val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
    data object EmptySpacer : MarkdownBlock
}

/**
 * Parses inline Markdown formatting (bold **, italic *, inline code ``, etc.)
 * into an AnnotatedString with ZERO raw syntax symbols leaking to the UI.
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Check markdown link: [label](url)
            if (text[i] == '[') {
                val endLabel = text.indexOf(']', i + 1)
                if (endLabel != -1 && endLabel + 1 < len && text[endLabel + 1] == '(') {
                    val endUrl = text.indexOf(')', endLabel + 2)
                    if (endUrl != -1) {
                        val label = text.substring(i + 1, endLabel)
                        val url = text.substring(endLabel + 2, endUrl).trim()
                        val linkStyles = TextLinkStyles(
                            style = SpanStyle(
                                color = BrandCoral,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        pushLink(LinkAnnotation.Url(url = url, styles = linkStyles))
                        append(if (label.isNotBlank()) label else url)
                        pop()
                        i = endUrl + 1
                        continue
                    }
                }
            }

            // Check bare auto-links: http:// or https://
            if (i + 7 < len && (text.startsWith("http://", i) || text.startsWith("https://", i))) {
                var endUrl = i
                while (endUrl < len && !text[endUrl].isWhitespace() && text[endUrl] !in listOf(')', ']', '>', '"', '\'')) {
                    endUrl++
                }
                var rawUrl = text.substring(i, endUrl)
                while (rawUrl.endsWith(".") || rawUrl.endsWith(",") || rawUrl.endsWith(";")) {
                    rawUrl = rawUrl.dropLast(1)
                    endUrl--
                }
                val linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        color = BrandCoral,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                pushLink(LinkAnnotation.Url(url = rawUrl, styles = linkStyles))
                append(rawUrl)
                pop()
                i = endUrl
                continue
            }

            // Check inline code: `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val codeContent = text.substring(i + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMono,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SyntaxPillText,
                            background = SyntaxPillBg
                        )
                    )
                    append(" $codeContent ")
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Check spoilers: ||spoiler||
            if (i + 1 < len && text.substring(i, i + 2) == "||") {
                val end = text.indexOf("||", i + 2)
                if (end != -1) {
                    val spoilerContent = text.substring(i + 2, end)
                    pushStyle(SpanStyle(color = Color(0xFFB0AEA5), background = Color(0xFF262523)))
                    append(" $spoilerContent ")
                    pop()
                    i = end + 2
                    continue
                } else {
                    i += 2
                    continue
                }
            }

            // Check strikethrough: ~~text~~
            if (i + 1 < len && text.substring(i, i + 2) == "~~") {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    val strikeContent = text.substring(i + 2, end)
                    pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                    append(strikeContent)
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Check bold & italic: ***bold italic***
            if (i + 2 < len && text.substring(i, i + 3) == "***") {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    val boldItalicContent = text.substring(i + 3, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(boldItalicContent)
                    pop()
                    i = end + 3
                    continue
                }
            }

            // Check bold: **bold**
            if (i + 1 < len && text.substring(i, i + 2) == "**") {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val boldContent = text.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PureWhite))
                    append(boldContent)
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Check bold: __bold__
            if (i + 1 < len && text.substring(i, i + 2) == "__") {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    val boldContent = text.substring(i + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PureWhite))
                    append(boldContent)
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Check single asterisk italic: *italic* (only if not bullet)
            if (text[i] == '*' && i + 1 < len && text[i + 1] != ' ') {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    val italicContent = text.substring(i + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicContent)
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Check single underscore italic: _italic_
            if (text[i] == '_' && i + 1 < len && text[i + 1] != ' ') {
                val end = text.indexOf('_', i + 1)
                if (end != -1 && end > i + 1) {
                    val italicContent = text.substring(i + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicContent)
                    pop()
                    i = end + 1
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}

/**
 * Parses raw markdown text into a List of MarkdownBlock nodes.
 */
fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val cleanLines = content.replace("\r\n", "\n").split("\n")

    var inCodeBlock = false
    var codeLanguage = "kotlin"
    val codeBuffer = StringBuilder()

    var inTable = false
    val tableRows = mutableListOf<List<String>>()

    fun flushTable() {
        if (tableRows.isNotEmpty()) {
            blocks.add(MarkdownBlock.Table(tableRows.toList()))
            tableRows.clear()
            inTable = false
        }
    }

    for (line in cleanLines) {
        val trimmed = line.trim()

        // 1. Code Block Fence (```)
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(
                    MarkdownBlock.CodeBlock(
                        code = codeBuffer.toString().trimEnd(),
                        language = if (codeLanguage.isBlank()) "Code" else codeLanguage.replaceFirstChar { it.uppercase() }
                    )
                )
                codeBuffer.clear()
                inCodeBlock = false
            } else {
                flushTable()
                inCodeBlock = true
                codeLanguage = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuffer.append(line).append("\n")
            continue
        }

        // 2. Table row detection (| col1 | col2 |)
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            if (trimmed.contains("---")) {
                continue
            }
            val cols = trimmed.split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (cols.isNotEmpty()) {
                inTable = true
                tableRows.add(cols)
                continue
            }
        } else if (inTable) {
            flushTable()
        }

        // 3. Horizontal Rule
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.Divider)
            continue
        }

        // 4. Headings
        if (trimmed.startsWith("### ")) {
            blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ")))
            continue
        }
        if (trimmed.startsWith("## ")) {
            blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ")))
            continue
        }
        if (trimmed.startsWith("# ")) {
            blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ")))
            continue
        }

        // 5. Blockquotes (> quote)
        if (trimmed.startsWith(">")) {
            blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix(">").trim()))
            continue
        }

        // 6. Bullet lists (- bullet, * bullet)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            blocks.add(MarkdownBlock.BulletItem(trimmed.substring(2)))
            continue
        }

        // 7. Numbered lists (1. , 2. )
        val numberMatch = Regex("""^(\d+)\.\s+(.*)$""").find(trimmed)
        if (numberMatch != null) {
            val num = numberMatch.groupValues[1]
            val itemText = numberMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, itemText))
            continue
        }

        // 8. Empty line -> Spacer
        if (trimmed.isEmpty()) {
            blocks.add(MarkdownBlock.EmptySpacer)
            continue
        }

        // 9. Regular Paragraph
        blocks.add(MarkdownBlock.Paragraph(line))
    }

    flushTable()
    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        blocks.add(
            MarkdownBlock.CodeBlock(
                code = codeBuffer.toString().trimEnd(),
                language = if (codeLanguage.isBlank()) "Code" else codeLanguage.replaceFirstChar { it.uppercase() }
            )
        )
    }

    return blocks
}

/**
 * Pure Claude Markdown Composable.
 */
@Composable
fun ClaudeMarkdownView(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 23.sp
                        2 -> 20.sp
                        else -> 18.sp
                    }
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = HermesTypography.headlineMedium.copy(
                            fontFamily = AnthropicSerif,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    ClaudeCodeBlock(
                        code = block.code,
                        language = block.language
                    )
                }

                is MarkdownBlock.Table -> {
                    TableCard(rows = block.rows)
                }

                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(26.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(BrandCoral)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = HermesTypography.bodyLarge.copy(
                                fontFamily = AnthropicSans,
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PureWhite,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = HermesTypography.bodyLarge.copy(
                                color = BrandCoral,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = HermesTypography.bodyLarge.copy(
                                fontFamily = AnthropicSans,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PureWhite,
                                lineHeight = 26.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = HermesTypography.bodyMedium.copy(
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = HermesTypography.bodyLarge.copy(
                                fontFamily = AnthropicSans,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PureWhite,
                                lineHeight = 26.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = HermesTypography.bodyLarge.copy(
                            fontFamily = AnthropicSans,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            lineHeight = 26.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = HairlineDivider,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                is MarkdownBlock.EmptySpacer -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Clean Dark Table Card component for Markdown tables.
 */
@Composable
private fun TableCard(rows: List<List<String>>) {
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF191816))
            .border(1.dp, Color(0xFF2A2826), RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        rows.forEachIndexed { rIdx, cols ->
            val isHeader = rIdx == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isHeader) Color(0xFF22211F) else Color.Transparent)
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cols.forEach { col ->
                    Text(
                        text = parseInlineMarkdown(col),
                        style = HermesTypography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        ),
                        modifier = Modifier.widthIn(min = 90.dp)
                    )
                }
            }
            if (rIdx < rows.size - 1) {
                HorizontalDivider(color = Color(0xFF2A2826))
            }
        }
    }
}
