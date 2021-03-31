package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentIncomingCallBinding
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment

class IncomingCallFragment :
    MainActivityFragment<FragmentIncomingCallBinding>(R.layout.fragment_incoming_call) {

    private val args: IncomingCallFragmentArgs by navArgs()
    private val viewModel: IncomingCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            sinchClient = mainActivityViewModel.sinchClient
        )
    }

    private val progressingCallTonePlayer: MediaPlayer by lazy {
        MediaPlayer.create(requireContext(), R.raw.progress_tone).apply { isLooping = true }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_incoming_call, container, false)

    override fun setupBinding(root: View): FragmentIncomingCallBinding =
        FragmentIncomingCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (progressingCallTonePlayer.isPlaying) {
            progressingCallTonePlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        setFullScreenMode(true)
    }

    override fun onDetach() {
        super.onDetach()
        setFullScreenMode(false)
    }

    private fun attachListeners() {
        binding.apply {
            acceptButton.setOnClickListener { viewModel.onCallAccepted() }
            declineButton.setOnClickListener { onBackPressed() }
        }
        viewModel.isCallProgressing.observe(viewLifecycleOwner) { isProgressing ->
            if (isProgressing) {
                progressingCallTonePlayer.start()
            } else if (progressingCallTonePlayer.isPlaying) {
                progressingCallTonePlayer.stop()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.callProperties.observe(viewLifecycleOwner) {
            binding.apply {
                calleNameText.text = it.calleeName
            }
        }
        viewModel.navigationEvents.observe(viewLifecycleOwner, this::handleNavigation)
    }

    private fun handleNavigation(navigationEvent: IncomingCallNavigationEvent) {
        when (navigationEvent) {
            is EstablishedCall -> findNavController().navigate(
                IncomingCallFragmentDirections.actionIncomingCallFragmentToEstablishedCallFragment(
                    navigationEvent.callItem,
                    navigationEvent.sinchCallId
                )
            )
            Back -> onBackPressed()
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
        super.onBackPressed()
    }

}