package com.sinch.rtc.vvc.reference.app.features.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentSettingsBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.label
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.navigation.main.MainActivity
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.ViewBindingFragment

class SettingsFragment : ViewBindingFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels {
        NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory(requireActivity().application)
    }

    override fun setupBinding(root: View): FragmentSettingsBinding =
        FragmentSettingsBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        setupScalingAdapters()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.apply {
            listOf(logoutButton, clearDataButton).forEach {
                it.isVisible = activity is MainActivity
            }
            logoutButton.setOnClickListener {
                viewModel.onLogoutClicked()
            }
            clearDataButton.setOnClickListener {
                viewModel.onClearDataClicked()
            }
        }
    }

    private fun setupScalingAdapters() {
        val possibleValues = VideoScalingType.values()
        val itemsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            possibleValues.map { it.label(requireContext()) }).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        listOf(
            binding.localVideoScalingSpinner,
            binding.remoteVideoScalingSpinner
        ).forEach { spinner ->
            spinner.adapter = itemsAdapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val chosenValue = possibleValues[position]
                    when (spinner) {
                        binding.localVideoScalingSpinner -> viewModel.onLocalScalingChanged(
                            chosenValue
                        )
                        binding.remoteVideoScalingSpinner -> viewModel.onRemoteScalingChanged(
                            chosenValue
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun handleNavigationEvent(event: SettingsNavigationEvent) {
        when (event) {
            Login -> {
                activity?.finish()
                findNavController().navigate(R.id.action_settingsFragment_to_loginActivity)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loggedInUser.observe(viewLifecycleOwner) { user ->
            binding.loggedInSettings.isVisible = (user != null)
            if (user != null) {
                adjustUiForLoggedInUser(user)
            }
        }

        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }
    }

    private fun adjustUiForLoggedInUser(user: User) {
        val scalingTypes = VideoScalingType.values().toList()
        binding.localVideoScalingSpinner.setSelection(scalingTypes.indexOf(user.localScalingType))
        binding.remoteVideoScalingSpinner.setSelection(scalingTypes.indexOf(user.remoteScalingType))
    }

}