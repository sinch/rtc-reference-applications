package com.sinch.rtc.vvc.reference.app.domain.calls

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallEndCause
import com.sinch.rtc.vvc.reference.app.R
import com.sinch.rtc.vvc.reference.app.application.Constants
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.storage.converters.CallTypeConverter
import com.sinch.rtc.vvc.reference.app.storage.converters.DateConverter
import com.sinch.rtc.vvc.reference.app.storage.converters.EndCauseConverter
import com.sinch.rtc.vvc.reference.app.utils.extensions.appendLineIfValuePresent
import com.sinch.rtc.vvc.reference.app.utils.extensions.expectedType
import com.sinch.rtc.vvc.reference.app.utils.extensions.isSet
import com.sinch.rtc.vvc.reference.app.utils.extensions.valueOrNullIfNotSet
import java.text.DateFormat
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = Constants.CALL_HISTORY_ITEMS_TABLE_NAME)
data class CallItem(
    @TypeConverters(CallTypeConverter::class)
    val type: CallType,
    val destination: String,
    @TypeConverters(DateConverter::class)
    val startDate: Date,
    @TypeConverters(DateConverter::class)
    val establishedDate: Date? = null,
    @TypeConverters(DateConverter::class)
    val endDate: Date? = null,
    @TypeConverters(EndCauseConverter::class)
    val endCause: CallEndCause? = null,
    val errorMessage: String? = null,
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    @ColumnInfo(name = Constants.CALL_ITEM_USER_ID_COLUMN_NAME) val userId: String = ""
) : Parcelable {

    constructor(call: Call, user: User) : this(
        call.details.expectedType,
        call.remoteUserId,
        if (call.details.startedTime.isSet) call.details.startedTime else Date(), //started time of call returns Date(0)
        call.details.establishedTime.valueOrNullIfNotSet,
        call.details.endedTime.valueOrNullIfNotSet,
        call.details.endCause,
        call.details.error?.message,
        userId = user.id,
    )

    fun withUpdatedCallData(call: Call): CallItem {
        val details = call.details
        return copy(
            startDate = if (details.startedTime.isSet) details.startedTime else this.startDate,
            establishedDate = details.establishedTime.valueOrNullIfNotSet,
            endDate = details.endedTime.valueOrNullIfNotSet,
            endCause = details.endCause,
            errorMessage = details.error?.message
        )
    }

}

fun CallItem.constructDetailsInfo(context: Context): String {
    val dateTime = DateFormat.getDateTimeInstance()
    return StringBuilder().apply {
        appendLine(
            String.format(
                context.getString(
                    R.string.started_at,
                    dateTime.format(startDate)
                )
            )
        )
        appendLineIfValuePresent(
            context.getString(R.string.established_at),
            establishedDate?.let { dateTime.format(it) })
        appendLineIfValuePresent(context.getString(R.string.ended_at), endDate?.let { dateTime.format(it) })
        if (establishedDate != null && endDate != null) {
            appendLine(
                String.format(
                    context.getString(R.string.duration),
                    (endDate.time - establishedDate.time).toFloat() / 1000.0
                )
            )
        }
        appendLineIfValuePresent(context.getString(R.string.end_cause), endCause?.name)
        appendLineIfValuePresent(context.getString(R.string.fail_msg), errorMessage)
    }.toString().trim()
}


