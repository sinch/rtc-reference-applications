package com.sinch.rtc.vvc.reference.app.features.calls.newcall

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.RTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentNewCallBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.domain.calls.newCallLabel
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.MainActivityFragment

class NewCallFragment : MainActivityFragment<FragmentNewCallBinding>(R.layout.fragment_new_call) {

    companion object {
        const val TAG = "NewCallFragment"
    }

    private val args: NewCallFragmentArgs by navArgs()
    private val viewModel: NewCallViewModel by viewModels {
        RTCVoiceVideoRefAppAndroidViewModelFactory(
            requireActivity().application,
            args,
            binder = mainActivityViewModel.sinchClientServiceBinder
        )
    }

    private val chipIdsByType: MutableMap<CallType, Int> = mutableMapOf()

    override fun setupBinding(root: View): FragmentNewCallBinding =
        FragmentNewCallBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCallTypeChips()
        binding.callButton.setOnClickListener {
            viewModel.onCallButtonClicked()
        }
        binding.destinationInputEditText.addTextChangedListener {
            viewModel.onNewDestination(it.toString())
        }

        viewModel.callItem.observe(viewLifecycleOwner) {
            chipIdsByType[it.type]?.let { chipId ->
                if (binding.callTypeChipGroup.checkedChipId != chipId) {
                    binding.callTypeChipGroup.check(chipId)
                }
            }
            binding.destinationInputEditText.setTextKeepState(it.destination)
            setupKeyboardType(it.type)
        }

        viewModel.isProceedEnabled.observe(viewLifecycleOwner) {
            binding.callButton.isEnabled = it
        }

        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }

        viewModel.loggedInUserLiveData.observe(viewLifecycleOwner) {
            binding.loggedInUsernameText.text =
                String.format(getString(R.string.logged_in_template), it?.id.orEmpty())
        }
    }

    private fun setupCallTypeChips() {
        binding.callTypeChipGroup.removeAllViews()
        chipIdsByType.clear()
        viewModel.callTypes.forEach { type ->
            val chip = Chip(requireContext()).apply {
                text = type.newCallLabel(requireContext())
                isCheckable = true
                isClickable = true
                setEnsureMinTouchTargetSize(true)
                id = View.generateViewId()
            }
            binding.callTypeChipGroup.addView(chip)
            chipIdsByType[type] = chip.id
        }
        binding.callTypeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            chipIdsByType.entries.firstOrNull { it.value == checkedId }?.key?.let { type ->
                viewModel.onCallTypeSelected(type)
            }
        }
    }

    private fun setupKeyboardType(callType: CallType) {
        binding.destinationInputEditText.inputType = when (callType) {
            CallType.AppToPhone -> InputType.TYPE_CLASS_PHONE
            else -> InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
    }

    private fun handleNavigationEvent(navigationEvent: NewCallNavigationEvent) {
        when (navigationEvent) {
            is OutgoingCall -> {
                findNavController().navigate(
                    NewCallFragmentDirections.actionNewCallFragmentToOutgoingCallFragment(
                        navigationEvent.call
                    )
                )
            }
        }
    }

}
