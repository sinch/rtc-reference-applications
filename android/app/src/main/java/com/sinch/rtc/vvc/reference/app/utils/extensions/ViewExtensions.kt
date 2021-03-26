package com.sinch.rtc.vvc.reference.app.utils.extensions

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

fun ViewGroup.addVideoViewChild(videoView: View) {
    if (this == videoView.parent) {
        return
    }
    (videoView.parent as? ViewGroup)?.removeView(videoView)
    addView(videoView)
}

fun Snackbar.makeMultiline(): Snackbar = apply {
    apply {
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines =
            10
    }
}