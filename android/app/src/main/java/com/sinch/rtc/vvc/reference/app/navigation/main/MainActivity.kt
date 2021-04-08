package com.sinch.rtc.vvc.reference.app.navigation.main

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout
import com.sinch.rtc.vvc.reference.app.MainNavGraphDirections
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory
import com.sinch.rtc.vvc.reference.app.databinding.ActivityMainBinding
import com.sinch.rtc.vvc.reference.app.domain.AppConfig
import com.sinch.rtc.vvc.reference.app.features.calls.incoming.IncomingCallInitialData
import com.sinch.rtc.vvc.reference.app.utils.base.activity.ViewBindingActivity
import com.sinch.rtc.vvc.reference.app.utils.extensions.jsonAssetAsObject

class MainActivity : ViewBindingActivity<ActivityMainBinding>() {

    companion object {
        const val INITIAL_INCOMING_CALL_DATA = "INC_CALL_DATA"
    }

    private val viewModel: MainViewModel by viewModels {
        NoArgsRTCVoiceVideoRefAppAndroidViewModelFactory(application)
    }

    private val topDestinations = listOf(
        R.id.newCallFragment,
        R.id.callHistoryFragment
    )

    private val tabPositionToDestinationMap = mapOf(
        0 to R.id.newCallFragment,
        1 to R.id.callHistoryFragment
    )

    //Workaround for IllegalStateException thrown when trying to access navigation host usual way
    //https://stackoverflow.com/questions/50502269/illegalstateexception-link-does-not-have-a-navcontroller-set
    private val navHostFragment
        get() =
            supportFragmentManager.findFragmentById(R.id.mainNavigationHostFragment) as NavHostFragment

    override fun setupBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeNavigationEvents()
        parseIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent()
    }

    override fun onResume() {
        super.onResume()
        clearNotifications()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.settingsMenuItem -> {
                navHostFragment.navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.settingsMenuItem)?.isVisible =
            topDestinations.contains(navHostFragment.navController.currentDestination?.id)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setupNavigation() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                navigateToTab(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                navigateToTab(tab)
            }
        })

        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.tabLayout.isVisible = topDestinations.contains(destination.id)
            if (topDestinations.indexOf(destination.id) >= 0) {
                binding.tabLayout.setScrollPosition(
                    topDestinations.indexOf(destination.id),
                    0f,
                    true
                )
            }
            invalidateOptionsMenu()
        }
    }

    private fun clearNotifications() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    private fun observeNavigationEvents() {
        viewModel.navigationEvents.observe(this) {
            when (it) {
                Login -> {
                    finish()
                    navHostFragment.navController.navigate(R.id.to_logged_out_flow)
                }
                Dashboard -> {
                    setupNavigation()
                }
                is IncomingCall -> {
                    setupNavigation()
                    val extraOptions = if (it.pop) {
                        NavOptions.Builder().setPopUpTo(R.id.newCallFragment, true).build()
                    } else {
                        NavOptions.Builder().build()
                    }
                    navHostFragment.navController.navigate(
                        MainNavGraphDirections.toIncomingCall(
                            it.callId,
                            it.initialAction,
                        ), extraOptions
                    )
                }
            }
        }
    }

    private fun parseIntent() {
        val incomingCallData =
            intent.getParcelableExtra(INITIAL_INCOMING_CALL_DATA) as? IncomingCallInitialData
        incomingCallData?.let {
            viewModel.onIncomingCallRequested(it)
            intent.extras?.clear()
        }
    }

    private fun navigateToTab(tab: TabLayout.Tab?) {
        tabPositionToDestinationMap[tab?.position]?.let {
            navHostFragment.navController.apply {
                navigate(it, null, NavOptions.Builder().setPopUpTo(it, true).build())
            }
        }
    }

}