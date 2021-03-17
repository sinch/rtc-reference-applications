package com.sinch.rtc.vvc.reference.app.features.calls.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.features.calls.history.list.CallHistoryEntryItem
import com.sinch.rtc.vvc.reference.app.features.calls.history.list.DateHeaderItem
import com.sinch.rtc.vvc.reference.app.utils.date.DateFormats
import com.sinch.rtc.vvc.reference.app.utils.mvvm.SingleLiveEvent
import com.sinch.rtc.vvc.reference.app.utils.sectionedrecycler.BaseItem
import java.util.*

class CallHistoryViewModel(
    val app: Application,
    private val loggedInUser: User,
    private val callDao: CallDao
) : AndroidViewModel(app), CallHistoryEntryItem.HistoryItemClicksListener {

    private val callItemsHistory: LiveData<List<CallItem>> by lazy {
        callDao.getLiveDataOfUserCallHistory(loggedInUser.id)
    }

    private val navigationEventsMutable: SingleLiveEvent<CallHistoryNavigationEvent> =
        SingleLiveEvent()

    val historyData: LiveData<List<BaseItem<*>>>
        get() = Transformations.map(callItemsHistory) {
            generateHeaderedCallItemsList(it)
        }

    val navigationEvents: SingleLiveEvent<CallHistoryNavigationEvent> get() = navigationEventsMutable

    override fun onVideoClicked(item: CallItem) {
        saveNewCallHistoryItem(item)
        navigationEventsMutable.postValue(OutGoingCall(item))
    }

    override fun onAudioClicked(item: CallItem) {
        saveNewCallHistoryItem(item)
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

    private fun saveNewCallHistoryItem(baseItem: CallItem) {
        callDao.insert(baseItem.copy(itemId = 0, startDate = Date()))
    }

}