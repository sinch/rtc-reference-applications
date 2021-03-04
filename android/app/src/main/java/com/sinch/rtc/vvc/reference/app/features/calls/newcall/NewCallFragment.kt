package com.sinch.rtc.vvc.reference.app.features.calls.newcall

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.databinding.FragmentNewCallBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.domain.calls.newCallLabel
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.AppDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.DestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.PSTNDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.SipDestinationValidator
import com.sinch.rtc.vvc.reference.app.utils.bindings.ViewBindingFragment

class NewCallFragment : ViewBindingFragment<FragmentNewCallBinding>(R.layout.fragment_new_call) {

    companion object {
        const val TAG = "NewCallFragment"
    }

    private val callTypes = listOf(
        CallType.AppToPhone,
        CallType.AppToAppAudio,
        CallType.AppToAppVideo,
        CallType.AppToSip
    )
    private val args: NewCallFragmentArgs by navArgs()

    private var destinationValidator: DestinationValidator? = null

    override fun setupBinding(root: View): FragmentNewCallBinding =
        FragmentNewCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCallTypeAdapter()
        binding.callButton.setOnClickListener {
            findNavController().navigate(R.id.action_newCallFragment_to_outgoingCallFragment)
        }
        binding.destinationInputEditText.addTextChangedListener {
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
        binding.callTypeSpinner.apply {
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
        binding.destinationInputEditText.inputType = when (callType) {
            CallType.AppToSip, CallType.AppToAppAudio, CallType.AppToAppVideo -> InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            CallType.AppToPhone -> InputType.TYPE_CLASS_PHONE
        }
        updateCallButtonState()
    }

    private fun updateCallButtonState() {
        binding.callButton.isEnabled =
            destinationValidator?.isCalleeValid(binding.destinationInputEditText.text.toString())
                ?: true
    }

    private fun populateWithInitialData() {
        args.initialCallItem?.type?.let {
            binding.callTypeSpinner.setSelection(callTypes.indexOf(it))
        }
        binding.destinationInputEditText.setText(args.initialCallItem?.destination.orEmpty())
    }

}