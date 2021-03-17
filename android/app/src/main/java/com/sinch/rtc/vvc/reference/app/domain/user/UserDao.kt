package com.sinch.rtc.vvc.reference.app.domain.user

import androidx.room.*
import com.sinch.rtc.vvc.reference.app.application.Constants

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("SELECT * FROM ${Constants.USERS_TABLE_NAME}")
    fun loadAllUsers(): List<User>

    @Query("SELECT * FROM ${Constants.USERS_TABLE_NAME} where ${Constants.USERS_IS_LOGGED_IN_COLUMN_NAME} = 1")
    fun loadLoggedInUser(): User?

    @Delete
    fun delete(user: User)
}