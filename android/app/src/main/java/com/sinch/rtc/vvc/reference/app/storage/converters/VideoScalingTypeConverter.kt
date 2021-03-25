package com.sinch.rtc.vvc.reference.app.storage.converters

import androidx.room.TypeConverter
import com.sinch.android.rtc.video.VideoScalingType

class VideoScalingTypeConverter {

    @TypeConverter
    fun toCallType(value: String) = enumValueOf<VideoScalingType>(value)

    @TypeConverter
    fun fromCallType(value: VideoScalingType) = value.name

}