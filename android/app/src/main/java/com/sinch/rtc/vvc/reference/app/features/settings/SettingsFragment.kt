package com.sinch.rtc.vvc.reference.app.features.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sinch.android.rtc.video.VideoScalingType
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

    override fun setupBinding(root: View): FragmentSettingsBinding =
        FragmentSettingsBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        setupScalingAdapters()
        setupConfigAdapters()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.apply {
            logoutButton.setOnClickListener {
                viewModel.onLogoutClicked()
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
            android.R.layout.simple_spinner_dropdown_item,
            configSpinnerValues
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.configsSpinner.adapter = itemsAdapter
        binding.configsSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.onEnvSpinnerItemChanged(
                        configSpinnerValues[position], binding.appKeyInputEditText.text.toString(),
                        binding.appSecretInputEditText.text.toString(),
                        binding.environmentInputEditText.text.toString(),
                        binding.cliInputEditText.text.toString()
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            binding.updateDevSettingsButton
            listOf(
                binding.loggedInSettings,
                binding.clearDataButton,
                binding.logoutButton
            ).forEach {
                it.isVisible = isAnyUserLoggedIn
            }
            listOf(
                binding.configsSpinner,
                binding.appKeyInputLayout,
                binding.appSecretInputLayout,
                binding.environmentInputLayout,
                binding.cliInputLayout
            ).forEach {
                it.isEnabled = !isAnyUserLoggedIn
            }
            binding.updateDevSettingsButton.isVisible = !isAnyUserLoggedIn
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
            binding.configsSpinner.setSelection(configSpinnerValues.indexOf(it.name))
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
            localVideoScalingSpinner.setSelection(scalingTypes.indexOf(user.localScalingType))
            remoteVideoScalingSpinner.setSelection(scalingTypes.indexOf(user.remoteScalingType))
        }
    }

}