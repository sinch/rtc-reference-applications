package com.sinch.rtc.vvc.reference.app.utils.extensions

import android.view.View
import android.view.ViewGroup

fun ViewGroup.addVideoViewChild(videoView: View) {
    if (this == videoView.parent) {
        return
    }
    (videoView.parent as? ViewGroup)?.removeView(videoView)
    addView(videoView)
}