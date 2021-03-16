package com.sinch.rtc.vvc.reference.app.domain.calls

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity
data class CallItem(
    val type: CallType,
    val destination: String,
    val startDate: Date,
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val userId: String = ""
) : Parcelable