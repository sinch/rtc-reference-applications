package com.sinch.rtc.vvc.reference.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sinch.rtc.vvc.reference.app.domain.calls.CallDao
import com.sinch.rtc.vvc.reference.app.domain.calls.CallItem
import com.sinch.rtc.vvc.reference.app.domain.user.User
import com.sinch.rtc.vvc.reference.app.domain.user.UserDao
import com.sinch.rtc.vvc.reference.app.storage.converters.CallTypeConverter
import com.sinch.rtc.vvc.reference.app.storage.converters.DateConverter
import com.sinch.rtc.vvc.reference.app.storage.converters.VideoScalingTypeConverter


@Database(entities = [User::class, CallItem::class], version = 1, exportSchema = false)
@TypeConverters(CallTypeConverter::class, DateConverter::class, VideoScalingTypeConverter::class)
abstract class RTCVoiceVideoAppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun callDao(): CallDao

    companion object {

        private const val DATABASE_NAME = "RTCVoiceVideoAppDatabase"

        private var INSTANCE: RTCVoiceVideoAppDatabase? = null

        fun getDatabase(context: Context): RTCVoiceVideoAppDatabase {
            if (INSTANCE == null) {
                synchronized(RTCVoiceVideoAppDatabase::class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            RTCVoiceVideoAppDatabase::class.java,
                            DATABASE_NAME
                        )
                            .allowMainThreadQueries() //Since this is a reference app with relatively small database for simplicity we allow queries on main thread
                            .fallbackToDestructiveMigration()
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }
    }

}
