package com.sinch.rtc.vvc.reference.app.utils.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.domain.calls.AudioState

class AudioToggleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        setOnClickListener { toggleState() }
    }

    var audioState: AudioState = AudioState.PHONE
        set(newValue) {
            field = newValue
            updateImage(newValue)
        }

    var onAudioStateChanged: (AudioState) -> Unit = {}

    private fun updateImage(audioState: AudioState) {
        setImageResource(audioState.iconResource)
    }

    private fun toggleState() {
        val currentIndex = TOGGLEABLE_AUDIO_STATES.indexOf(audioState)
        val nextIndex = (currentIndex + 1) % TOGGLEABLE_AUDIO_STATES.size
        audioState = TOGGLEABLE_AUDIO_STATES[nextIndex]
        onAudioStateChanged(audioState)
    }

    private val AudioState.iconResource: Int
        get() = when (this) {
            AudioState.SPEAKER -> R.drawable.ic_baseline_speaker_phone_24
            AudioState.PHONE -> R.drawable.ic_baseline_phone_in_talk_24
            AudioState.AAR -> R.drawable.ic_baseline_aar_24
            AudioState.MANUAL -> R.drawable.ic_baseline_devices_24
        }

    companion object {
        private val TOGGLEABLE_AUDIO_STATES = listOf(AudioState.AAR, AudioState.SPEAKER, AudioState.PHONE)
    }

}