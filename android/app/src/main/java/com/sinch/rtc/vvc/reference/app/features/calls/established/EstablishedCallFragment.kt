package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentEstablishedCallBinding
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment

class EstablishedCallFragment :
    MainActivityFragment<FragmentEstablishedCallBinding>(R.layout.fragment_established_call) {

    private val args: EstablishedCallFragmentArgs by navArgs()
    private val viewModel: EstablishedCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            mainActivityViewModel.sinchClient
        )
    }

    override fun setupBinding(root: View): FragmentEstablishedCallBinding =
        FragmentEstablishedCallBinding.bind(root)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigation(it)
        }
        viewModel.callDurationFormatted.observe(viewLifecycleOwner) {
            binding.durationText.text = it
        }
        viewModel.audioRoutingPermissionRequiredEvent.observe(viewLifecycleOwner) {
            requestPermissions(listOf(Manifest.permission.BLUETOOTH)) {
                viewModel.onAudioRoutingPermissionsResult(it)
            }
        }
        viewModel.audioCallProperties.observe(viewLifecycleOwner) {
            binding.isSpeakerOnCheckbox.isChecked = it.isSpeakerOn
            binding.isMutedCheckbox.isChecked = it.isMuted
            binding.isAutomaticRoutingEnabledCheckbox.isChecked = it.isAudioRoutingEnabled
        }

        binding.hangUpButton.setOnClickListener { viewModel.onHangUpClicked() }
        binding.isAutomaticRoutingEnabledCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onAudioRoutingCheckboxStateChanged(
                isChecked
            )
        }
        binding.isMutedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onMuteCheckboxChanged(
                isChecked
            )
        }
        binding.isSpeakerOnCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEnableSpeakerCheckboxChanged(
                isChecked
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.onBackPressed()
    }

    private fun handleNavigation(event: EstablishedCallNavigationEvent) {
        when (event) {
            Back -> findNavController().popBackStack()
        }
    }

}