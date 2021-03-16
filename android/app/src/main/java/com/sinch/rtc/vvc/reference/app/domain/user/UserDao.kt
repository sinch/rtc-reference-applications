package com.sinch.rtc.vvc.reference.app.domain.user

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(user: User)

    @Query("SELECT * FROM user")
    fun loadAllUsers(): List<User>

    @Delete
    fun delete(user: User)
}