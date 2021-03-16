package com.sinch.rtc.vvc.reference.app.features.calls.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.databinding.FragmentHistoryBinding
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.features.calls.history.list.CallHistoryAdapter
import com.sinch.rtc.vvc.reference.app.utils.bindings.ViewBindingFragment

class CallHistoryFragment : ViewBindingFragment<FragmentHistoryBinding>(R.layout.fragment_history) {

    private val viewModel: CallHistoryViewModel by viewModels()

    private val adapter = CallHistoryAdapter()

    override fun setupBinding(root: View): FragmentHistoryBinding =
        FragmentHistoryBinding.bind(root)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            callHistoryRecycler.adapter = adapter
            callHistoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        }
        viewModel.historyData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        viewModel.navigationEvents.observe(viewLifecycleOwner) {
            when (it) {
                is OutGoingCall -> navigateToOutgoingCall(it.callItem)
                is NewCall -> navigateToNewCall(it.callItem)
            }
        }
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

}