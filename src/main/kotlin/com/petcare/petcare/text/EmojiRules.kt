package com.petcare.petcare.text

import com.ibm.icu.lang.UCharacter
import com.ibm.icu.lang.UProperty

object EmojiRules {
    fun requireSingle(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            throw IllegalArgumentException("이모지를 입력하세요.")
        }
        if (value.length > 64 || Graphemes.count(value) != 1) {
            throw IllegalArgumentException("이모지는 하나만 입력할 수 있습니다.")
        }
        if (!isEmoji(value)) {
            throw IllegalArgumentException("이모지를 입력하세요. 글자나 기호는 사용할 수 없습니다.")
        }
        return value
    }

    private fun isEmoji(cluster: String): Boolean {
        var index = 0
        var pictograph = false
        var regional = 0
        var keycap = false
        while (index < cluster.length) {
            val codePoint = cluster.codePointAt(index)
            if (codePoint == 0x20E3) keycap = true
            if (codePoint in 0x1F1E6..0x1F1FF) regional++
            if (UCharacter.hasBinaryProperty(codePoint, UProperty.EXTENDED_PICTOGRAPHIC) ||
                UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_PRESENTATION)
            ) {
                pictograph = true
            }
            index += Character.charCount(codePoint)
        }
        if (keycap) return true
        if (regional == 2 && cluster.codePointCount(0, cluster.length) == 2) return true
        return pictograph
    }
}
