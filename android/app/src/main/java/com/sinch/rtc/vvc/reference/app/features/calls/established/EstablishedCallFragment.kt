package com.sinch.rtc.vvc.reference.app.features.calls.established

import android.Manifest
import android.media.AudioDeviceInfo
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.sinch.rtc.vvc.reference.app.utils.extensions.friendlyName
import com.sinch.rtc.vvc.reference.app.utils.extensions.makeMultiline

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

    private var qualityWarningSnackbar: Snackbar? = null
    private var qualityWarningQueue = mutableListOf<CallQualityWarningEvent>()

    override fun setupBinding(root: View): FragmentEstablishedCallBinding =
        FragmentEstablishedCallBinding.bind(root)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        attachBindings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qualityWarningSnackbar?.dismiss()
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
        binding.communicationDeviceButton.setOnClickListener {
            viewModel.onCommunicationDeviceButtonClicked()
        }
    }

    private fun observeViewModel() {
        viewModel.messageEvents.observe(viewLifecycleOwner, this::showError)
        viewModel.navigationEvents.observe(viewLifecycleOwner, this::handleNavigation)
        viewModel.callProperties.observe(viewLifecycleOwner) {
            binding.calleNameText.text = it.calleeName
            binding.audioCalleNameText.text = it.calleeName
        }
        viewModel.callDurationFormatted.observe(viewLifecycleOwner) {
            binding.durationText.text = it
            binding.audioDurationText.text = it
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
                binding.videoControl,
                binding.torchControl,
                binding.screenshotControl,
                binding.calleeChip
            ).forEach {
                it.isVisible = (videoCallProperties != null)
            }
            binding.audioCallInfoLayout.isVisible = (videoCallProperties == null)
            if (videoCallProperties != null) {
                adjustVideoOnlyUI(videoCallProperties)
            }
        }
        viewModel.qualityWarningEvents.observe(viewLifecycleOwner) {
            handleCallQualityWarningEvent(it)
        }
        viewModel.communicationDevicePickerEvent.observe(viewLifecycleOwner) {
            showCommunicationDevicePicker(it)
        }
    }

    private fun handleCallQualityWarningEvent(qualityWarningEvent: CallQualityWarningEvent) {
        qualityWarningQueue.add(qualityWarningEvent)
        handleNextCallQualityWarning()
    }

    private fun handleNextCallQualityWarning() {
        if (qualityWarningSnackbar?.isShown == true) {
            return
        }
        val context = context ?: return
        val qualityWarningEvent = qualityWarningQueue.removeFirstOrNull() ?: return
        val isTrigger = qualityWarningEvent.type == CallQualityWarningEventType.Trigger
        val icon = if (isTrigger) R.drawable.baseline_thumb_down_24 else R.drawable.baseline_thumb_up_24
        val title = getString(if (isTrigger) R.string.warning_trigger else R.string.warning_recover)
        val message = getString(R.string.warning_name, qualityWarningEvent.name)

        qualityWarningSnackbar = Snackbar.make(requireView(), "$title\n$message", DISMISS_DELAY_MS.toInt())
            .apply {
                anchorView = binding.callSettingsLayout
                setBackgroundTint(ContextCompat.getColor(context,
                    if (isTrigger) R.color.decline else R.color.accept))
                setTextColor(ContextCompat.getColor(context, R.color.onCallSurface))
                val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView.maxLines = 3
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
                textView.compoundDrawablePadding = context.resources.getDimensionPixelSize(R.dimen.space_s)
                textView.compoundDrawableTintList = ContextCompat.getColorStateList(context, R.color.onCallSurface)
                duration = DISMISS_DELAY_MS.toInt()
                addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        handleNextCallQualityWarning()
                    }
                })
            }
        qualityWarningSnackbar?.show()
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
                AudioState.MANUAL -> getString(R.string.manual_device_on_msg)
            }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
            .apply {
                anchorView = binding.callSettingsLayout
            }
            .makeMultiline().show()
    }

    private fun showCommunicationDevicePicker(devices: List<AudioDeviceInfo>) {
        val context = context ?: return
        val deviceNames = devices.map { it.friendlyName(context) }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.select_communication_device)
            .setItems(deviceNames) { _, which ->
                viewModel.onCommunicationDeviceSelected(devices[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val DISMISS_DELAY_MS = 2000L
    }

}