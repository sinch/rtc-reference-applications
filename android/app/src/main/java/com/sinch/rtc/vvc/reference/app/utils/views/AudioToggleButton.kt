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
        val nextState = (audioState.ordinal + 1) % AudioState.values().size
        audioState = AudioState.values()[nextState]
        onAudioStateChanged(audioState)
    }

    private val AudioState.iconResource: Int
        get() = when (this) {
            AudioState.SPEAKER -> R.drawable.ic_baseline_speaker_phone_24
            AudioState.PHONE -> R.drawable.ic_baseline_phone_in_talk_24
            AudioState.AAR -> R.drawable.ic_baseline_aar_24
        }

}