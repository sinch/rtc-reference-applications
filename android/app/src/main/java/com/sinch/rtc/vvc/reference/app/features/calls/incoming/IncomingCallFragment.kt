package com.sinch.rtc.vvc.reference.app.features.calls.incoming

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sinch.android.rtc.calling.CallState
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentIncomingCallBinding
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.UriHelper
import com.sinch.rtc.vvc.reference.app.utils.extensions.safeStop


class IncomingCallFragment :
    MainActivityFragment<FragmentIncomingCallBinding>(R.layout.fragment_incoming_call) {

    companion object {
        const val TAG = "IncomingCallFragment"
    }

    private val args: IncomingCallFragmentArgs by navArgs()
    private val viewModel: IncomingCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            binder = mainActivityViewModel.sinchClientServiceBinder
        )
    }

    private val progressingCallTonePlayer: MediaPlayer by lazy {
        MediaPlayer().apply {
            setDataSource(requireContext(), UriHelper.uriForResource(requireContext(), R.raw.progress_tone))
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
        binding.stateTextView.startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.blink)
        )
        attachListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressingCallTonePlayer.safeStop()
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
        viewModel.onBackPressed()
        super.onBackPressed()
    }

    private fun attachListeners() {
        binding.apply {
            acceptButton.setOnClickListener { viewModel.onCallAccepted() }
            declineButton.setOnClickListener { onBackPressed() }
        }
        viewModel.callState.observe(viewLifecycleOwner) { callState ->
            Log.d(TAG, "Call state $callState")
            val wasAnsweredAlready =
                listOf(CallState.ANSWERED, CallState.ESTABLISHED, CallState.ENDED).contains(callState)
            if (!wasAnsweredAlready) {
                progressingCallTonePlayer.start()
            } else {
                progressingCallTonePlayer.safeStop()
            }
            binding.acceptButton.isEnabled = !wasAnsweredAlready
            val stateText = when (callState) {
                CallState.INITIATING, CallState.RINGING, CallState.PROGRESSING -> R.string.calling
                CallState.ANSWERED -> R.string.connecting
                else -> null
            }
            if (stateText != null) {
                binding.stateTextView.setText(stateText)
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
        viewModel.permissionsEvents.observe(viewLifecycleOwner, this::handlePermissionsRequired)
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

    private fun handlePermissionsRequired(permissions: List<String>) {
        requestPermissions(permissions) {
            viewModel.onPermissionsResult(it)
        }
    }

}