package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallListener
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class EstablishedCallViewModel(
    private val callClient: CallClient,
    private val callItem: CallItem,
    private val sinchCallId: String,
    application: Application
) :
    AndroidViewModel(application), CallListener {

    companion object {
        const val TAG = "EstablishCallViewModel"
    }

    private val callDurationMutable: MutableLiveData<Int> = MutableLiveData()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private var sinchCall: Call? = null //We have to store it as user can cancel the call first

    val callDurationFormatted: LiveData<String> = Transformations.map(callDurationMutable) {
        DateUtils.formatElapsedTime(it.toLong())
    }

    val navigationEvents: SingleLiveEvent<EstablishedCallNavigationEvent> =
        SingleLiveEvent()

    init {
        sinchCall =
            callClient.getCall(sinchCallId).apply { addCallListener(this@EstablishedCallViewModel) }
        initiateCallTimeCheck()
    }

    fun onHangUpClicked() {
        sinchCall?.hangup()
        navigationEvents.postValue(Back)
    }

    fun onBackPressed() {
        sinchCall?.hangup()
    }

    override fun onCallProgressing(call: Call?) {
        Log.d(TAG, "onCallProgressing $call")
    }

    override fun onCallEstablished(call: Call?) {
        Log.d(TAG, "onCallEstablished $call")
    }

    override fun onCallEnded(call: Call?) {
        Log.d(TAG, "onCallEnded $call")
        navigationEvents.postValue(Back)
    }

    override fun onCleared() {
        super.onCleared()
        sinchCall?.removeCallListener(this)
        mainThreadHandler.removeCallbacksAndMessages(null)
    }

    private fun initiateCallTimeCheck() {
        val delayMS = 1000L
        val checkCallTimeRunnable = object : Runnable {
            override fun run() {
                checkCallTime()
                mainThreadHandler.postDelayed(this, delayMS)
            }
        }
        mainThreadHandler.postDelayed(checkCallTimeRunnable, delayMS)
    }

    private fun checkCallTime() {
        Log.d(TAG, "Current call time is ${sinchCall?.details?.duration}")
        val durationInS = sinchCall?.details?.duration ?: return
        callDurationMutable.value = durationInS
    }

}