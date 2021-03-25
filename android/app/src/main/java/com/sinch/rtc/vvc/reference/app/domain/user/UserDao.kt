package com.sinch.rtc.vvc.reference.app.domain.user

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sinch.rtc.vvc.reference.app.application.Constants

@Dao
interface UserDao {

    companion object {
        const val LOAD_LOGGED_IN_USER_QUERY =
            "SELECT * FROM ${Constants.USERS_TABLE_NAME} where ${Constants.USERS_IS_LOGGED_IN_COLUMN_NAME} = 1"
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("SELECT * FROM ${Constants.USERS_TABLE_NAME}")
    fun loadAllUsers(): List<User>

    @Query(LOAD_LOGGED_IN_USER_QUERY)
    fun getLoggedInUserLiveData(): LiveData<User?>

    @Query(LOAD_LOGGED_IN_USER_QUERY)
    fun loadLoggedInUser(): User?

    @Delete
    fun delete(user: User)
}