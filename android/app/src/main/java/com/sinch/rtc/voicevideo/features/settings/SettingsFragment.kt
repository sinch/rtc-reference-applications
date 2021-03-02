package com.sinch.rtc.voicevideo.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.navigation.LoggedInActivity
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        //Simply for demo purposes
        logoutButton.isVisible = activity is LoggedInActivity
        logoutButton.setOnClickListener {
            activity?.finish()
            findNavController().navigate(R.id.action_settingsFragment_to_loggedOutActivity)
        }
    }

}