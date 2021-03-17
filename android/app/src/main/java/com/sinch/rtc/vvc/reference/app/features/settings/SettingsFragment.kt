package com.sinch.rtc.vvc.reference.app.features.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentSettingsBinding
import com.sinch.rtc.vvc.reference.app.navigation.main.MainActivity
import com.sinch.rtc.vvc.reference.app.utils.bindings.ViewBindingFragment

class SettingsFragment : ViewBindingFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels {
        NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory(requireActivity().application)
    }

    override fun setupBinding(root: View): FragmentSettingsBinding =
        FragmentSettingsBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }
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

    private fun handleNavigationEvent(event: SettingsNavigationEvent) {
        when (event) {
            Login -> {
                activity?.finish()
                findNavController().navigate(R.id.action_settingsFragment_to_loginActivity)
            }
        }
    }

}