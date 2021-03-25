package com.sinch.rtc.vvc.reference.app.utils.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatToggleButton

class RTCToggleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatToggleButton(context, attrs, defStyleAttr) {

    init {
        isClickable = true
        textOn = ""
        textOff = ""
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        alpha = if (checked) 1f else 0.5f
    }
}