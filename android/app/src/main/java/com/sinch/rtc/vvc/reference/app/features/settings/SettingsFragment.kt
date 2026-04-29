package com.sinch.rtc.vvc.reference.app.features.settings

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sinch.android.rtc.Sinch
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.BuildConfig
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentSettingsBinding
import com.sinch.rtc.vvc.reference.app.domain.AppConfig
import com.sinch.rtc.vvc.reference.app.domain.calls.label
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.ViewBindingFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.defaultConfigs

class SettingsFragment : ViewBindingFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels {
        NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory(requireActivity().application)
    }

    private val configSpinnerValues by lazy {
        requireContext().defaultConfigs.map { it.name } + AppConfig.CUSTOM_CONFIG_NAME
    }

    private val scalingLabels by lazy {
        VideoScalingType.values().map { it.label(requireContext()) }
    }

    override fun setupBinding(root: View): FragmentSettingsBinding =
        FragmentSettingsBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        setupLabels()
        setupScalingAdapters()
        setupConfigAdapters()
        observeViewModel()
        if (!BuildConfig.SHOW_DEBUG_INFO) {
            binding.environmentCard.isVisible = false
            binding.updateDevSettingsButton.isVisible = false
        }
    }

    private fun setupLabels() {
        binding.versionText.text = getString(R.string.version_text_template, Sinch.version)
    }

    private fun setupButtons() {
        binding.apply {
            logoutButton.setOnClickListener {
                showLogoutConfirmation()
            }
            clearDataButton.setOnClickListener {
                viewModel.onClearDataClicked()
            }
            updateDevSettingsButton.setOnClickListener {
                showConfirmationDialog()
            }
        }
    }

    private fun setupConfigAdapters() {
        val itemsAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_item,
            configSpinnerValues
        )
        binding.configsDropdown.setAdapter(itemsAdapter)
        binding.configsDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.onEnvSpinnerItemChanged(
                configSpinnerValues[position],
                binding.appKeyInputEditText.text.toString(),
                binding.appSecretInputEditText.text.toString(),
                binding.environmentInputEditText.text.toString(),
                binding.cliInputEditText.text.toString()
            )
        }
    }

    private fun setupScalingAdapters() {
        val possibleValues = VideoScalingType.values()
        listOf(
            binding.localVideoScalingDropdown to { value: VideoScalingType ->
                viewModel.onLocalScalingChanged(value)
            },
            binding.remoteVideoScalingDropdown to { value: VideoScalingType ->
                viewModel.onRemoteScalingChanged(value)
            }
        ).forEach { (dropdown, onSelected) ->
            dropdown.setAdapter(
                ArrayAdapter(requireContext(), R.layout.dropdown_menu_item, scalingLabels)
            )
            dropdown.setOnItemClickListener { _, _, position, _ ->
                onSelected(possibleValues[position])
            }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout) { _, _ -> viewModel.onLogoutClicked() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.update_logout)
            .setMessage(R.string.update_message).setPositiveButton(R.string.yes) { _, _ ->
                viewModel.onUpdateDevSettingsClicked(
                    binding.appKeyInputEditText.text.toString(),
                    binding.appSecretInputEditText.text.toString(),
                    binding.environmentInputEditText.text.toString(),
                    binding.cliInputEditText.text.toString()
                )
            }.setNegativeButton(R.string.no, null).show()
    }

    private fun handleNavigationEvent(event: SettingsNavigationEvent) {
        when (event) {
            Login -> {
                activity?.finish()
                findNavController().navigate(SettingsFragmentDirections.toLoggedOutFlow())
            }
        }
    }

    private fun observeViewModel() {
        viewModel.loggedInUser.observe(viewLifecycleOwner) { user ->
            val isAnyUserLoggedIn = (user != null)
            listOf(
                binding.loggedInSettings,
                binding.clearDataButton,
                binding.logoutButton
            ).forEach {
                it.isVisible = isAnyUserLoggedIn
            }
            listOf(
                binding.configsInputLayout,
                binding.configsDropdown,
                binding.appKeyInputLayout,
                binding.appSecretInputLayout,
                binding.environmentInputLayout,
                binding.cliInputLayout
            ).forEach {
                it.isEnabled = !isAnyUserLoggedIn
            }
            binding.updateDevSettingsButton.isVisible = !isAnyUserLoggedIn && BuildConfig.SHOW_DEBUG_INFO
            if (user != null) {
                adjustUiForLoggedInUser(user)
            }
        }

        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }

        viewModel.devDataLiveData.observe(viewLifecycleOwner) {
            listOf(
                binding.appKeyInputEditText,
                binding.appSecretInputEditText,
                binding.environmentInputEditText,
                binding.cliInputEditText
            ).forEach { textInput ->
                textInput.isEnabled = it.isCustom
            }
            binding.configsDropdown.setText(it.name, false)
            binding.appKeyInputEditText.setText(it.appKey)
            binding.appSecretInputEditText.setText(it.appSecret)
            binding.environmentInputEditText.setText(it.environment)
            binding.cliInputEditText.setText(it.cli)
        }
    }

    private fun adjustUiForLoggedInUser(user: User) {
        val scalingTypes = VideoScalingType.values().toList()
        binding.apply {
            loggedInUsernameText.text =
                String.format(getString(R.string.logged_in_template), user.id)
            localVideoScalingDropdown.setText(
                scalingLabels.getOrNull(scalingTypes.indexOf(user.localScalingType)).orEmpty(),
                false
            )
            remoteVideoScalingDropdown.setText(
                scalingLabels.getOrNull(scalingTypes.indexOf(user.remoteScalingType)).orEmpty(),
                false
            )
        }
    }

}
