package com.sinch.rtc.vvc.reference.app.features.calls.newcall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.AppDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.DestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.PSTNDestinationValidator
import com.sinch.rtc.vvc.reference.app.features.calls.newcall.validator.SipDestinationValidator
import java.util.*

class NewCallViewModel(initialCallItem: CallItem?) : ViewModel() {

    private var destinationValidator: DestinationValidator? = null

    private val callItemMutable: MutableLiveData<CallItem> =
        MutableLiveData(initialCallItem ?: CallItem(CallType.AppToPhone, "", Date()))

    val callItem: LiveData<CallItem> get() = callItemMutable

    val isProceedEnabled: LiveData<Boolean> =
        Transformations.map(callItem) {
            destinationValidator?.isCalleeValid(it.destination) ?: true
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

}