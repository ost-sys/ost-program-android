package com.ost.application.ui.core.util

import java.util.StringTokenizer

fun String.containsTokenOf(query: String): Boolean{
    val tokenizer = StringTokenizer(query)
    while (tokenizer.hasMoreTokens()) {
        if (!this.contains(tokenizer.nextToken(),true)) {
            return false
        }
    }
    return true
}

