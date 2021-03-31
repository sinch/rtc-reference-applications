package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentOutgoingCallBinding
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment

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
            mainActivityViewModel.sinchClient
        )
    }

    private val progressingCallTonePlayer: MediaPlayer by lazy {
        MediaPlayer.create(requireContext(), R.raw.progress_tone).apply { isLooping = true }
    }

    override fun setupBinding(root: View): FragmentOutgoingCallBinding =
        FragmentOutgoingCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cancelButton.setOnClickListener {
            viewModel.onCancelButtonPressed()
        }
        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }
        viewModel.permissionsRequiredEvent.observe(viewLifecycleOwner) {
            handlePermissionsRequired(it)
        }
        viewModel.isCallProgressing.observe(viewLifecycleOwner) { isProgressing ->
            if (isProgressing) {
                progressingCallTonePlayer.start()
            } else if (progressingCallTonePlayer.isPlaying) {
                progressingCallTonePlayer.stop()
            }
        }
        viewModel.onViewCreated()
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