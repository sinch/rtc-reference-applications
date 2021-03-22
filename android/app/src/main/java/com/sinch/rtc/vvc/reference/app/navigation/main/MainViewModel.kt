package com.sinch.rtc.vvc.reference.app.navigation.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.sinch.android.rtc.*
import com.sinch.rtc.vvc.reference.app.application.Constants
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.utils.jwt.JWTFetcher
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class MainViewModel(
    private val app: Application,
    private val jwtFetcher: JWTFetcher,
    private val userDao: UserDao
) :
    AndroidViewModel(app),
    SinchClientListener {

    companion object {
        const val TAG = "MainViewModel"
    }

    val navigationEvents: SingleLiveEvent<MainNavigationEvent> = SingleLiveEvent()

    var sinchClient: SinchClient? = null
        private set

    fun onViewCreated() {
        val loggedInUser = userDao.loadLoggedInUser()
        if (loggedInUser == null) {
            navigationEvents.postValue(Login)
        } else {
            registerSinchClient(userId = loggedInUser.id)
            navigationEvents.postValue(Dashboard)
        }
    }

    private fun registerSinchClient(userId: String) {
        if (sinchClient != null && sinchClient?.isStarted == true) {
            return
        }
        sinchClient = Sinch.getSinchClientBuilder()
            .context(app)
            .environmentHost(Constants.ENVIRONMENT)
            .userId(userId)
            .applicationKey(Constants.APP_KEY)
            .build()
            .apply {
                addSinchClientListener(this@MainViewModel)
                setSupportManagedPush(true)
                start()
            }
    }

    override fun onCredentialsRequired(clientRegistration: ClientRegistration?) {
        Log.d(TAG, "onCredentialsRequired $clientRegistration")
        val loggedInUser = userDao.loadLoggedInUser();
        if (loggedInUser != null) {
            jwtFetcher.acquireJWT(Constants.APP_KEY, loggedInUser.id) { jwt ->
                clientRegistration?.register(jwt)
            }
        }
    }

    override fun onUserRegistered() {
        Log.d(TAG, "onUserRegistered called")
    }

    override fun onUserRegistrationFailed(p0: SinchError?) {
        Log.d(TAG, "onUserRegistrationFailed $p0")
    }

    override fun onPushTokenRegistered() {
        Log.d(TAG, "onPushTokenRegistered")
    }

    override fun onPushTokenRegistrationFailed(p0: SinchError?) {
        Log.d(TAG, "onPushTokenRegistrationFailed $p0")
    }

    override fun onClientStarted(p0: SinchClient?) {
        Log.d(TAG, "onClientStarted")
    }

    override fun onClientFailed(p0: SinchClient?, p1: SinchError?) {
        Log.d(TAG, "onClientFailed $p1")
    }

    override fun onLogMessage(p0: Int, p1: String?, p2: String?) {
        Log.d(TAG, "onLogMessage $p1 $p2")
    }

}