package com.sinch.rtc.vvc.reference.app.features.login

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.databinding.FragmentLoginBinding
import com.sinch.rtc.vvc.reference.app.utils.bindings.ViewBindingFragment

class LoginFragment : ViewBindingFragment<FragmentLoginBinding>(R.layout.fragment_login) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun setupBinding(root: View): FragmentLoginBinding = FragmentLoginBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginButton.setOnClickListener {
            activity?.finish()
            findNavController().navigate(R.id.action_loginFragment_to_loggedInActivity)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.settingsMenuItem -> {
                findNavController().navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

}