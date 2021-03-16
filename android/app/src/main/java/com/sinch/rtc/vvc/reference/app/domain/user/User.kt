package com.sinch.rtc.vvc.reference.app.domain.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey val id: String,
    val isLoggedIn: Boolean = false
)