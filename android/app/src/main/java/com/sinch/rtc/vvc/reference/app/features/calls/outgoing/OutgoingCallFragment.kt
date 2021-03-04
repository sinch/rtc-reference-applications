package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sinch.rtc.vvc.reference.app.R
import kotlinx.android.synthetic.main.fragment_new_call.*

class OutgoingCallFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_outgoing_call, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callButton.setOnClickListener {
            findNavController().navigate(R.id.action_outgoingCallFragment_to_establishedCallFragment)
        }
    }

}