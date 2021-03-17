package com.sinch.rtc.vvc.reference.app.features.calls.outgoing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem

class OutgoingCallViewModel(callItem: CallItem, application: Application) :
    AndroidViewModel(application)