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
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.domain.calls.AudioState
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.features.calls.established.properties.AudioCallProperties
import com.sinch.rtc.vvc.reference.app.features.calls.established.properties.CallProperties
import com.sinch.rtc.vvc.reference.app.features.calls.established.properties.VideoCallProperties
import com.sinch.rtc.vvc.reference.app.utils.extensions.*
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class EstablishedCallViewModel(
    private val sinchClient: SinchClient,
    private val loggedInUser: User,
    private val sinchCallId: String,
    private val app: Application
) :
    AndroidViewModel(app), CallListener {

    companion object {
        const val TAG = "EstablishCallViewModel"
    }

    private val callDurationMutable: MutableLiveData<Int> = MutableLiveData()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private var sinchCall: Call? = null //We have to store it as user can cancel the call first
    private val callPropertiesMutable: MutableLiveData<CallProperties> = MutableLiveData()
    private val audioCallPropertiesMutable: MutableLiveData<AudioCallProperties> = MutableLiveData()
    private val videoCallPropertiesMutable: MutableLiveData<VideoCallProperties?> =
        MutableLiveData()

    private var isLocalVideoOnTop = true
    private var isVideoPaused = false
    private var isTorchOn = false
    private var currentAudioState: AudioState = AudioState.AAR
        set(value) {
            field = value
            setNewAudioState(value)
            updateAudioProperties()
        }

    val callDurationFormatted: LiveData<String> = Transformations.map(callDurationMutable) {
        DateUtils.formatElapsedTime(it.toLong())
    }

    val navigationEvents: SingleLiveEvent<EstablishedCallNavigationEvent> =
        SingleLiveEvent()
    val messageEvents: SingleLiveEvent<String> = SingleLiveEvent()

    val audioRoutingPermissionRequiredEvent = SingleLiveEvent<Unit>()
    val audioCallProperties: LiveData<AudioCallProperties> = audioCallPropertiesMutable
    val videoCallProperties: LiveData<VideoCallProperties?> = videoCallPropertiesMutable
    val callProperties: LiveData<CallProperties> = callPropertiesMutable

    init {
        sinchCall =
            sinchClient.callClient.getCall(sinchCallId)
                .apply {
                    addCallListener(this@EstablishedCallViewModel)
                }
        sinchClient.videoController.setResizeBehaviour(loggedInUser.remoteScalingType)
        sinchClient.videoController.setLocalVideoResizeBehaviour(loggedInUser.localScalingType)
        currentAudioState = AudioState.AAR
        updateAudioProperties()
        updateVideoProperties()
        updateCallProperties()
        initiateCallTimeCheck()
    }

    fun onHangUpClicked() {
        sinchCall?.hangup()
        navigationEvents.postValue(Back)
    }

    fun onBackPressed() {
        sinchCall?.hangup()
    }

    fun onAudioStateChanged(newState: AudioState) {
        Log.d(TAG, "onAudioStateChanged called $newState")
        currentAudioState = newState
    }

    fun onAudioRoutingPermissionsResult(result: PermissionRequestResult) {
        if (result.areAllPermissionsGranted) {
            sinchClient.audioController.setAutomaticRoutingEnabled(true)
        }
        updateAudioProperties()
    }

    fun onMuteCheckboxChanged(isOn: Boolean) {
        sinchClient.audioController.setMuted(isOn)
        updateAudioProperties()
    }

    fun toggleFrontCamera() {
        sinchClient.videoController.toggleCaptureDevicePosition()
        if (isTorchOn && sinchClient.videoController.isFrontCameraUsedForCapture) {
            isTorchOn = false
            updateVideoProperties()
        }
    }

    fun onTorchStateChanged(isOn: Boolean) {
        val isFrontCameraUsed = sinchClient.videoController.isFrontCameraUsedForCapture
        if (isOn && isFrontCameraUsed) {
            messageEvents.postValue(app.getString(R.string.only_rear_camera_torch))
        }
        isTorchOn = isOn && !isFrontCameraUsed
        sinchClient.videoController.setTorchMode(isTorchOn)
        updateVideoProperties()
    }

    fun setIsPaused(isPaused: Boolean) {
        this.isVideoPaused = isPaused
        if (isPaused) {
            sinchCall?.pauseVideo()
        } else {
            sinchCall?.resumeVideo()
        }
        updateVideoProperties()
    }

    fun onVideoPositionToggled() {
        this.isLocalVideoOnTop = !isLocalVideoOnTop
        sinchClient.videoController.setLocalVideoZOrder(isLocalVideoOnTop)
        updateVideoProperties()
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

    private fun setNewAudioState(audioState: AudioState) {
        sinchClient.audioController.setAutomaticRoutingEnabled(audioState == AudioState.AAR)
        if (audioState != AudioState.AAR) {
            sinchClient.audioController.setSpeakerEnabled(audioState == AudioState.SPEAKER)
        }
    }

    private fun checkCallTime() {
        val durationInS = sinchCall?.details?.duration ?: return
        callDurationMutable.value = durationInS
    }

    private fun updateCallProperties() {
        callPropertiesMutable.value = CallProperties(sinchCall?.remoteUserId.orEmpty())
    }

    private fun updateAudioProperties() {
        audioCallPropertiesMutable.postValue(AudioCallProperties(sinchClient.audioController, currentAudioState))
    }

    private fun updateVideoProperties() {
        if (sinchCall?.details?.isVideoOffered == false) {
            videoCallPropertiesMutable.postValue(null)
        } else {
            videoCallPropertiesMutable.postValue(
                VideoCallProperties(
                    sinchClient.videoController,
                    isLocalVideoOnTop,
                    isVideoPaused,
                    isTorchOn
                )
            )
        }
    }

}