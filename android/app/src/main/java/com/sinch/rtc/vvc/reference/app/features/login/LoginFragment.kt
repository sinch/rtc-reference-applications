package com.sinch.rtc.vvc.reference.app.features.login

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.FragmentLoginBinding
import com.sinch.rtc.vvc.reference.app.utils.base.fragment.ViewBindingFragment
import com.sinch.rtc.vvc.reference.app.utils.extensions.areAllPermissionsGranted
import com.sinch.rtc.vvc.reference.app.utils.extensions.makeMultiline

class LoginFragment : ViewBindingFragment<FragmentLoginBinding>(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels {
        NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory(requireActivity().application)
    }

    override fun setupBinding(root: View): FragmentLoginBinding = FragmentLoginBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_login, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.settingsMenuItem -> {
                            findNavController().navigate(R.id.settingsFragment)
                            true
                        }
                        else -> false
                    }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        binding.loginButton.setOnClickListener {
            viewModel.onLoginClicked(binding.loginInputEditText.text.toString())
        }
        viewModel.isLoginButtonEnabled.observe(viewLifecycleOwner) {
            binding.loginButton.isEnabled = it
        }
        viewModel.errorMessages.observe(viewLifecycleOwner) {
            showError(it)
        }
        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            handleNavigationEvent(it)
        }
        requestBasePermissions()
    }

    private fun requestBasePermissions() {
        val permissions = mutableListOf(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions(permissions) {
            if (!it.areAllPermissionsGranted) {
                showError(getString(R.string.permissions_missing_error))
            }
        }
    }

    private fun handleNavigationEvent(navigationEvent: LoginNavigationEvent) {
        when (navigationEvent) {
            Dashboard -> {
                activity?.finish()
                findNavController().navigate(R.id.action_loginFragment_to_mainActivity)
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
            .makeMultiline().show()
    }

}
