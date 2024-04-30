package com.assoft.peekster.util

import java.util.*

object TextUtils {
    fun getLetters(text: String?, length: Int): String? {
        var txt = text
        var len = length
        if (txt == null || txt.isEmpty()) txt = "?"
        val breakAfter = --len
        val stringBuilder = StringBuilder()
        for (letter in txt.split(" ").toTypedArray()) {
            if (stringBuilder.length > breakAfter) break
            if (letter.isEmpty()) continue
            stringBuilder.append(letter[0])
        }
        return stringBuilder.toString().toUpperCase(Locale.getDefault())
    }

    fun searchWord(word: String, searchThis: String?): Boolean {
        return searchThis == null || searchThis.isEmpty() || word.toLowerCase(Locale.getDefault())
            .contains(searchThis.toLowerCase(Locale.getDefault()))
    }

    fun trimText(text: String?, length: Int): String? {
        return if (text == null || text.length <= length) text else text.substring(0, length)
    }
}