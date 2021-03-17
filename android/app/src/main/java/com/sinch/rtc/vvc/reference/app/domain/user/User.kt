package com.sinch.rtc.vvc.reference.app.domain.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sinch.rtc.vvc.reference.app.application.Constants

@Entity(tableName = Constants.USERS_TABLE_NAME)
data class User(
    @PrimaryKey val id: String,
    @ColumnInfo(name = Constants.USERS_IS_LOGGED_IN_COLUMN_NAME) val isLoggedIn: Boolean = false
)