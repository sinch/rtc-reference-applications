package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentEstablishedCallBinding
import com.sinch.rtc.vvc.reference.app.features.calls.established.properties.VideoCallProperties
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.addVideoViewChild

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
        observeViewModel()
        attachBindings()
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

    private fun attachBindings() {
        binding.hangUpButton.setOnClickListener { viewModel.onHangUpClicked() }

        binding.isAutomaticRoutingEnabledCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onAudioRoutingCheckboxStateChanged(
                isChecked
            )
        }
        binding.isMutedToggleButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onMuteCheckboxChanged(
                isChecked
            )
        }
        binding.isTorchOnToggleButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onTorchStateChanged(isChecked)
        }
        binding.isSpeakerOnToggleButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEnableSpeakerCheckboxChanged(
                isChecked
            )
        }

        listOf(binding.smallVideoFrame, binding.bigVideoFrame).forEach {
            it.setOnLongClickListener {
                viewModel.onVideoPositionToggled()
                true
            }
        }
        listOf(binding.smallVideoFrame, binding.bigVideoFrame).forEach {
            it.setOnClickListener { viewModel.toggleFrontCamera() }
        }
        binding.isVideoPausedToggleButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsPaused(isChecked)
        }
    }

    private fun observeViewModel() {
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
            binding.isSpeakerOnToggleButton.isChecked = it.isSpeakerOn
            binding.isMutedToggleButton.isChecked = it.isMuted
            binding.isAutomaticRoutingEnabledCheckbox.isChecked = it.isAudioRoutingEnabled
        }
        viewModel.videoCallProperties.observe(viewLifecycleOwner) { videoCallProperties ->
            listOf(
                binding.smallVideoFrame,
                binding.bigVideoFrame,
                binding.isVideoPausedToggleButton,
                binding.isTorchOnToggleButton
            ).forEach {
                it.isVisible = (videoCallProperties != null)
            }
            if (videoCallProperties != null) {
                adjustVideoOnlyUI(videoCallProperties)
            }
        }
    }

    private fun adjustVideoOnlyUI(videoCallProperties: VideoCallProperties) {
        binding.isVideoPausedToggleButton.isChecked = videoCallProperties.isVideoPaused
        binding.isTorchOnToggleButton.isChecked = false
        if (videoCallProperties.isLocalOnTop) {
            binding.bigVideoFrame.addVideoViewChild(videoCallProperties.remoteView)
            binding.smallVideoFrame.addVideoViewChild(videoCallProperties.localView)
        } else {
            binding.bigVideoFrame.addVideoViewChild(videoCallProperties.localView)
            binding.smallVideoFrame.addVideoViewChild(videoCallProperties.remoteView)
        }
    }

}