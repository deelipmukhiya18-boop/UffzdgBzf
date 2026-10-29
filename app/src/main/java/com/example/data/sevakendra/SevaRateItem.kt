package com.example.data.sevakendra

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Service Rate Item Entity for Seva Kendra rate list.
 */
@Entity(tableName = "seva_rates")
data class SevaRateItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "service_name")
    val serviceName: String,
    @ColumnInfo(name = "price")
    val price: Double
)
