package com.sinch.rtc.vvc.reference.app.domain.calls

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sinch.android.rtc.calling.Call
import com.sinch.rtc.vvc.reference.app.application.Constants
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.utils.extensions.expectedType
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = Constants.CALL_HISTORY_ITEMS_TABLE_NAME)
data class CallItem(
    val type: CallType,
    val destination: String,
    val startDate: Date,
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    @ColumnInfo(name = Constants.CALL_ITEM_USER_ID_COLUMN_NAME) val userId: String = ""
) : Parcelable {

    constructor(call: Call, user: User) : this(
        call.details.expectedType,
        call.remoteUserId,
        Date(), //started time of call returns Date(0)
        userId = user.id
    )

}