package com.sinch.rtc.vvc.reference.app.features.calls.established

import com.sinch.android.rtc.video.VideoFrame

object VideoGrayscaleEffect {

    private val CHROMA_NEUTRAL = 128.toByte()

    fun applyGrayscale(frame: VideoFrame) {
        // Zero out U and V planes — Y (luma) stays, removing all colour information
        frame.yuvPlanes[1].let { u -> for (i in 0 until u.limit()) u.put(i, CHROMA_NEUTRAL) }
        frame.yuvPlanes[2].let { v -> for (i in 0 until v.limit()) v.put(i, CHROMA_NEUTRAL) }
    }
}
