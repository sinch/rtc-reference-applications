package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.sinch.android.rtc.callquality.warnings.CallQualityWarningEvent
import com.sinch.android.rtc.callquality.warnings.CallQualityWarningEventType
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentEstablishedCallBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.AudioState
import com.sinch.rtc.vvc.reference.app.domain.calls.properties.VideoCallProperties
import com.sinch.rtc.vvc.reference.app.features.calls.established.screenshot.Idle
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.addVideoViewChild
import com.sinch.rtc.vvc.reference.app.utils.extensions.makeMultiline
import java.util.Timer
import java.util.TimerTask

class EstablishedCallFragment :
    MainActivityFragment<FragmentEstablishedCallBinding>(R.layout.fragment_established_call) {

    private val args: EstablishedCallFragmentArgs by navArgs()
    private val viewModel: EstablishedCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            mainActivityViewModel.sinchClientServiceBinder
        )
    }

    private var qualityWarningDialog: AlertDialog? = null
    private var timer: Timer? = null
    private var dismissDialogTask: TimerTask? = null
    private var qualityWarningQueue = mutableListOf<CallQualityWarningEvent>()

    override fun setupBinding(root: View): FragmentEstablishedCallBinding =
        FragmentEstablishedCallBinding.bind(root)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        attachBindings()
        timer = Timer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qualityWarningDialog?.dismiss()
        timer?.cancel()
        timer = null
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
            showSnackbar(getString(R.string.screenshot_saved))
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
            viewModel.requestIsPaused(isChecked)
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
        viewModel.videoPermissionsRequestEvent.observe(viewLifecycleOwner) {
            requestPermissions(listOf(Manifest.permission.CAMERA)) {
                viewModel.onVideoPermissionResult(it)
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
        viewModel.qualityWarningEvents.observe(viewLifecycleOwner) {
            handleCallQualityWarningEvent(it)
        }
    }

    private fun handleCallQualityWarningEvent(qualityWarningEvent: CallQualityWarningEvent) {
        qualityWarningQueue.add(qualityWarningEvent)
        handleNextCallQualityWarning()
    }

    private fun handleNextCallQualityWarning() {
        if (qualityWarningDialog?.isShowing == true) {
            return
        }
        val context = context ?: return
        val qualityWarningEvent = qualityWarningQueue.removeFirstOrNull() ?: return
        val isTrigger = qualityWarningEvent.type == CallQualityWarningEventType.Trigger
        qualityWarningDialog = AlertDialog.Builder(context)
            .setTitle(if (isTrigger) R.string.warning_trigger else R.string.warning_recover)
            .setMessage(getString(R.string.warning_name, qualityWarningEvent.name))
            .setIcon(if (isTrigger) R.drawable.baseline_thumb_down_24 else R.drawable.baseline_thumb_up_24)
            .show()
        qualityWarningDialog?.setOnDismissListener {
            dismissDialogTask?.cancel()
            handleNextCallQualityWarning()
        }
        dismissDialogTask = object : TimerTask() {
            override fun run() {
                qualityWarningDialog?.dismiss()
            }
        }
        timer?.schedule(dismissDialogTask, DISMISS_DIALOG_DELAY_MS)
    }

    private fun adjustVideoOnlyUI(videoCallProperties: VideoCallProperties) {
        binding.isVideoPausedToggleButton.setCheckedOmitListeners(videoCallProperties.isVideoPaused)
        binding.isTorchToggleButton.setCheckedOmitListeners(videoCallProperties.isTorchOn)
        binding.smallVideoFrameOverlay.isVisible =
            (videoCallProperties.isLocalOnTop && videoCallProperties.isVideoPaused) || (!videoCallProperties.isLocalOnTop && videoCallProperties.isRemoteVideoPaused)
        binding.bigVideoFrameOverlay.isVisible =
            (!videoCallProperties.isLocalOnTop && videoCallProperties.isVideoPaused) || (videoCallProperties.isLocalOnTop && videoCallProperties.isRemoteVideoPaused)
        if (videoCallProperties.isLocalOnTop) {
            binding.bigVideoFrame.addVideoViewChild(videoCallProperties.remoteView)
            binding.smallVideoFrame.addVideoViewChild(videoCallProperties.localView)
        } else {
            binding.bigVideoFrame.addVideoViewChild(videoCallProperties.localView)
            binding.smallVideoFrame.addVideoViewChild(videoCallProperties.remoteView)
        }
    }

    private fun showError(message: String) {
        showSnackbar(message)
    }

    private fun showModeMessage(mode: AudioState) {
        showSnackbar(mode.modeEnabledMessage)
    }

    private val AudioState.modeEnabledMessage: String
        get() =
            when (this) {
                AudioState.AAR -> getString(R.string.aar_on_msg)
                AudioState.SPEAKER -> getString(R.string.external_speaker_on_msg)
                AudioState.PHONE -> getString(R.string.phone_speaker_on)
            }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
            .apply {
                anchorView = binding.callSettingsLayout
            }
            .makeMultiline().show()
    }

    companion object {
        private const val DISMISS_DIALOG_DELAY_MS = 2000L
    }

}