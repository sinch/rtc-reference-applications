package com.sinch.rtc.vvc.reference.app.features.settings

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sinch.android.rtc.*
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.application.Constants
import com.sinch.rtc.vvc.reference.app.application.service.SinchClientService
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class SettingsViewModel(
    private val app: Application,
    private val userDao: UserDao,
    private val callDao: CallDao,
    private val sinchClient: SinchClient?
) :
    AndroidViewModel(app), PushTokenRegistrationCallback {

    companion object {
        const val TAG = "SettingsViewModel"
    }

    val navigationEvents: SingleLiveEvent<SettingsNavigationEvent> = SingleLiveEvent()
    val loggedInUser: LiveData<User?> = userDao.getLoggedInUserLiveData()

    fun onLogoutClicked() {
        userDao.loadLoggedInUser()?.let { user ->
            userDao.update(user.copy(isLoggedIn = false))
            userController(user.id).unregisterPushToken(this)
            performLogoutCleanup()
            navigationEvents.postValue(Login)
        }
    }

    fun onClearDataClicked() {
        userDao.loadLoggedInUser()?.let {
            callDao.delete(callDao.loadCallHistoryOfUser(it.id))
        }
    }

    fun onLocalScalingChanged(scaling: VideoScalingType) {
        Log.d(TAG, "onLocalScalingChanged to $scaling")
        userDao.loadLoggedInUser()?.let {
            userDao.update(it.copy(localScalingType = scaling))
        }
    }

    fun onRemoteScalingChanged(scaling: VideoScalingType) {
        Log.d(TAG, "onRemoteScalingChanged to $scaling")
        userDao.loadLoggedInUser()?.let {
            userDao.update(it.copy(remoteScalingType = scaling))
        }
    }

    override fun onPushTokenRegistered() {
        Log.d(TAG, "Push token unregistered")
    }

    override fun onPushTokenRegistrationFailed(error: SinchError?) {
        Log.d(TAG, "Push token unregistration failed with error $error")
    }

    private fun performLogoutCleanup() {
        sinchClient?.terminateGracefully()
        app.stopService(Intent(app, SinchClientService::class.java))
    }

    private fun userController(userId: String): UserController =
        Sinch.getUserControllerBuilder()
            .context(app)
            .applicationKey(Constants.APP_KEY)
            .userId(userId)
            .environmentHost(Constants.ENVIRONMENT)
            .build()

}