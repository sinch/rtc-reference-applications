package com.sinch.rtc.vvc.reference.app.storage.converters

import androidx.room.TypeConverter
import com.sinch.android.rtc.calling.CallEndCause

class EndCauseConverter {

    @TypeConverter
    fun toEndCauseType(value: String?) = value?.let { enumValueOf<CallEndCause>(it) }

    @TypeConverter
    fun fromEndCauseType(value: CallEndCause?) = value?.name

}