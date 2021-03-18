package com.sinch.rtc.vvc.reference.app.domain.calls

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sinch.rtc.vvc.reference.app.application.Constants
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
) : Parcelable
