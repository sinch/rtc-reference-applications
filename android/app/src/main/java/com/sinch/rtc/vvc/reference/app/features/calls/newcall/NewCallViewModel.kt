package com.sinch.rtc.vvc.reference.app.features.calls.newcall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.sinch.rtc.vvc.reference.app.domain.AppConfig
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.domain.calls.insertAndGetWithGeneratedId
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.AppDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.DestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.PSTNDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.SipDestinationValidator
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent
import java.util.Date

class NewCallViewModel(
    initialCallItem: CallItem?,
    app: Application,
    private val usedConfig: AppConfig,
    private val userDao: UserDao,
    private val callDao: CallDao
) : AndroidViewModel(app) {

    private var destinationValidator: DestinationValidator? = null

    private val callItemMutable: MutableLiveData<CallItem> =
        MutableLiveData(
            initialCallItem ?: CallItem(
                CallType.AppToAppAudio,
                "",
                Date(),
                userId = loggedInUser?.id.orEmpty()
            )
        )

    private val loggedInUser: User? get() = userDao.loadLoggedInUser()

    val navigationEvents: SingleLiveEvent<NewCallNavigationEvent> = SingleLiveEvent()

    val callItem: LiveData<CallItem> get() = callItemMutable

    val isProceedEnabled: LiveData<Boolean> =
        callItem.map { destinationValidator?.isCalleeValid(it.destination) ?: true }

    val loggedInUserLiveData get() = userDao.getLoggedInUserLiveData()

    val callTypes: List<CallType>
        get() {
            val base = mutableListOf(
                CallType.AppToAppAudio,
                CallType.AppToAppVideo,
                CallType.AppToSip
            )
            return if (!usedConfig.cli.isNullOrBlank()) {
                base.apply { add(CallType.AppToPhone) }
            } else {
                base
            }
        }

    fun onCallTypeSelected(newType: CallType) {
        destinationValidator = when (newType) {
            CallType.AppToSip -> SipDestinationValidator()
            CallType.AppToPhone -> PSTNDestinationValidator()
            CallType.AppToAppAudio, CallType.AppToAppVideo -> AppDestinationValidator()
        }
        callItemMutable.value = callItem.value?.copy(type = newType)
    }

    fun onNewDestination(newDestination: String) {
        if (newDestination != callItem.value?.destination) {
            callItemMutable.value = callItem.value?.copy(destination = newDestination)
        }
    }

    fun onCallButtonClicked() {
        val newCallItem = callItem.value?.copy(startDate = Date(), itemId = 0) ?: return
        navigationEvents.postValue(OutgoingCall(callDao.insertAndGetWithGeneratedId(newCallItem)))
    }

}