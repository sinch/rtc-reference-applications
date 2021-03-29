package com.sinch.rtc.vvc.reference.app.domain.calls.properties

import android.view.View
import com.sinch.android.rtc.video.VideoController
import com.sinch.rtc.vvc.reference.app.utils.extensions.isFrontCameraUsedForCapture

data class VideoCallProperties(
    val remoteView: View,
    val localView: View,
    val isFrontCameraUsed: Boolean,
    val isLocalOnTop: Boolean,
    val isVideoPaused: Boolean,
    val isTorchOn: Boolean
) {
    constructor(
        videoController: VideoController,
        isLocalOnTop: Boolean,
        isPaused: Boolean,
        isTorchEnabled: Boolean
    ) : this(
        videoController.remoteView,
        videoController.localView,
        videoController.isFrontCameraUsedForCapture,
        isLocalOnTop = isLocalOnTop,
        isVideoPaused = isPaused,
        isTorchOn = isTorchEnabled
    )
}
