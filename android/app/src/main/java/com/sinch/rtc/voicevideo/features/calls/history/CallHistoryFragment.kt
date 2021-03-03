package com.sinch.rtc.voicevideo.features.calls.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sinch.rtc.voicevideo.R
import com.sinch.rtc.voicevideo.domain.calls.CallHistoryItem
import com.sinch.rtc.voicevideo.domain.calls.CallType
import kotlinx.android.synthetic.main.fragment_history.*
import java.util.*

class CallHistoryFragment : Fragment(), CallHistoryAdapter.HistoryItemClicksListener {

    private val fakeData = Random().let {
        listOf(
            CallHistoryItem(CallType.Video, "aleks1", Date(it.nextLong())),
            CallHistoryItem(CallType.PSTN, "+48123456789", Date(it.nextLong())),
            CallHistoryItem(CallType.Video, "aleks1", Date(it.nextLong())),
            CallHistoryItem(CallType.Audio, "aleks1", Date(it.nextLong())),
            CallHistoryItem(CallType.SIP, "alek@sinch.com", Date(it.nextLong()))
        )
    }

    private val adapter = CallHistoryAdapter(fakeData, this)

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
        adapter.notifyDataSetChanged()
    }

    override fun onVoiceClicked(item: CallHistoryItem) {
        navigateToOutgoingCall(item)
    }

    override fun onAudioClicked(item: CallHistoryItem) {
        navigateToOutgoingCall(item)
    }

    override fun onCalleeNameClicked(item: CallHistoryItem) {
        navigateToNewCall(item)
    }

    private fun navigateToOutgoingCall(item: CallHistoryItem) {
        findNavController().navigate(R.id.action_callHistoryFragment_to_outgoingCallFragment)
    }

    private fun navigateToNewCall(item: CallHistoryItem) {
        findNavController().navigate(R.id.action_callHistoryFragment_to_chooseRecipientFragment)
    }

}