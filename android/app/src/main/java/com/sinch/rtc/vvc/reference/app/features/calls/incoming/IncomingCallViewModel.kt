package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.CallProperties
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.features.calls.outgoing.OutgoingCallNavigationEvent
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class IncomingCallViewModel(
    private val sinchClient: SinchClient,
    val callId: String,
    private val app: Application,
    private val callDao: CallDao,
    private val user: User?
) : AndroidViewModel(app), CallListener {

    private val call: Call
    private var callItem: CallItem? = null

    private val isCallProgressingMutable: MutableLiveData<Boolean> = MutableLiveData(false)
    private val callPropertiesMutable: MutableLiveData<CallProperties> = MutableLiveData()

    val navigationEvents: SingleLiveEvent<IncomingCallNavigationEvent> = SingleLiveEvent()
    val callProperties: LiveData<CallProperties> = callPropertiesMutable
    val isCallProgressing: LiveData<Boolean> get() = isCallProgressingMutable

    companion object {
        const val TAG = "IncomingCallViewModel"
    }

    init {
        Log.d(TAG, "IncomingCallViewModel initialised with $callId")
        call = sinchClient.callClient.getCall(callId).apply {
            addCallListener(this@IncomingCallViewModel)
        }
        callPropertiesMutable.postValue(CallProperties(call.remoteUserId))
        isCallProgressingMutable.postValue(true)
        user?.let {
            val generatedCallItem = CallItem(call = call, user = it)
            callDao.insert(generatedCallItem)
            callItem = generatedCallItem
        }
    }

    fun onCallAccepted() {
        call.answer()
        isCallProgressingMutable.postValue(false)
        callItem?.let {
            navigationEvents.postValue(EstablishedCall(it, callId))
        }
    }

    fun onBackPressed() {
        isCallProgressingMutable.postValue(false)
        call.hangup()
    }

    override fun onCallProgressing(call: Call?) {
        Log.d(TAG, "onCallProgressing for $call")
    }

    override fun onCallEstablished(call: Call?) {
        Log.d(TAG, "onCallProgressing for $call")
    }

    override fun onCallEnded(p0: Call?) {
        navigationEvents.postValue(Back)
    }

    override fun onCleared() {
        super.onCleared()
        call.removeCallListener(this)
    }

}