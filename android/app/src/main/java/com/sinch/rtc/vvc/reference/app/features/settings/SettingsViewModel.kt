package com.sinch.rtc.vvc.reference.app.features.settings

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sinch.android.rtc.*
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.application.service.SinchClientService
import com.sinch.rtc.vvc.reference.app.domain.AppConfig
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class SettingsViewModel(
    private val app: Application,
    private val userDao: UserDao,
    private val callDao: CallDao,
    private val sharedPrefsManager: SharedPrefsManager,
    private val sinchClient: SinchClient?
) :
    AndroidViewModel(app), PushTokenRegistrationCallback {

    companion object {
        const val TAG = "SettingsViewModel"
    }

    private val devDataMutable = MutableLiveData(sharedPrefsManager.usedConfig)

    val navigationEvents: SingleLiveEvent<SettingsNavigationEvent> = SingleLiveEvent()
    val loggedInUser: LiveData<User?> = userDao.getLoggedInUserLiveData()
    val devDataLiveData: LiveData<AppConfig> = devDataMutable

    fun onLogoutClicked() {
        logoutUser()
    }

    fun onEnvSpinnerItemChanged(
        name: String,
        typedAppKey: String,
        typedAppSecret: String,
        typedEnv: String,
        typedCli: String
    ) {
        val defaultConfigWithChosenName =
            sharedPrefsManager.defaultConfigs.firstOrNull { it.name == name }
        if (defaultConfigWithChosenName == null) {
            devDataMutable.value =
                AppConfig(AppConfig.CUSTOM_CONFIG_NAME, typedAppKey, typedAppSecret, typedEnv, typedCli, true)
        } else {
            devDataMutable.value = defaultConfigWithChosenName
        }
    }

    fun onUpdateDevSettingsClicked(newAppKey: String, newAppSecret: String, newEnv: String, newCli: String) {
        val currentAppConfig = devDataLiveData.value ?: return
        if (currentAppConfig.isCustom) {
            devDataMutable.value = currentAppConfig.copy(
                appKey = newAppKey,
                appSecret = newAppSecret,
                environment = newEnv,
                cli = newCli
            )
        }
        sharedPrefsManager.usedConfig = devDataMutable.value ?: return
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

    private fun logoutUser() {
        userDao.loadLoggedInUser()?.let { user ->
            userDao.update(user.copy(isLoggedIn = false))
            userController(user).unregisterPushToken(this)
            performLogoutCleanup()
            navigationEvents.postValue(Login)
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

    private fun userController(user: User): UserController =
        Sinch.getUserControllerBuilder()
            .context(app)
            .applicationKey(sharedPrefsManager.usedConfig.appKey)
            .userId(user.id)
            .environmentHost(sharedPrefsManager.usedConfig.environment)
            .build()

}