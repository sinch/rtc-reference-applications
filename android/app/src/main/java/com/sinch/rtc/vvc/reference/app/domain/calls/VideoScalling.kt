package com.sinch.rtc.vvc.reference.app.domain.calls

import android.content.Context
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.R

fun VideoScalingType.label(context: Context): String {

    val resource = when (this) {
        VideoScalingType.ASPECT_FIT -> R.string.aspect_fit
        VideoScalingType.ASPECT_FILL -> R.string.aspect_fill
        VideoScalingType.ASPECT_BALANCED -> R.string.aspect_balanced
    }
    return context.getString(resource)

}