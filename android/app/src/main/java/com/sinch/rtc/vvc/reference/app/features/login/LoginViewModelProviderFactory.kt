package com.sinch.rtc.vvc.reference.app.features.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sinch.rtc.vvc.reference.app.utils.jwt.FakeJWTFetcher

class LoginViewModelProviderFactory(private val application: Application) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            LoginViewModel::class.java -> {
                LoginViewModel(application, FakeJWTFetcher()) as T
            }
            else -> super.create(modelClass)
        }
    }

}