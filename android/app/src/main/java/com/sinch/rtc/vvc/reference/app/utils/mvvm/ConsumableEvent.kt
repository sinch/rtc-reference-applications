package com.sinch.rtc.vvc.reference.app.utils.mvvm

/**
 * This is a wrapper around event that can be handled only once (e.g. when showing a notification or summary screen)
 */
class ConsumableEvent<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * @return the content if it has not been handled yet, otherwise null.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}