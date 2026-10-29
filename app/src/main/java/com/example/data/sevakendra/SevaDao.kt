package com.example.data.sevakendra

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SevaDao {

    // Works
    @Query("SELECT * FROM seva_works ORDER BY created_at DESC")
    fun getAllWorks(): Flow<List<SevaCustomerWork>>

    @Query("""
        SELECT * FROM seva_works 
        WHERE customer_name LIKE '%' || :query || '%' 
           OR phone_number LIKE '%' || :query || '%' 
           OR service_name LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchWorks(query: String): Flow<List<SevaCustomerWork>>

    @Query("SELECT * FROM seva_works WHERE id = :id LIMIT 1")
    suspend fun getWorkById(id: Long): SevaCustomerWork?

    @Query("""
        SELECT * FROM seva_works 
        WHERE due_date >= :startOfDay AND due_date <= :endOfDay 
          AND status NOT IN ('Done', 'Delivered')
        ORDER BY due_date ASC
    """)
    suspend fun getPendingDueToday(startOfDay: Long, endOfDay: Long): List<SevaCustomerWork>

    @Query("""
        SELECT * FROM seva_works 
        WHERE (amount - paid_amount) > 0
        ORDER BY created_at DESC
    """)
    suspend fun getWorksWithPendingBalance(): List<SevaCustomerWork>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: SevaCustomerWork): Long

    @Update
    suspend fun updateWork(work: SevaCustomerWork)

    @Delete
    suspend fun deleteWork(work: SevaCustomerWork)

    @Query("DELETE FROM seva_works WHERE id = :id")
    suspend fun deleteWorkById(id: Long)

    @Query("SELECT * FROM seva_works ORDER BY created_at DESC")
    suspend fun getAllWorksDirect(): List<SevaCustomerWork>

    @Query("DELETE FROM seva_works")
    suspend fun deleteAllWorks()

    // Rates
    @Query("SELECT * FROM seva_rates ORDER BY service_name ASC")
    fun getAllRates(): Flow<List<SevaRateItem>>

    @Query("SELECT * FROM seva_rates ORDER BY service_name ASC")
    suspend fun getAllRatesDirect(): List<SevaRateItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: SevaRateItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<SevaRateItem>)

    @Update
    suspend fun updateRate(rate: SevaRateItem)

    @Delete
    suspend fun deleteRate(rate: SevaRateItem)

    @Query("DELETE FROM seva_rates")
    suspend fun deleteAllRates()
}
