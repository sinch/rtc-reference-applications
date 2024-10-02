package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.android.rtc.calling.CallState
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentOutgoingCallBinding
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.UriHelper

class OutgoingCallFragment :
    MainActivityFragment<FragmentOutgoingCallBinding>(R.layout.fragment_outgoing_call) {

    companion object {
        const val TAG = "OutgoingCallFragment"
    }

    private val args: OutgoingCallFragmentArgs by navArgs()
    private val viewModel: OutgoingCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            mainActivityViewModel.sinchClientServiceBinder
        )
    }

    private val progressingCallTonePlayer: MediaPlayer by lazy {
        MediaPlayer().apply {
            setDataSource(requireContext(), UriHelper.uriForResource(requireContext(), R.raw.progress_out))
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
        }
    }

    override fun setupBinding(root: View): FragmentOutgoingCallBinding =
        FragmentOutgoingCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cancelButton.setOnClickListener {
            viewModel.onCancelButtonPressed()
        }
        binding.stateTextView.startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.blink)
        )
        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }
        viewModel.permissionsRequiredEvent.observe(viewLifecycleOwner) {
            handlePermissionsRequired(it)
        }
        viewModel.callState.observe(viewLifecycleOwner) { callState ->
            if (callState == CallState.RINGING) {
                progressingCallTonePlayer.start()
            } else if (progressingCallTonePlayer.isPlaying) {
                progressingCallTonePlayer.stop()
            }
            val stateText = when (callState) {
                CallState.INITIATING, CallState.PROGRESSING -> R.string.initiating
                CallState.RINGING -> R.string.calling
                CallState.ANSWERED -> R.string.connecting
                else -> null
            }
            if (stateText != null) {
                binding.stateTextView.setText(stateText)
            }
        }
        viewModel.callItemLiveData.observe(viewLifecycleOwner) {
            binding.calleNameText.text = it.destination
        }
        viewModel.onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        setFullScreenMode(true)
    }

    override fun onDetach() {
        super.onDetach()
        setFullScreenMode(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (progressingCallTonePlayer.isPlaying) {
            progressingCallTonePlayer.pause()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.onBackPressed()
    }

    private fun handleNavigationEvent(outgoingCallNavigationEvent: OutgoingCallNavigationEvent) {
        when (outgoingCallNavigationEvent) {
            is EstablishedCall -> {
                findNavController().navigate(
                    OutgoingCallFragmentDirections.actionOutgoingCallFragmentToEstablishedCallFragment(
                        outgoingCallNavigationEvent.callItem,
                        outgoingCallNavigationEvent.sinchCallId
                    )
                )
            }

            Back -> findNavController().popBackStack()
        }
    }

    private fun handlePermissionsRequired(permissions: List<String>) {
        requestPermissions(permissions) {
            viewModel.onPermissionsResult(it)
        }
    }

}