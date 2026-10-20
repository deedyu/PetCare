package com.petcare.petcare.text

import com.ibm.icu.text.BreakIterator
import com.ibm.icu.util.ULocale

object Graphemes {
    fun first(value: String?): String {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return "?"
        val iterator = BreakIterator.getCharacterInstance(ULocale.ROOT)
        iterator.setText(text)
        val start = iterator.first()
        val end = iterator.next()
        if (end == BreakIterator.DONE) return "?"
        return text.substring(start, end)
    }

    fun count(value: String): Int {
        val iterator = BreakIterator.getCharacterInstance(ULocale.ROOT)
        iterator.setText(value)
        var count = 0
        iterator.first()
        while (iterator.next() != BreakIterator.DONE) {
            count++
        }
        return count
    }
}
