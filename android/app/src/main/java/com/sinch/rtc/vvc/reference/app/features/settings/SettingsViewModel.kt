package com.sinch.rtc.vvc.reference.app.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class SettingsViewModel(
    application: Application,
    private val userDao: UserDao,
    private val callDao: CallDao
) :
    AndroidViewModel(application) {

    val navigationEvents: SingleLiveEvent<SettingsNavigationEvent> = SingleLiveEvent()

    fun onLogoutClicked() {
        userDao.loadLoggedInUser()?.let {
            userDao.update(it.copy(isLoggedIn = false))
            navigationEvents.postValue(Login)
        }
    }

    fun onClearDataClicked() {
        userDao.loadLoggedInUser()?.let {
            val loggedInUserCallHistory = callDao.getLiveDataOfUserCallHistory(it.id).value
            if (loggedInUserCallHistory != null) {
                callDao.delete(loggedInUserCallHistory)
            }
        }
    }
}