package com.sinch.rtc.vvc.reference.app.domain.calls

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sinch.rtc.vvc.reference.app.application.Constants

@Dao
interface CallDao {

    companion object {
        const val LOAD_USER_HISTORY_QUERY =
            "SELECT * FROM ${Constants.CALL_HISTORY_ITEMS_TABLE_NAME} WHERE ${Constants.CALL_ITEM_USER_ID_COLUMN_NAME} = :userId"
    }

    @Insert
    fun insert(callItem: CallItem): Long

    @Update
    fun update(callItem: CallItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(callItem: List<CallItem>)

    @Query(LOAD_USER_HISTORY_QUERY)
    fun getLiveDataOfUserCallHistory(userId: String): LiveData<List<CallItem>>

    @Query(LOAD_USER_HISTORY_QUERY)
    fun loadCallHistoryOfUser(userId: String): List<CallItem>

    @Delete
    fun delete(callItems: List<CallItem>)
}

fun CallDao.insertAndGetWithGeneratedId(callItem: CallItem): CallItem {
    val newId = insert(callItem)
    return callItem.copy(itemId = newId)
}