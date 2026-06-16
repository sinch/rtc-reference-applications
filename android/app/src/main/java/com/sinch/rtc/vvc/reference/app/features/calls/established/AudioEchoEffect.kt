package com.sinch.rtc.vvc.reference.app.features.calls.established

import com.sinch.android.rtc.AudioPlayoutInfo
import com.sinch.android.rtc.AudioRecordInfo
import com.sinch.android.rtc.LocalAudioFrameListener
import com.sinch.android.rtc.RemoteAudioFrameListener
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Demonstrates LocalAudioFrameListener and RemoteAudioFrameListener by applying an echo/delay
 * effect to raw PCM buffers before they are encoded (local) or played out (remote).
 *
 * A 250 ms delay line with 40% feedback — each successive echo is ~8 dB quieter than the
 * previous, producing a natural-sounding room echo.
 *
 * Register on the local path:  audioController.setLocalAudioFrameListener(AudioEchoEffect())
 * Register on the remote path: audioController.setRemoteAudioFrameListener(AudioEchoEffect())
 *
 * Each instance maintains an independent delay buffer, so local and remote paths must use
 * separate instances.
 */
class AudioEchoEffect : LocalAudioFrameListener, RemoteAudioFrameListener {

    @Volatile var isEnabled: Boolean = false

    private val delayMs = 250
    private val feedbackGain = 0.4f
    private var delayBuffer: ShortArray? = null
    private var writeIndex = 0
    private var currentDelaySamples = 0

    // LocalAudioFrameListener ------------------------------------------------------------------

    override fun onAudioFrame(record: AudioRecordInfo, bytesRead: Int, byteBuffer: ByteBuffer): Boolean {
        process(byteBuffer, record.sampleRate, record.channels, bytesRead / 2)
        return true
    }

    // RemoteAudioFrameListener -----------------------------------------------------------------

    override fun onAudioFrame(track: AudioPlayoutInfo, bytesAvailable: Int, byteBuffer: ByteBuffer): Boolean {
        process(byteBuffer, track.sampleRate, track.channels, bytesAvailable / 2)
        return true
    }

    // ------------------------------------------------------------------------------------------

    private fun ensureBuffer(sampleRate: Int, channels: Int) {
        val needed = delayMs * sampleRate / 1000 * channels
        if (currentDelaySamples != needed) {
            delayBuffer = ShortArray(needed)
            writeIndex = 0
            currentDelaySamples = needed
        }
    }

    private fun process(byteBuffer: ByteBuffer, sampleRate: Int, channels: Int, validSamples: Int) {
        if (!isEnabled) return
        ensureBuffer(sampleRate, channels)
        val buffer = delayBuffer ?: return
        val shorts = byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val bufferSize = buffer.size
        for (i in 0 until validSamples) {
            val delayed = buffer[writeIndex]
            val mixed = (shorts[i].toFloat() + delayed.toFloat() * feedbackGain)
                .coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
                .toInt().toShort()
            buffer[writeIndex] = mixed
            shorts.put(i, mixed)
            writeIndex = (writeIndex + 1) % bufferSize
        }
    }
}
