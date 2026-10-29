package com.example.data.sevakendra

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Seva Kendra Customer Work Order Entity.
 * NOTE: Stored ONLY locally in Room database.
 * Never store Aadhaar, PAN, Bank account or passwords here.
 */
@Entity(tableName = "seva_works")
data class SevaCustomerWork(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "service_name")
    val serviceName: String,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "paid_amount")
    val paidAmount: Double,
    @ColumnInfo(name = "status")
    val status: String = STATUS_PENDING,
    @ColumnInfo(name = "due_date")
    val dueDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "notes")
    val notes: String = ""
) {
    val balanceDue: Double
        get() = (amount - paidAmount).coerceAtLeast(0.0)

    val isPending: Boolean
        get() = status != STATUS_DONE && status != STATUS_DELIVERED

    companion object {
        const val STATUS_PENDING = "Pending"
        const val STATUS_IN_PROGRESS = "In progress"
        const val STATUS_DONE = "Done"
        const val STATUS_DELIVERED = "Delivered"

        val ALL_STATUSES = listOf(STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_DONE, STATUS_DELIVERED)
    }
}
