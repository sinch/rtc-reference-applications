package com.sinch.rtc.vvc.reference.app.features.calls.history

import SingleLiveEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType
import com.sinch.rtc.vvc.reference.app.features.calls.history.list.CallHistoryEntryItem
import com.sinch.rtc.vvc.reference.app.features.calls.history.list.DateHeaderItem
import com.sinch.rtc.vvc.reference.app.utils.date.DateFormats
import com.sinch.rtc.vvc.reference.app.utils.sectionedrecycler.BaseItem
import java.util.*

class CallHistoryViewModel : ViewModel(), CallHistoryEntryItem.HistoryItemClicksListener {

    private val fakeData = listOf(
        CallItem(CallType.AppToAppVideo, "aleks1", Date(1614849567000)),
        CallItem(CallType.AppToPhone, "+48123456789", Date(1614845967000)),
        CallItem(CallType.AppToAppVideo, "aleks1", Date(1614824427000)),
        CallItem(CallType.AppToAppAudio, "aleks1", Date(1612441227000)),
        CallItem(CallType.AppToAppVideo, "aleks2", Date(1614849567000)),
        CallItem(CallType.AppToPhone, "+481234569", Date(1614845967000)),
        CallItem(CallType.AppToAppVideo, "aleks3", Date(161482127000)),
        CallItem(CallType.AppToAppAudio, "aleks4", Date(1612441527000)),
        CallItem(CallType.AppToSip, "alek@sinch.com", Date(1609762827000))
    )

    private val navigationEventsMutable: SingleLiveEvent<CallHistoryNavigationEvent> =
        SingleLiveEvent()

    private val historyDataMutable: MutableLiveData<List<BaseItem<*>>> =
        MutableLiveData(generateHeaderedCallItemsList(fakeData))

    val historyData: LiveData<List<BaseItem<*>>> get() = historyDataMutable
    val navigationEvents: SingleLiveEvent<CallHistoryNavigationEvent> get() = navigationEventsMutable

    override fun onVideoClicked(item: CallItem) {
        navigationEventsMutable.postValue(OutGoingCall(item))
    }

    override fun onAudioClicked(item: CallItem) {
        navigationEventsMutable.postValue(OutGoingCall(item))
    }

    override fun onCalleeNameClicked(item: CallItem) {
        navigationEventsMutable.postValue(NewCall(item))
    }

    private fun generateHeaderedCallItemsList(items: List<CallItem>): List<BaseItem<*>> {
        val sortedByDateHistoryItems = items.sortedByDescending {
            it.startDate
        }
        val historyItemsWithHeaders = mutableListOf<BaseItem<*>>()
        var currentDateString: String? = null
        sortedByDateHistoryItems.forEach { callHistoryItem ->
            DateFormats.dateOnlyDefault(callHistoryItem.startDate).let {
                if (it != currentDateString) {
                    historyItemsWithHeaders.add(DateHeaderItem(it))
                    currentDateString = it
                }
            }
            historyItemsWithHeaders.add(CallHistoryEntryItem(callHistoryItem, this))
        }
        return historyItemsWithHeaders
    }

}