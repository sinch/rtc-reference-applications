package com.sinch.rtc.voicevideo.features.calls.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.domain.calls.CallItem
import com.sinch.rtc.voicevideo.domain.calls.CallType
import com.sinch.rtc.voicevideo.features.calls.history.list.CallHistoryAdapter
import com.sinch.rtc.voicevideo.features.calls.history.list.CallHistoryEntryItem
import com.sinch.rtc.voicevideo.features.calls.history.list.DateHeaderItem
import com.sinch.rtc.voicevideo.utils.date.DateFormats
import com.sinch.rtc.voicevideo.utils.sectionedrecycler.BaseItem
import kotlinx.android.synthetic.main.fragment_history.*
import java.util.*

class CallHistoryFragment : Fragment(), CallHistoryEntryItem.HistoryItemClicksListener {

    private val fakeData = Random().let {
        listOf(
            CallItem(CallType.Video, "aleks1", Date(1614849567000)),
            CallItem(CallType.PSTN, "+48123456789", Date(1614845967000)),
            CallItem(CallType.Video, "aleks1", Date(1614824427000)),
            CallItem(CallType.Audio, "aleks1", Date(1612441227000)),
            CallItem(CallType.Video, "aleks2", Date(1614849567000)),
            CallItem(CallType.PSTN, "+481234569", Date(1614845967000)),
            CallItem(CallType.Video, "aleks3", Date(161482127000)),
            CallItem(CallType.Audio, "aleks4", Date(1612441527000)),
            CallItem(CallType.SIP, "alek@sinch.com", Date(1609762827000))
        )
    }

    private val adapter = CallHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callHistoryRecycler.adapter = adapter
        callHistoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        adapter.submitList(generateHeaderedCallItemsList(fakeData))
    }

    override fun onVideoClicked(item: CallItem) {
        navigateToOutgoingCall(item)
    }

    override fun onAudioClicked(item: CallItem) {
        navigateToOutgoingCall(item)
    }

    override fun onCalleeNameClicked(item: CallItem) {
        navigateToNewCall(item)
    }

    private fun navigateToOutgoingCall(item: CallItem) {
        findNavController().navigate(R.id.action_callHistoryFragment_to_outgoingCallFragment)
    }

    private fun navigateToNewCall(item: CallItem) {
        findNavController().navigate(
            CallHistoryFragmentDirections.actionCallHistoryFragmentToNewCallFragment(
                item
            )
        )
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