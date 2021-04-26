package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentEstablishedCallBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.AudioState
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.VideoCallProperties
import com.sinch.rtc.vvc.reference.app.features.calls.established.screenshot.Idle
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.addVideoViewChild
import com.sinch.rtc.vvc.reference.app.utils.extensions.makeMultiline

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

    override fun onResume() {
        super.onResume()
        setFullScreenMode(true)
    }

    override fun onDetach() {
        super.onDetach()
        setFullScreenMode(false)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.onBackPressed()
    }

    private fun handleNavigation(event: EstablishedCallNavigationEvent) {
        when (event) {
            Back -> onBackPressed()
        }
    }

    private fun attachBindings() {
        binding.hangUpButton.setOnClickListener { onBackPressed() }

        binding.audioStateButton.onAudioStateChanged = { newState ->
            viewModel.onAudioStateChanged(newState)
            showModeMessage(newState)
        }

        binding.isMutedToggleButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onMuteCheckboxChanged(
                isChecked
            )
        }
        binding.isTorchToggleButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onTorchStateChanged(isChecked)
        }
        binding.screenshotButton.setOnClickListener {
            viewModel.onScreenshotButtonClicked()
            Snackbar.make(requireView(), getString(R.string.screenshot_saved), Snackbar.LENGTH_SHORT)
                .makeMultiline().show()
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
        viewModel.messageEvents.observe(viewLifecycleOwner, this::showError)
        viewModel.navigationEvents.observe(viewLifecycleOwner, this::handleNavigation)
        viewModel.callProperties.observe(viewLifecycleOwner) {
            binding.calleNameText.text = it.calleeName
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
            binding.audioStateButton.audioState = it.audioState
            binding.isMutedToggleButton.setCheckedOmitListeners(it.isMuted)
        }
        viewModel.captureState.observe(viewLifecycleOwner) {
            binding.screenshotButton.isEnabled = it == Idle
        }

        viewModel.videoCallProperties.observe(viewLifecycleOwner) { videoCallProperties ->
            listOf(
                binding.smallVideoFrame,
                binding.bigVideoFrame,
                binding.isVideoPausedToggleButton,
                binding.isTorchToggleButton,
                binding.screenshotButton
            ).forEach {
                it.isVisible = (videoCallProperties != null)
            }
            if (videoCallProperties != null) {
                adjustVideoOnlyUI(videoCallProperties)
            }
        }
    }

    private fun adjustVideoOnlyUI(videoCallProperties: VideoCallProperties) {
        binding.isVideoPausedToggleButton.setCheckedOmitListeners(videoCallProperties.isVideoPaused)
        binding.isTorchToggleButton.setCheckedOmitListeners(videoCallProperties.isTorchOn)
        if (videoCallProperties.isLocalOnTop) {
            binding.bigVideoFrame.addVideoViewChild(videoCallProperties.remoteView)
            binding.smallVideoFrame.addVideoViewChild(videoCallProperties.localView)
        } else {
            binding.bigVideoFrame.addVideoViewChild(videoCallProperties.localView)
            binding.smallVideoFrame.addVideoViewChild(videoCallProperties.remoteView)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
            .makeMultiline().show()
    }

    private fun showModeMessage(mode: AudioState) {
        Snackbar.make(requireView(), mode.modeEnabledMessage, Snackbar.LENGTH_SHORT)
            .makeMultiline().show()
    }

    private val AudioState.modeEnabledMessage: String get() =
        when (this) {
            AudioState.AAR -> getString(R.string.aar_on_msg)
            AudioState.SPEAKER -> getString(R.string.external_speaker_on_msg)
            AudioState.PHONE -> getString(R.string.phone_speaker_on)
        }

}