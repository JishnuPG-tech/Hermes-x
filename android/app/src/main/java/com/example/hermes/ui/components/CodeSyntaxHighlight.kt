package com.example.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hermes.theme.*

/**
 * Builds syntax highlighted AnnotatedString matching the exact Claude Android palette in Screenshot 4.
 *
 * Palette:
 * - Keywords (val, fun, class, dependencies, import, package, return): Lavender (#D2A8FF)
 * - Types / Identifiers: Warm Orange (#FFA657)
 * - Functions / Methods: Sky Blue (#79C0FF)
 * - Numbers / Hex literals: Mint Green (#7EE787)
 * - Strings: Light Cyan (#A5D6FF)
 * - Comments: Slate Gray (#6E7681)
 */
fun buildSyntaxHighlightedCode(rawCode: String, language: String = "kotlin"): AnnotatedString {
    val keywords = setOf(
        "val", "var", "fun", "class", "object", "interface", "import", "package",
        "return", "if", "else", "when", "for", "while", "dependencies", "implementation",
        "plugins", "id", "true", "false", "null", "private", "public", "protected",
        "override", "companion", "data", "sealed", "const", "by", "remember", "mutableStateOf"
    )

    val types = setOf(
        "String", "Int", "Boolean", "Float", "Double", "Long", "Color", "Modifier",
        "Dp", "Text", "Box", "Column", "Row", "Composable", "Unit", "List", "Map", "Set"
    )

    return buildAnnotatedString {
        val lines = rawCode.split("\n")
        lines.forEachIndexed { lineIdx, line ->
            var i = 0
            val trimmed = line.trimStart()

            // Check full-line or end-of-line comment
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                pushStyle(SpanStyle(color = SyntaxComment))
                append(line)
                pop()
            } else {
                // Tokenize simple words, strings, literals
                while (i < line.length) {
                    // Check comment
                    if (i < line.length - 1 && line[i] == '/' && line[i + 1] == '/') {
                        pushStyle(SpanStyle(color = SyntaxComment))
                        append(line.substring(i))
                        pop()
                        i = line.length
                        break
                    }

                    // Check string literal
                    if (line[i] == '"' || line[i] == '\'') {
                        val quote = line[i]
                        val start = i
                        i++
                        while (i < line.length && line[i] != quote) {
                            if (line[i] == '\\' && i + 1 < line.length) i++
                            i++
                        }
                        if (i < line.length) i++ // include closing quote
                        pushStyle(SpanStyle(color = SyntaxString))
                        append(line.substring(start, i))
                        pop()
                        continue
                    }

                    // Check identifiers / words
                    if (line[i].isLetter() || line[i] == '_') {
                        val start = i
                        while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                            i++
                        }
                        val word = line.substring(start, i)
                        when {
                            keywords.contains(word) -> {
                                pushStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Normal))
                                append(word)
                                pop()
                            }
                            types.contains(word) -> {
                                pushStyle(SpanStyle(color = SyntaxType))
                                append(word)
                                pop()
                            }
                            // If followed by '(' -> function call
                            i < line.length && line[i] == '(' -> {
                                pushStyle(SpanStyle(color = SyntaxFunction))
                                append(word)
                                pop()
                            }
                            else -> {
                                // Default identifier
                                pushStyle(SpanStyle(color = TextPrimaryWarm))
                                append(word)
                                pop()
                            }
                        }
                        continue
                    }

                    // Check numbers / hex
                    if (line[i].isDigit()) {
                        val start = i
                        if (line.substring(start).startsWith("0x", ignoreCase = true)) {
                            i += 2
                            while (i < line.length && (line[i].isDigit() || line[i] in 'a'..'f' || line[i] in 'A'..'F')) {
                                i++
                            }
                        } else {
                            while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'f' || line[i] == 'L')) {
                                i++
                            }
                        }
                        pushStyle(SpanStyle(color = SyntaxLiteral))
                        append(line.substring(start, i))
                        pop()
                        continue
                    }

                    // Punctuation and symbols
                    pushStyle(SpanStyle(color = TextPrimaryWarm))
                    append(line[i].toString())
                    pop()
                    i++
                }
            }

            if (lineIdx < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Pixel-perfect Claude Code Block Card matching Screenshot 4.
 */
@Composable
fun ClaudeCodeBlock(
    code: String,
    language: String = "Kotlin",
    modifier: Modifier = Modifier,
    onCopy: () -> Unit = {}
) {
    val annotatedCode = buildSyntaxHighlightedCode(code, language)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SyntaxCodeBg)
            .border(1.dp, SyntaxCodeBorder, RoundedCornerShape(12.dp))
    ) {
        // Code Block Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language,
                style = HermesTypography.labelSmall.copy(
                    fontSize = 13.sp,
                    color = Color(0xFF8E8B82),
                    fontWeight = FontWeight.Medium
                )
            )

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onCopy)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color(0xFF8E8B82),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Code Content with Horizontal Scrolling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = annotatedCode,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Inline Cyan Pill Badge matching Screenshot 4 (e.g. `implementation(...)`)
 */
@Composable
fun InlineCyanCodePill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SyntaxPillBg)
            .padding(horizontal = 8.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = text,
            style = HermesTypography.bodySmall.copy(
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                color = SyntaxPillText,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
