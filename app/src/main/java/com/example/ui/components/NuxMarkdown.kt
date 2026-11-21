package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalNuxColors
import com.example.ui.theme.MonoFontFamily

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String, val prefix: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
}

@Composable
fun NuxMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val primary = MaterialTheme.colorScheme.primary
    val nux = LocalNuxColors.current

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MdBlock.Heading -> {
                        val size = when (block.level) {
                            1 -> 20.sp
                            2 -> 17.5.sp
                            else -> 16.sp
                        }
                        Text(
                            text = parseInlineMarkdown(block.text, primary, nux.codeBackground),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = size,
                                fontWeight = FontWeight.Bold
                            ),
                            color = textColor
                        )
                    }

                    is MdBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.text, primary, nux.codeBackground),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.5.sp,
                                lineHeight = 27.sp
                            ),
                            color = textColor
                        )
                    }

                    is MdBlock.Bullet -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = block.prefix,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = parseInlineMarkdown(block.text, primary, nux.codeBackground),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.5.sp,
                                    lineHeight = 26.sp
                                ),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is MdBlock.Quote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(primary.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(22.dp)
                                    .background(primary, RoundedCornerShape(999.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(block.text, primary, nux.codeBackground),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.9f)
                            )
                        }
                    }

                    is MdBlock.Code -> {
                        CodeBlockView(lang = block.lang, code = block.code)
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(lang: String, code: String) {
    val nux = LocalNuxColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(nux.codeBackground)
                .border(1.dp, nux.codeBorder, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lang.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = Color(0xFF9CA3AF)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) nux.success else Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MonoFontFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

private fun parseMarkdownBlocks(input: String): List<MdBlock> {
    val result = mutableListOf<MdBlock>()
    val lines = input.replace("\r\n", "\n").split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeBuf = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (codeBuf.isNotEmpty()) codeBuf.append('\n')
                codeBuf.append(lines[i])
                i++
            }
            result.add(MdBlock.Code(lang, codeBuf.toString()))
            i++
            continue
        }
        if (trimmed.isEmpty()) {
            i++
            continue
        }
        when {
            trimmed.startsWith("### ") -> result.add(MdBlock.Heading(3, trimmed.removePrefix("### ")))
            trimmed.startsWith("## ") -> result.add(MdBlock.Heading(2, trimmed.removePrefix("## ")))
            trimmed.startsWith("# ") -> result.add(MdBlock.Heading(1, trimmed.removePrefix("# ")))
            trimmed.startsWith("> ") -> result.add(MdBlock.Quote(trimmed.removePrefix("> ")))
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                result.add(MdBlock.Bullet(trimmed.drop(2), "•"))
            }
            Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                val dot = trimmed.indexOf('.')
                result.add(MdBlock.Bullet(trimmed.substring(dot + 1).trim(), trimmed.substring(0, dot + 1)))
            }
            else -> {
                val pBuf = StringBuilder(trimmed)
                while (i + 1 < lines.size) {
                    val next = lines[i + 1].trim()
                    if (next.isEmpty() || next.startsWith("#") || next.startsWith("```") ||
                        next.startsWith("- ") || next.startsWith("* ") || next.startsWith("> ") ||
                        Regex("^\\d+\\.\\s+.*").matches(next)
                    ) break
                    pBuf.append('\n').append(next)
                    i++
                }
                result.add(MdBlock.Paragraph(pBuf.toString()))
            }
        }
        i++
    }
    return result
}

private fun parseInlineMarkdown(
    text: String,
    primaryColor: Color,
    codeBg: Color
): AnnotatedString = buildAnnotatedString {
    var idx = 0
    while (idx < text.length) {
        when {
            text.startsWith("**", idx) -> {
                val end = text.indexOf("**", idx + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(idx + 2, end))
                    }
                    idx = end + 2
                } else {
                    append("**")
                    idx += 2
                }
            }
            text[idx] == '`' -> {
                val end = text.indexOf('`', idx + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = MonoFontFamily,
                            background = codeBg.copy(alpha = 0.25f),
                            color = primaryColor
                        )
                    ) {
                        append(" ${text.substring(idx + 1, end)} ")
                    }
                    idx = end + 1
                } else {
                    append('`')
                    idx++
                }
            }
            text[idx] == '*' -> {
                val end = text.indexOf('*', idx + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(idx + 1, end))
                    }
                    idx = end + 1
                } else {
                    append('*')
                    idx++
                }
            }
            else -> {
                append(text[idx])
                idx++
            }
        }
    }
}
