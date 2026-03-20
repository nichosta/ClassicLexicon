package com.greeklexicon.app.data

import android.util.Xml
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.xmlpull.v1.XmlPullParser

/**
 * Renders dictionary XML entry content into a styled AnnotatedString for Compose.
 */
object XmlEntryRenderer {

    private val greekColor = Color(0xFF1A237E) // dark blue for Greek text
    private val translationColor = Color(0xFF2E7D32) // green for translations
    private val authorColor = Color(0xFF6A1B9A) // purple for author citations
    private val senseColor = Color(0xFF37474F) // dark grey for sense labels
    private val nestedSenseColor = Color(0xFF1565C0)
    private val deepSenseColor = Color(0xFF2E7D32)
    private val refColor = Color(0xFF0277BD) // blue for cross-references
    private val smithHallSectionColor = Color(0xFF6D4C41)
    private val smithHallSubsectionColor = Color(0xFF00897B)
    private val smithHallDefinitionColor = Color(0xFF455A64)

    fun render(xmlContent: String, entryType: String? = null): AnnotatedString {
        if (entryType == "smithhall" || looksLikeSmithHall(xmlContent)) {
            return runCatching { renderSmithHall(xmlContent) }
                .getOrElse { AnnotatedString(xmlContent.replace(Regex("<[^>]+>"), "")) }
        }

        return buildAnnotatedString {
            try {
                val parser = Xml.newPullParser()
                parser.setInput(xmlContent.reader())
                parseEntry(parser, this)
            } catch (e: Exception) {
                // Fallback: strip tags and show plain text
                append(xmlContent.replace(Regex("<[^>]+>"), ""))
            }
        }
    }

    private fun looksLikeSmithHall(xmlContent: String): Boolean {
        return xmlContent.contains("<sense") && xmlContent.contains("marker=") && xmlContent.contains("<div>")
    }

    private fun renderSmithHall(xmlContent: String): AnnotatedString {
        return buildAnnotatedString {
            var boldDepth = 0
            var italicDepth = 0
            var refDepth = 0
            var biblDepth = 0
            var authorDepth = 0
            var foreignDepth = 0
            var lastChar: Char? = null

            fun appendRaw(text: String) {
                if (text.isEmpty()) return
                append(text)
                lastChar = text.last()
            }

            fun appendSpaceIfNeeded() {
                if (lastChar != null && lastChar != ' ' && lastChar != '\n' && lastChar != '\u00A0') {
                    appendRaw(" ")
                }
            }

            fun appendNewlineIfNeeded() {
                if (length > 0 && lastChar != '\n') {
                    appendRaw("\n")
                }
            }

            fun appendBlankLineIfNeeded() {
                if (length == 0) {
                    return
                }
                if (lastChar != '\n') {
                    appendRaw("\n\n")
                } else {
                    appendRaw("\n")
                }
            }

            fun currentStyle(): SpanStyle? {
                return when {
                    boldDepth > 0 -> SpanStyle(fontWeight = FontWeight.Bold, color = greekColor)
                    refDepth > 0 -> SpanStyle(color = refColor)
                    authorDepth > 0 -> SpanStyle(color = authorColor, fontStyle = FontStyle.Italic)
                    foreignDepth > 0 -> SpanStyle(color = greekColor, fontStyle = FontStyle.Italic)
                    biblDepth > 0 -> SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic)
                    italicDepth > 0 -> SpanStyle(fontStyle = FontStyle.Italic)
                    else -> null
                }
            }

            fun appendStyled(text: String, style: SpanStyle?) {
                if (text.isEmpty()) return
                if (style != null) {
                    withStyle(style) { append(text) }
                    lastChar = text.last()
                } else {
                    appendRaw(text)
                }
            }

            fun appendSmithHallDefinitionLead(text: String, baseStyle: SpanStyle?) {
                val colonIndex = text.indexOf(':')
                if (colonIndex <= 0) {
                    appendStyled(text, baseStyle)
                    return
                }

                val lead = text.substring(0, colonIndex + 1)
                val remainder = text.substring(colonIndex + 1)
                val leadStyle = (baseStyle ?: SpanStyle()).merge(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = smithHallDefinitionColor,
                    )
                )

                appendStyled(lead, leadStyle)
                appendStyled(remainder, baseStyle)
            }

            fun formatInlineSmithHallText(text: String): String {
                return text
                    .replace(Regex("\\s+"), " ")
                    .replace(Regex("(?<=[:.])\\s+(?=(?:X|IX|IV|V?I{1,3})\\.\\s)"), "\n\n")
                    .replace(Regex("(?<=[:.])\\s+(?=\\d+\\.\\s)"), "\n\u00A0\u00A0")
                    .replace(Regex("(?<=\\.)\\s+(?=(Phr\\.:|Dimin\\.:|\\[N\\.B\\.--))"), "\n")
            }

            fun appendSmithHallHeadingText(text: String, baseStyle: SpanStyle?) {
                val topLevelMatch = Regex("^(X|IX|IV|V?I{1,3})\\.\\s+").find(text)
                if (topLevelMatch != null) {
                    appendStyled(
                        topLevelMatch.value,
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = smithHallSectionColor,
                        )
                    )
                    appendSmithHallDefinitionLead(text.removePrefix(topLevelMatch.value), baseStyle)
                    return
                }

                val numberedMatch = Regex("^\\d+\\.\\s+").find(text)
                if (numberedMatch != null) {
                    appendStyled(
                        numberedMatch.value,
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = smithHallSubsectionColor,
                        )
                    )
                    appendSmithHallDefinitionLead(text.removePrefix(numberedMatch.value), baseStyle)
                    return
                }

                val labelMatch = Regex("^(Phr\\.:|Dimin\\.:|\\[N\\.B\\.--[^\\]]*\\])\\s*").find(text)
                if (labelMatch != null) {
                    appendStyled(
                        labelMatch.value,
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = smithHallSectionColor,
                        )
                    )
                    appendSmithHallDefinitionLead(text.removePrefix(labelMatch.value), baseStyle)
                    return
                }

                appendStyled(text, baseStyle)
            }

            fun appendFormattedSmithHallText(text: String) {
                val formatted = formatInlineSmithHallText(text)
                val lines = formatted.split("\n")

                lines.forEachIndexed { index, rawLine ->
                    if (index > 0) {
                        appendRaw("\n")
                    }

                    if (rawLine.isEmpty()) {
                        return@forEachIndexed
                    }

                    val leadingIndent = rawLine.takeWhile { it == ' ' || it == '\u00A0' }
                    val content = rawLine.trim()
                    if (content.isEmpty()) {
                        return@forEachIndexed
                    }

                    if (leadingIndent.isNotEmpty()) {
                        appendRaw(leadingIndent.replace(' ', '\u00A0'))
                    } else if (rawLine.first().isWhitespace()) {
                        appendSpaceIfNeeded()
                    }

                    appendSmithHallHeadingText(content, currentStyle())
                }
            }

            val tokenRegex = Regex("""<[^>]+>|[^<]+""")
            val tagNameRegex = Regex("""^</?\s*([A-Za-z0-9]+)""")
            val attrRegex = Regex("([A-Za-z_:][-A-Za-z0-9_:.]*)=\"([^\"]*)\"")

            for (match in tokenRegex.findAll(xmlContent)) {
                val token = match.value
                if (!token.startsWith("<")) {
                    val normalized = token
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                    if (normalized.isBlank()) {
                        if (boldDepth > 0 || italicDepth > 0 || refDepth > 0 || biblDepth > 0 || authorDepth > 0 || foreignDepth > 0) {
                            appendSpaceIfNeeded()
                        }
                    } else {
                        appendFormattedSmithHallText(normalized)
                    }
                    continue
                }

                val isClosing = token.startsWith("</")
                val isSelfClosing = token.endsWith("/>")
                val name = tagNameRegex.find(token)?.groupValues?.get(1)?.lowercase() ?: continue
                val attrs = attrRegex.findAll(token).associate { it.groupValues[1] to it.groupValues[2] }

                if (!isClosing) {
                    when (name) {
                        "sense" -> {
                            val level = attrs["level"]?.toIntOrNull() ?: 1
                            val marker = attrs["marker"].orEmpty()
                            if (level <= 2) {
                                appendBlankLineIfNeeded()
                            } else {
                                appendNewlineIfNeeded()
                            }
                            appendRaw("\u00A0\u00A0".repeat((level - 1).coerceAtLeast(0)))
                            if (marker.isNotBlank()) {
                                appendStyled(
                                    formatSenseMarker(marker),
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = colorForSenseLevel(level),
                                    )
                                )
                            }
                        }
                        "div", "p" -> appendBlankLineIfNeeded()
                        "br", "lb" -> appendNewlineIfNeeded()
                        "b" -> boldDepth++
                        "hi" -> if (attrs["rend"] == "italic") italicDepth++
                        "a", "ref" -> refDepth++
                        "bibl" -> biblDepth++
                        "author" -> authorDepth++
                        "foreign" -> foreignDepth++
                    }
                }

                if (isClosing || isSelfClosing) {
                    when (name) {
                        "sense" -> appendNewlineIfNeeded()
                        "div", "p" -> appendBlankLineIfNeeded()
                        "br", "lb" -> appendNewlineIfNeeded()
                        "b" -> boldDepth = (boldDepth - 1).coerceAtLeast(0)
                        "hi" -> italicDepth = (italicDepth - 1).coerceAtLeast(0)
                        "a", "ref" -> refDepth = (refDepth - 1).coerceAtLeast(0)
                        "bibl" -> biblDepth = (biblDepth - 1).coerceAtLeast(0)
                        "author" -> authorDepth = (authorDepth - 1).coerceAtLeast(0)
                        "foreign" -> foreignDepth = (foreignDepth - 1).coerceAtLeast(0)
                    }
                }
            }
        }
    }

    private fun parseEntry(parser: XmlPullParser, builder: AnnotatedString.Builder) {
        var depth = 0
        var ignoredDepth = 0
        var insideOrth = false
        var insideBold = false
        var insideTr = false
        var insideForeign = false
        var insideAuthor = false
        var insideRef = false
        var insideBibl = false
        var insideItalic = false
        var lastChar: Char? = null

        fun appendRaw(text: String) {
            if (text.isEmpty()) {
                return
            }
            builder.append(text)
            lastChar = text.last()
        }

        fun appendSpaceIfNeeded() {
            if (lastChar != null && lastChar != ' ' && lastChar != '\n' && lastChar != '\t') {
                appendRaw(" ")
            }
        }

        fun appendNewlineIfNeeded() {
            if (builder.length > 0 && lastChar != '\n') {
                appendRaw("\n")
            }
        }

        fun appendStyled(text: String, style: SpanStyle?) {
            if (text.isEmpty()) {
                return
            }
            if (style != null) {
                builder.withStyle(style) {
                    append(text)
                }
                lastChar = text.last()
            } else {
                appendRaw(text)
            }
        }

        fun normalizeText(text: String): Pair<String, Boolean> {
            val collapsed = text.replace(Regex("\\s+"), " ")
            val needsLeadingSpace = collapsed.startsWith(" ")
            return collapsed.trim() to needsLeadingSpace
        }

        fun formatSenseMarker(marker: String): String {
            val trimmed = marker.trim()
            if (trimmed.isEmpty()) {
                return ""
            }
            return if (trimmed.last() in listOf('.', ':', ')')) {
                "$trimmed "
            } else {
                "$trimmed. "
            }
        }

        fun colorForSenseLevel(level: Int): Color {
            return when {
                level <= 1 -> senseColor
                level == 2 -> nestedSenseColor
                else -> deepSenseColor
            }
        }

        while (true) {
            val event = parser.next()
            when (event) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (ignoredDepth > 0) {
                        ignoredDepth++
                        continue
                    }

                    when (parser.name) {
                        "style", "script" -> {
                            ignoredDepth = 1
                        }
                        "orth" -> {
                            insideOrth = true
                        }
                        "b" -> {
                            insideBold = true
                        }
                        "sense" -> {
                            val n = parser.getAttributeValue(null, "n")
                                ?: parser.getAttributeValue(null, "marker")
                                ?: ""
                            val level = parser.getAttributeValue(null, "level")?.toIntOrNull() ?: 1

                            appendNewlineIfNeeded()
                            appendRaw("  ".repeat((level - 1).coerceAtLeast(0)))

                            if (n.isNotEmpty()) {
                                appendStyled(
                                    formatSenseMarker(n),
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = colorForSenseLevel(level),
                                    )
                                )
                            }
                        }
                        "tr" -> {
                            insideTr = true
                        }
                        "foreign" -> {
                            insideForeign = true
                        }
                        "author" -> {
                            insideAuthor = true
                        }
                        "ref" -> {
                            insideRef = true
                        }
                        "a" -> {
                            insideRef = true
                        }
                        "bibl" -> {
                            insideBibl = true
                        }
                        "hi" -> {
                            insideItalic = parser.getAttributeValue(null, "rend") == "italic"
                        }
                        "br", "lb" -> {
                            appendNewlineIfNeeded()
                        }
                        "itype" -> {
                            // Show inflection type inline
                        }
                        "gen" -> {
                            // Show gender inline
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (ignoredDepth > 0) {
                        continue
                    }

                    val text = parser.text ?: ""
                    if (text.isBlank()) {
                        if (insideOrth || insideTr || insideBold || insideItalic || insideForeign || insideAuthor || insideRef || insideBibl) {
                            appendSpaceIfNeeded()
                        }
                    } else {
                        val (normalizedText, needsLeadingSpace) = normalizeText(text)
                        if (needsLeadingSpace) {
                            appendSpaceIfNeeded()
                        }

                        val style = when {
                            insideOrth -> SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                                color = greekColor
                            )
                            insideTr -> SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                color = translationColor
                            )
                            insideBold -> SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = greekColor
                            )
                            insideForeign -> SpanStyle(
                                color = greekColor,
                                fontStyle = FontStyle.Italic
                            )
                            insideAuthor -> SpanStyle(
                                color = authorColor,
                                fontStyle = FontStyle.Italic
                            )
                            insideRef -> SpanStyle(
                                color = refColor
                            )
                            insideBibl && !insideAuthor -> SpanStyle(
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic
                            )
                            insideItalic -> SpanStyle(
                                fontStyle = FontStyle.Italic
                            )
                            else -> null
                        }
                        appendStyled(normalizedText, style)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (ignoredDepth > 0) {
                        ignoredDepth--
                        depth--
                        if (depth <= 0) break
                        continue
                    }

                    when (parser.name) {
                        "orth" -> {
                            insideOrth = false
                            appendRaw("  ") // space after headword
                        }
                        "b" -> insideBold = false
                        "tr" -> insideTr = false
                        "foreign" -> insideForeign = false
                        "author" -> insideAuthor = false
                        "ref" -> insideRef = false
                        "a" -> insideRef = false
                        "bibl" -> insideBibl = false
                        "hi" -> insideItalic = false
                        "div", "p", "sense" -> appendNewlineIfNeeded()
                    }
                    depth--
                    if (depth <= 0) break
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }
    }

    private fun formatSenseMarker(marker: String): String {
        val trimmed = marker.trim()
        if (trimmed.isEmpty()) {
            return ""
        }
        return if (trimmed.last() in listOf('.', ':', ')')) {
            "$trimmed "
        } else {
            "$trimmed. "
        }
    }

    private fun colorForSenseLevel(level: Int): Color {
        return when {
            level <= 1 -> senseColor
            level == 2 -> nestedSenseColor
            else -> deepSenseColor
        }
    }
}
