package com.sinch.rtc.vvc.reference.app.utils.mvvm

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Mutable live data implementation that notifies observers only if each posted event has not yet been handled.
 */
class ConsumableEventsLiveData<T> : MutableLiveData<ConsumableEvent<T>>() {

    private val pendingEvents = ConcurrentLinkedQueue<ConsumableEvent<T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in ConsumableEvent<T>>) {
        super.observe(owner) { event ->
            event?.getContentIfNotHandled()?.let { value ->
                observer.onChanged(ConsumableEvent(value))
            }
        }
    }

    fun observeData(owner: LifecycleOwner, observer: Observer<in T>) {
        observe(owner) { event ->
            event.getContentIfNotHandled()?.let { value ->
                observer.onChanged(value)
            }
        }
    }

    /**
     * Emits event with given value.
     */
    fun postData(value: T) {
        val event = ConsumableEvent(value)
        pendingEvents.add(event)
        postValue(event)
    }

}