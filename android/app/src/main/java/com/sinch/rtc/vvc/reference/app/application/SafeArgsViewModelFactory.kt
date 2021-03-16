package com.sinch.rtc.vvc.reference.app.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavArgs
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.NewCallFragmentArgs
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.NewCallViewModel

/**
 * ViewModelProvider Factory responsible for creating view models based on safe args Kotlin plugin
 * If there is a better way of solving this problem pls make a proposal.
 */
class SafeArgsViewModelFactory<Args : NavArgs>(val arguments: Args) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when (modelClass) {
            NewCallViewModel::class.java -> {
                val newCalArgs = arguments as NewCallFragmentArgs
                NewCallViewModel(newCalArgs.initialCallItem) as T
            }
            else -> throw IllegalArgumentException("$modelClass not defined for safe arguments factory")
        }
}