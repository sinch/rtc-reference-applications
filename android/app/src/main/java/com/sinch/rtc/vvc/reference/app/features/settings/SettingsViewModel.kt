package com.sinch.rtc.vvc.reference.app.features.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class SettingsViewModel(
    application: Application,
    private val userDao: UserDao,
    private val callDao: CallDao
) :
    AndroidViewModel(application) {

    companion object {
        const val TAG = "SettingsViewModel"
    }

    val navigationEvents: SingleLiveEvent<SettingsNavigationEvent> = SingleLiveEvent()
    val loggedInUser: LiveData<User?> = userDao.getLoggedInUserLiveData()

    fun onLogoutClicked() {
        userDao.loadLoggedInUser()?.let {
            userDao.update(it.copy(isLoggedIn = false))
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

}