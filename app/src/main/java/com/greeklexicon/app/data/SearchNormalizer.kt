package com.greeklexicon.app.data

import java.text.Normalizer

private fun normalizeForLookup(query: String): String {
    val decomposed = Normalizer.normalize(query.lowercase(), Normalizer.Form.NFD)
    return buildString(decomposed.length) {
        decomposed.forEach { ch ->
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                append(ch)
            }
        }
    }
}

fun normalizeGreekForLookup(query: String): String {
    return normalizeForLookup(query)
}

fun normalizeLatinForLookup(query: String): String {
    return normalizeForLookup(query)
}
