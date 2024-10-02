package com.sinch.rtc.vvc.reference.app.domain.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sinch.android.rtc.video.VideoScalingType
import com.sinch.rtc.vvc.reference.app.application.Constants

@Entity(tableName = Constants.USERS_TABLE_NAME)
data class User(
    @PrimaryKey @ColumnInfo(name = Constants.USERS_ID_COLUMN_NAME) val id: String,
    @ColumnInfo(name = Constants.USERS_IS_LOGGED_IN_COLUMN_NAME) val isLoggedIn: Boolean = false,
    @ColumnInfo(name = Constants.USERS_LOCAL_SCALING_VIDEO_COLUMN_NAME)
    val localScalingType: VideoScalingType = VideoScalingType.ASPECT_FIT,
    @ColumnInfo(name = Constants.USERS_REMOTE_SCALING_VIDEO_COLUMN_NAME)
    val remoteScalingType: VideoScalingType = VideoScalingType.ASPECT_FILL
)