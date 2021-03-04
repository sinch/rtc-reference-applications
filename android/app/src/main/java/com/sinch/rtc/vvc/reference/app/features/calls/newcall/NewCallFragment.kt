package com.sinch.rtc.vvc.reference.app.features.calls.newcall

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
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.domain.calls.newCallLabel
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.AppDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.DestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.PSTNDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.SipDestinationValidator
import kotlinx.android.synthetic.main.fragment_new_call.*

class NewCallFragment : Fragment() {

    companion object {
        const val TAG = "NewCallFragment"
    }

    private val callTypes = listOf(CallType.AppToPhone, CallType.AppToAppAudio, CallType.AppToAppVideo, CallType.AppToSip)
    private val args: NewCallFragmentArgs by navArgs()

    private var destinationValidator: DestinationValidator? = null

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
        destinationInputEditText.addTextChangedListener {
            updateCallButtonState()
        }
        populateWithInitialData()
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
        destinationValidator = when (callType) {
            CallType.AppToSip -> SipDestinationValidator()
            CallType.AppToPhone -> PSTNDestinationValidator()
            CallType.AppToAppAudio, CallType.AppToAppVideo -> AppDestinationValidator()
        }
        destinationInputEditText.inputType = when (callType) {
            CallType.AppToSip, CallType.AppToAppAudio, CallType.AppToAppVideo -> InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            CallType.AppToPhone -> InputType.TYPE_CLASS_PHONE
        }
        updateCallButtonState()
    }

    private fun updateCallButtonState() {
        callButton.isEnabled =
            destinationValidator?.isCalleeValid(destinationInputEditText.text.toString()) ?: true
    }

    private fun populateWithInitialData() {
        args.initialCallItem?.type?.let {
            callTypeSpinner.setSelection(callTypes.indexOf(it))
        }
        destinationInputEditText.setText(args.initialCallItem?.destination.orEmpty())
    }

}