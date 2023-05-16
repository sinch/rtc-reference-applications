package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.Manifest
import android.app.*
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.video.RemoteVideoFrameListener
import com.sinch.android.rtc.video.VideoFrame
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.domain.calls.AudioState
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.AudioCallProperties
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.CallProperties
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.VideoCallProperties
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.features.calls.established.screenshot.*
import com.sinch.rtc.vvc.reference.app.utils.extensions.*
import com.sinch.rtc.vvc.reference.app.utils.jwt.getString
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class EstablishedCallViewModel(
    private val sinchClient: SinchClient,
    private val loggedInUser: User,
    private val sinchCallId: String,
    private val callDao: CallDao,
    private val callItem: CallItem,
    private val app: Application
) :
    AndroidViewModel(app), CallListener, RemoteVideoFrameListener {

    companion object {
        const val TAG = "EstablishCallViewModel"
        const val SCREENSHOT_SUFIX = "screenshot"
        const val REMOTE_VIDEO_FRAME_MAX_INTERVAL_MS = 1500L
    }

    private val callDurationMutable: MutableLiveData<Int> = MutableLiveData()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private var sinchCall: Call? = null //We have to store it as user can cancel the call first
    private val callPropertiesMutable: MutableLiveData<CallProperties> = MutableLiveData()
    private val audioCallPropertiesMutable: MutableLiveData<AudioCallProperties> = MutableLiveData()
    private val videoCallPropertiesMutable: MutableLiveData<VideoCallProperties?> =
        MutableLiveData()
    private val frameCaptureStateMutable: MutableLiveData<FrameCaptureState> = MutableLiveData(Idle)

    private var isLocalVideoOnTop = true
    private var isRemoteVideoPaused = false
    private var isVideoPaused = false
    private var isTorchOn = false
    private var currentAudioState: AudioState = AudioState.AAR
        set(value) {
            field = value
            setNewAudioState(value)
            updateAudioProperties()
        }
    private var lastVideoFrameTimestamp: Long? = null

    val callDurationFormatted: LiveData<String> = callDurationMutable.map {
        DateUtils.formatElapsedTime(it.toLong())
    }

    val navigationEvents: SingleLiveEvent<EstablishedCallNavigationEvent> =
        SingleLiveEvent()
    val messageEvents: SingleLiveEvent<String> = SingleLiveEvent()

    val videoPermissionsRequestEvent = SingleLiveEvent<Unit>()
    val audioCallProperties: LiveData<AudioCallProperties> = audioCallPropertiesMutable
    val videoCallProperties: LiveData<VideoCallProperties?> = videoCallPropertiesMutable
    val callProperties: LiveData<CallProperties> = callPropertiesMutable
    val captureState: LiveData<FrameCaptureState> = frameCaptureStateMutable

    init {
        sinchCall =
            sinchClient.callController.getCall(sinchCallId)
                ?.apply {
                    addCallListener(this@EstablishedCallViewModel)
                }
        sinchCall?.takeIf { it.details.isVideoOffered }?.let {
            sinchClient.videoController.setResizeBehaviour(loggedInUser.remoteScalingType)
            sinchClient.videoController.setLocalVideoResizeBehaviour(loggedInUser.localScalingType)
            sinchClient.videoController.setRemoteVideoFrameListener(this)
            setIsPaused(
                ContextCompat.checkSelfPermission(
                    app,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            )
        }
        currentAudioState = AudioState.AAR
        sinchClient.audioController.setMuted(false)
        updateAudioProperties()
        updateVideoProperties()
        updateCallProperties()
        initiateCallTimer()
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

    fun onVideoPermissionResult(result: PermissionRequestResult) {
        setIsPaused(!result.areAllPermissionsGranted)
        if (result.areAllPermissionsGranted) {
            messageEvents.postValue(getString(R.string.video_resumed))
        } else {
            messageEvents.postValue(getString(R.string.video_permissions_explanation))
        }
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

    fun onScreenshotButtonClicked() {
        Log.d(TAG, "Screenshot button clicked")
        frameCaptureStateMutable.value = Triggered
    }

    fun requestIsPaused(isPaused: Boolean) {
        if (!isPaused) {
            videoPermissionsRequestEvent.call()
        } else {
            messageEvents.postValue(getString(R.string.video_pause))
            setIsPaused(true)
        }
    }

    fun onVideoPositionToggled() {
        this.isLocalVideoOnTop = !isLocalVideoOnTop
        sinchClient.videoController.setLocalVideoZOrder(isLocalVideoOnTop)
        updateVideoProperties()
    }

    override fun onCallProgressing(call: Call) {
        Log.d(TAG, "onCallProgressing $call")
    }

    override fun onCallEstablished(call: Call) {
        callItem.updateBasedOnSinchCall(call, callDao)
        Log.d(TAG, "onCallEstablished $call")
    }

    override fun onCallEnded(call: Call) {
        Log.d(TAG, "onCallEnded $call")
        callItem.updateBasedOnSinchCall(call, callDao)
        navigationEvents.postValue(Back)
    }

    override fun onCleared() {
        super.onCleared()
        sinchCall?.hangup()
        sinchCall?.removeCallListener(this)
        sinchCall?.takeIf { it.details.isVideoOffered }?.let {
            sinchClient.videoController.setRemoteVideoFrameListener(null)
        }
        mainThreadHandler.removeCallbacksAndMessages(null)
    }

    private fun setIsPaused(isPaused: Boolean) {
        this.isVideoPaused = isPaused
        if (isPaused) {
            sinchCall?.pauseVideo()
        } else {
            sinchCall?.resumeVideo()
        }
        updateVideoProperties()
    }

    private fun initiateCallTimer() {
        val delayMS = 1000L
        val checkCallTimeRunnable = object : Runnable {
            override fun run() {
                checkCallTime()
                if(sinchCall?.details?.isVideoOffered == true) {
                    checkLastFrameTimestamp()
                }
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

    private fun checkLastFrameTimestamp() {
        if (lastVideoFrameTimestamp == null) {
            lastVideoFrameTimestamp = Date().time
        }
        val currentTimestamp = Date().time
        val previousTimestamp = lastVideoFrameTimestamp
        if (
            !isRemoteVideoPaused &&
            previousTimestamp != null &&
            (currentTimestamp - previousTimestamp) > REMOTE_VIDEO_FRAME_MAX_INTERVAL_MS
        ) {
            isRemoteVideoPaused = true
            messageEvents.postValue(getString(R.string.remote_paused))
            updateVideoProperties()
        }
    }

    private fun updateCallProperties() {
        callPropertiesMutable.value = CallProperties(sinchCall?.remoteUserId.orEmpty())
        callItem.updateBasedOnSinchCall(sinchCall, callDao)
    }

    private fun updateAudioProperties() {
        audioCallPropertiesMutable.postValue(
            AudioCallProperties(
                sinchClient.audioController,
                currentAudioState
            )
        )
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
                    isRemoteVideoPaused,
                    isTorchOn
                )
            )
        }
    }

    override fun onFrame(callId: String, frame: VideoFrame) {
        lastVideoFrameTimestamp = Date().time
        GlobalScope.launch(Dispatchers.Main) {
            if (captureState.value == Triggered) {
                frameCaptureStateMutable.value = Capturing
                launchScopedFrameCapture(callId, frame)
            }
            if (isRemoteVideoPaused) {
                messageEvents.postValue(getString(R.string.remote_video_resumed))
                isRemoteVideoPaused = false
                updateVideoProperties()
            }
        }
    }

    private fun launchScopedFrameCapture(callId: String, videoFrame: VideoFrame) {
        viewModelScope.launch {
            val result = ScreenshotCoroutineSaver(
                app,
                callId.plus(SCREENSHOT_SUFIX),
                videoFrame
            ).saveAsync().await()
            if (result is Error) {
                messageEvents.postValue(result.error.localizedMessage)
            }
            frameCaptureStateMutable.postValue(Idle)
        }
    }

}