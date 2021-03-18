package com.sinch.rtc.vvc.reference.app.features.calls.newcall

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentNewCallBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.domain.calls.newCallLabel
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment

class NewCallFragment : MainActivityFragment<FragmentNewCallBinding>(R.layout.fragment_new_call) {

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
    private val viewModel: NewCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            sinchClient = mainActivityViewModel.sinchClient
        )
    }

    override fun setupBinding(root: View): FragmentNewCallBinding =
        FragmentNewCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCallTypeAdapter()
        binding.callButton.setOnClickListener {
            viewModel.onCallButtonClicked()
        }
        binding.destinationInputEditText.addTextChangedListener {
            viewModel.onNewDestination(it.toString())
        }

        viewModel.callItem.observe(viewLifecycleOwner) {
            binding.callTypeSpinner.setSelection(callTypes.indexOf(it.type))
            binding.destinationInputEditText.setTextKeepState(it.destination)
            setupKeyboardType(it.type)
        }

        viewModel.isProceedEnabled.observe(viewLifecycleOwner) {
            binding.callButton.isEnabled = it
        }

        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }
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
        viewModel.onCallTypeSelected(callType)
    }

    private fun setupKeyboardType(callType: CallType) {
        binding.destinationInputEditText.inputType = when (callType) {
            CallType.AppToPhone -> InputType.TYPE_CLASS_PHONE
            else -> InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
    }

    private fun handleNavigationEvent(navigationEvent: NewCallNavigationEvent) {
        when (navigationEvent) {
            is OutgoingCall -> {
                findNavController().navigate(
                    NewCallFragmentDirections.actionNewCallFragmentToOutgoingCallFragment(
                        navigationEvent.call
                    )
                )
            }
        }
    }

}