package com.sinch.rtc.vvc.reference.app.utils.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatToggleButton

class RTCToggleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatToggleButton(context, attrs, defStyleAttr) {

    private var currentCheckChangedListener: CompoundButton.OnCheckedChangeListener? = null

    init {
        isClickable = true
        textOn = ""
        textOff = ""
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        alpha = if (checked) 1f else 0.5f
    }

    override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.currentCheckChangedListener = listener
        super.setOnCheckedChangeListener(listener)
    }

    fun setCheckedOmitListeners(checked: Boolean) {
        val listener = currentCheckChangedListener
        setOnCheckedChangeListener(null)
        isChecked = checked
        setOnCheckedChangeListener(listener)
    }

}