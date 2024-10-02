package com.sinch.rtc.vvc.reference.app.features.calls.summary

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallDirection
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.databinding.DialogFinishedCallBinding
import java.util.Date

class CallSummaryDialogFragment : DialogFragment() {

    private var _binding: DialogFinishedCallBinding? = null
    private val binding get() = _binding!!

    var call: Call? = null
        set(value) {
            field = value
            fillCallDetails()
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFinishedCallBinding.inflate(layoutInflater)
        binding.okButton.setOnClickListener {
            dismiss()
        }
        fillCallDetails()
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fillCallDetails() {
        if (_binding == null) {
            return
        }
        val call = call ?: return
        binding.apply {
            remoteUserIdTextView.text = call.remoteUserId
            durationTextView.text = call.details.duration.toString()
            directionTextView.text = getString(
                if (call.direction == CallDirection.OUTGOING)
                    R.string.direction_outgoing
                else R.string.direction_incoming
            )
            endCauseTextView.text = call.details.endCause.toString()
            errorTextView.text = call.details.error?.message ?: getString(R.string.error_none)
            startedTimeTextView.text = call.details.startedTime.asFormattedString()
            progressedTimeTextView.text = call.details.progressedTime.asFormattedString()
            rungTimeTextView.text = call.details.rungTime.asFormattedString()
            answeredTimeTextView.text = call.details.answeredTime.asFormattedString()
            establishedTimeTextView.text = call.details.establishedTime.asFormattedString()
            endedTimeTextView.text = call.details.endedTime.asFormattedString()
        }
    }

    private fun Date.asFormattedString(): String {
        if (this.time == 0L) {
            return "-"
        }
        return toString()
    }
}