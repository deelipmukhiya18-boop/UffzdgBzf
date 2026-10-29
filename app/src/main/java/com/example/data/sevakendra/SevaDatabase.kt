package com.example.data.sevakendra

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SevaCustomerWork::class, SevaRateItem::class],
    version = 1,
    exportSchema = false
)
abstract class SevaDatabase : RoomDatabase() {

    abstract fun sevaDao(): SevaDao

    companion object {
        @Volatile
        private var INSTANCE: SevaDatabase? = null

        fun getDatabase(context: Context): SevaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SevaDatabase::class.java,
                    "mamta_sevakendra.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
