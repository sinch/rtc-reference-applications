package com.sinch.rtc.vvc.reference.app.utils.extensions

fun StringBuilder.appendLineIfValuePresent(template: String, value: Any?) =
    value?.let {
        appendLine(String.format(template, it))
    }