package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentOutgoingCallBinding
import com.sinch.rtc.vvc.reference.app.utils.bindings.ViewBindingFragment

class OutgoingCallFragment :
    ViewBindingFragment<FragmentOutgoingCallBinding>(R.layout.fragment_outgoing_call) {

    private val viewModel: OutgoingCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory<OutgoingCallFragmentArgs>(
            requireActivity().application,
        )
    }

    override fun setupBinding(root: View): FragmentOutgoingCallBinding =
        FragmentOutgoingCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.callButton.setOnClickListener {
            findNavController().navigate(R.id.action_outgoingCallFragment_to_establishedCallFragment)
        }
    }

}