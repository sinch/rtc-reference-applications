package com.sinch.rtc.vvc.reference.app.navigation.loggedin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent

class LoggedInViewModel(app: Application, private val userDao: UserDao) : AndroidViewModel(app) {

    val navigationEvents: SingleLiveEvent<LoggedInNavigationEvent> = SingleLiveEvent()

    fun onViewCreated() {
        if (userDao.loadLoggedInUser() == null) {
            navigationEvents.postValue(Login)
        } else {
            navigationEvents.postValue(Dashboard)
        }
    }

}