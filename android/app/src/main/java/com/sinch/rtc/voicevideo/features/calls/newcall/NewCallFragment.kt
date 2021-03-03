package com.sinch.rtc.voicevideo.features.calls.newcall

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.domain.calls.CallType
import com.sinch.rtc.voicevideo.domain.calls.newCallLabel
import com.sinch.rtc.voicevideo.features.calls.newcall.validator.AppCalleeValidator
import com.sinch.rtc.voicevideo.features.calls.newcall.validator.CalleeValidator
import com.sinch.rtc.voicevideo.features.calls.newcall.validator.PSTNCalleeValidator
import com.sinch.rtc.voicevideo.features.calls.newcall.validator.SipCalleeValidator
import kotlinx.android.synthetic.main.fragment_new_call.*

class NewCallFragment : Fragment() {

    companion object {
        const val TAG = "NewCallFragment"
    }

    private val callTypes = listOf(CallType.PSTN, CallType.Audio, CallType.Video, CallType.SIP)
    private var calleeValidator: CalleeValidator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_new_call, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCallTypeAdapter()
        callButton.setOnClickListener {
            findNavController().navigate(R.id.action_newCallFragment_to_outgoingCallFragment)
        }
        calleeInputEditText.addTextChangedListener {
            updateCallButtonState()
        }
    }

    private fun setupCallTypeAdapter() {
        val itemsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            callTypes.map { it.newCallLabel(requireContext()) }).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        callTypeSpinner.apply {
            adapter = itemsAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    onTypeChanged(callTypes[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun onTypeChanged(callType: CallType) {
        calleeValidator = when (callType) {
            CallType.SIP -> SipCalleeValidator()
            CallType.PSTN -> PSTNCalleeValidator()
            CallType.Audio, CallType.Video -> AppCalleeValidator()
        }
        calleeInputEditText.inputType = when (callType) {
            CallType.SIP, CallType.Audio, CallType.Video -> InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            CallType.PSTN -> InputType.TYPE_CLASS_PHONE
        }
        updateCallButtonState()
    }

    private fun updateCallButtonState() {
        callButton.isEnabled =
            calleeValidator?.isCalleeValid(calleeInputEditText.text.toString()) ?: true
    }

}