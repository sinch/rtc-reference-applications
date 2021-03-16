package com.sinch.rtc.vvc.reference.app.storage.converters

import androidx.room.TypeConverter
import com.sinch.rtc.vvc.reference.app.domain.calls.CallType

class CallTypeConverter {

    @TypeConverter
    fun toCallType(value: String) = enumValueOf<CallType>(value)

    @TypeConverter
    fun fromCallType(value: CallType) = value.name

}
