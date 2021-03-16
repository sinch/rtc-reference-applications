package com.sinch.rtc.vvc.reference.app.features.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.databinding.FragmentSettingsBinding
import com.sinch.rtc.vvc.reference.app.navigation.loggedin.LoggedInActivity
import com.sinch.rtc.vvc.reference.app.utils.bindings.ViewBindingFragment

class SettingsFragment : ViewBindingFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    override fun setupBinding(root: View): FragmentSettingsBinding =
        FragmentSettingsBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        //Simply for demo purposes
        binding.apply {
            logoutButton.isVisible = activity is LoggedInActivity
            logoutButton.setOnClickListener {
                activity?.finish()
                findNavController().navigate(R.id.action_settingsFragment_to_loggedOutActivity)
            }
        }
    }

}