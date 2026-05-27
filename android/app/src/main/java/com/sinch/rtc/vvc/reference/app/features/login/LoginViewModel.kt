package com.sinch.rtc.vvc.reference.app.features.login

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.huawei.agconnect.AGConnectInstance
import com.huawei.hms.aaid.HmsInstanceId
import com.sinch.rtc.vvc.reference.app.BuildConfig
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.service.SinchClientService
import com.sinch.rtc.vvc.reference.app.application.service.SinchRegistrationStatus
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.domain.push.PushProvider
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.storage.prefs.SharedPrefsManager
import com.sinch.rtc.vvc.reference.app.utils.jwt.getString
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class LoginViewModel(
    private val application: Application,
    private val prefsManager: SharedPrefsManager,
    private val userDao: UserDao,
    private val callDao: CallDao
) :
    AndroidViewModel(application), ServiceConnection {

    companion object {
        const val TAG = "LoginViewModel"
        const val LOGGING_TIMEOUT_MS = 10000L
    }

    private val viewModelState: MutableLiveData<LoginViewState> = MutableLiveData(Idle)
    private var loggingTimeoutHandler: Handler? = null

    private var serviceBinder: SinchClientService.SinchClientServiceBinder? = null
    private val registrationObserver = Observer<SinchRegistrationStatus> { status ->
        onRegistrationStatusChanged(status)
    }

    val errorMessages: SingleLiveEvent<String> = SingleLiveEvent()
    val navigationEvents: SingleLiveEvent<LoginNavigationEvent> = SingleLiveEvent()

    val availablePushProviders: List<PushProvider> = PushProvider.availableProviders()

    private val _selectedPushProvider = MutableLiveData(
        PushProvider.defaultProvider()
    )
    val selectedPushProvider: LiveData<PushProvider?> get() = _selectedPushProvider

    val isLoginButtonEnabled: LiveData<Boolean>
        get() = viewModelState.map {
            when (it) {
                Idle -> true
                else -> false
            }
        }

    init {
        application.bindService(
            Intent(application, SinchClientService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    fun onPushProviderSelected(provider: PushProvider) {
        _selectedPushProvider.value = provider
    }

    fun onLoginClicked(username: String) {
        Log.d(TAG, "Login clicked with username $username")
        login(username)
    }

    private fun login(username: String) {
        val provider = _selectedPushProvider.value ?: PushProvider.defaultProvider()!!

        viewModelState.value = Logging(
            username,
            isUserRegistered = false,
            isPushTokenRegistered = false
        )
        prefsManager.selectedPushProvider = provider.name

        when (provider) {
            PushProvider.FCM -> loginWithFcm(username)
            PushProvider.HMS -> loginWithHms(username)
        }
    }

    private fun loginWithFcm(username: String) {
        if (prefsManager.fcmRegistrationToken.isEmpty()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isSuccessful) {
                    prefsManager.fcmRegistrationToken = it.result.orEmpty()
                    startRegistration(username)
                } else {
                    resetToIdleWithErrorMessage(getString(R.string.fcm_not_available))
                }
            }
        } else {
            startRegistration(username)
        }
    }

    private fun loginWithHms(username: String) {
        if (prefsManager.hmsRegistrationToken.isEmpty()) {
            viewModelScope.launch {
                try {
                    val token = withContext(Dispatchers.IO) {
                        val appId = AGConnectInstance.getInstance().options
                            .getString("client/app_id")
                        HmsInstanceId.getInstance(application).getToken(appId, "HCM")
                    }
                    if (!token.isNullOrEmpty()) {
                        prefsManager.hmsRegistrationToken = token
                        startRegistration(username)
                    } else {
                        resetToIdleWithErrorMessage(getString(R.string.hms_not_available))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "HMS token retrieval failed", e)
                    resetToIdleWithErrorMessage(getString(R.string.hms_not_available))
                }
            }
        } else {
            startRegistration(username)
        }
    }

    private fun startRegistration(username: String) {
        initLoggingTimeoutTimer()
        serviceBinder?.startRegistration(username)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service !is SinchClientService.SinchClientServiceBinder) return
        serviceBinder = service
        service.registrationStatus.observeForever(registrationObserver)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        serviceBinder = null
    }

    private fun onRegistrationStatusChanged(status: SinchRegistrationStatus) {
        val current = viewModelState.value as? Logging ?: return
        val error = status.error
        if (error != null) {
            failLogin("${error.message}\nExtras: ${error.extras}")
            return
        }
        val newState = Logging(current.username, status.isUserRegistered, status.isPushTokenRegistered)
        viewModelState.value = newState
        if (newState.isLoggingComplete) {
            cancelLoggingTimeoutTimer()
            viewModelState.value = Idle
            userDao.insert(User(newState.username, isLoggedIn = true))
            insertFakeCallHistoryIfNeeded(newState.username)
            navigationEvents.postValue(Dashboard)
        }
    }

    private fun failLogin(message: String) {
        cancelLoggingTimeoutTimer()
        viewModelState.value = Idle
        serviceBinder?.cancelRegistration()
        errorMessages.postValue(message)
    }

    private fun initLoggingTimeoutTimer() {
        cancelLoggingTimeoutTimer()
        loggingTimeoutHandler = Handler(Looper.getMainLooper()).apply {
            postDelayed({
                failLogin(getString(R.string.logging_timeout_error_message))
            }, LOGGING_TIMEOUT_MS)
        }
    }

    private fun resetToIdleWithErrorMessage(message: String) {
        cancelLoggingTimeoutTimer()
        viewModelState.value = Idle
        errorMessages.postValue(message)
    }

    private fun cancelLoggingTimeoutTimer() {
        loggingTimeoutHandler?.removeCallbacksAndMessages(null)
        loggingTimeoutHandler = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelLoggingTimeoutTimer()
        serviceBinder?.registrationStatus?.removeObserver(registrationObserver)
        application.unbindService(this)
        serviceBinder = null
    }

    private fun insertFakeCallHistoryIfNeeded(userId: String) {
        val fakeData = listOf(
            CallItem(CallType.AppToAppVideo, "aleks1", Date(1614849567000), userId = userId),
            CallItem(CallType.AppToPhone, "+48123456789", Date(1614845967000), userId = userId),
            CallItem(CallType.AppToAppVideo, "aleks1", Date(1614824427000), userId = userId),
            CallItem(CallType.AppToAppAudio, "aleks1", Date(1612441227000), userId = userId),
            CallItem(CallType.AppToAppVideo, "aleks2", Date(1614849567000), userId = userId),
            CallItem(CallType.AppToPhone, "+481234569", Date(1614845967000), userId = userId),
            CallItem(CallType.AppToAppVideo, "aleks3", Date(161482127000), userId = userId),
            CallItem(CallType.AppToAppAudio, "aleks4", Date(1612441527000), userId = userId),
            CallItem(CallType.AppToSip, "alek@sinch.com", Date(1609762827000), userId = userId)
        )

        if (callDao.loadCallHistoryOfUser(userId).isEmpty()) {
            callDao.insert(fakeData)
        }
    }

}
