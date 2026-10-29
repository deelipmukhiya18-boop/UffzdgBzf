package com.example.data.sevakendra

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SevaRepository(private val context: Context) {

    private val db = SevaDatabase.getDatabase(context)
    private val dao = db.sevaDao()
    private val prefs = context.getSharedPreferences("mamta_seva_kendra_prefs", Context.MODE_PRIVATE)

    val allWorks: Flow<List<SevaCustomerWork>> = dao.getAllWorks()
    val allRates: Flow<List<SevaRateItem>> = dao.getAllRates()

    fun searchWorks(query: String): Flow<List<SevaCustomerWork>> {
        return if (query.isBlank()) {
            dao.getAllWorks()
        } else {
            dao.searchWorks(query.trim())
        }
    }

    suspend fun insertWork(work: SevaCustomerWork): Long = withContext(Dispatchers.IO) {
        dao.insertWork(work)
    }

    suspend fun updateWork(work: SevaCustomerWork) = withContext(Dispatchers.IO) {
        dao.updateWork(work)
    }

    suspend fun deleteWork(work: SevaCustomerWork) = withContext(Dispatchers.IO) {
        dao.deleteWork(work)
    }

    suspend fun deleteWorkById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteWorkById(id)
    }

    suspend fun getWorksWithPendingBalance(): List<SevaCustomerWork> = withContext(Dispatchers.IO) {
        dao.getWorksWithPendingBalance()
    }

    suspend fun insertRate(rate: SevaRateItem): Long = withContext(Dispatchers.IO) {
        dao.insertRate(rate)
    }

    suspend fun updateRate(rate: SevaRateItem) = withContext(Dispatchers.IO) {
        dao.updateRate(rate)
    }

    suspend fun deleteRate(rate: SevaRateItem) = withContext(Dispatchers.IO) {
        dao.deleteRate(rate)
    }

    // Default rate population
    suspend fun ensureDefaultRates() = withContext(Dispatchers.IO) {
        val existing = dao.getAllRatesDirect()
        if (existing.isEmpty()) {
            val defaults = listOf(
                SevaRateItem(serviceName = "Aadhaar Print / PVC Card", price = 50.0),
                SevaRateItem(serviceName = "PAN Card Online Apply / Correction", price = 150.0),
                SevaRateItem(serviceName = "Voter ID New / Correction", price = 100.0),
                SevaRateItem(serviceName = "Income / Caste / Domicile Certificate", price = 120.0),
                SevaRateItem(serviceName = "Ration Card Apply", price = 100.0),
                SevaRateItem(serviceName = "Driving Licence Apply", price = 250.0),
                SevaRateItem(serviceName = "Online Job / Exam Form Apply", price = 100.0),
                SevaRateItem(serviceName = "Passport Online Apply", price = 300.0),
                SevaRateItem(serviceName = "Electricity / Water Bill Payment", price = 20.0),
                SevaRateItem(serviceName = "Printout / Xerox (B&W)", price = 10.0),
                SevaRateItem(serviceName = "Lamination", price = 20.0)
            )
            dao.insertRates(defaults)
        }
    }

    // Shop Settings
    fun getShopName(): String = prefs.getString("shop_name", "डिजिटल सेवा केंद्र / Digital Seva Kendra") ?: "Digital Seva Kendra"
    fun setShopName(name: String) = prefs.edit().putString("shop_name", name.trim()).apply()

    fun getShopPhone(): String = prefs.getString("shop_phone", "") ?: ""
    fun setShopPhone(phone: String) = prefs.edit().putString("shop_phone", phone.trim()).apply()

    fun getShopTagline(): String = prefs.getString("shop_tagline", "Online Forms & Certificates") ?: "Online Forms & Certificates"
    fun setShopTagline(tagline: String) = prefs.edit().putString("shop_tagline", tagline.trim()).apply()

    // Backup & Restore
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exported_at", System.currentTimeMillis())

        val shop = JSONObject().apply {
            put("name", getShopName())
            put("phone", getShopPhone())
            put("tagline", getShopTagline())
        }
        root.put("shop", shop)

        val works = dao.getAllWorksDirect()
        val worksArr = JSONArray()
        for (w in works) {
            val obj = JSONObject().apply {
                put("id", w.id)
                put("customer_name", w.customerName)
                put("phone_number", w.phoneNumber)
                put("service_name", w.serviceName)
                put("amount", w.amount)
                put("paid_amount", w.paidAmount)
                put("status", w.status)
                put("due_date", w.dueDate)
                put("created_at", w.createdAt)
                put("notes", w.notes)
            }
            worksArr.put(obj)
        }
        root.put("works", worksArr)

        val rates = dao.getAllRatesDirect()
        val ratesArr = JSONArray()
        for (r in rates) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("service_name", r.serviceName)
                put("price", r.price)
            }
            ratesArr.put(obj)
        }
        root.put("rates", ratesArr)

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("shop")) {
                val shop = root.getJSONObject("shop")
                if (shop.has("name")) setShopName(shop.getString("name"))
                if (shop.has("phone")) setShopPhone(shop.getString("phone"))
                if (shop.has("tagline")) setShopTagline(shop.getString("tagline"))
            }

            if (root.has("works")) {
                val worksArr = root.getJSONArray("works")
                dao.deleteAllWorks()
                for (i in 0 until worksArr.length()) {
                    val obj = worksArr.getJSONObject(i)
                    val work = SevaCustomerWork(
                        id = obj.optLong("id", 0),
                        customerName = obj.optString("customer_name", ""),
                        phoneNumber = obj.optString("phone_number", ""),
                        serviceName = obj.optString("service_name", ""),
                        amount = obj.optDouble("amount", 0.0),
                        paidAmount = obj.optDouble("paid_amount", 0.0),
                        status = obj.optString("status", SevaCustomerWork.STATUS_PENDING),
                        dueDate = obj.optLong("due_date", System.currentTimeMillis()),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        notes = obj.optString("notes", "")
                    )
                    dao.insertWork(work)
                }
            }

            if (root.has("rates")) {
                val ratesArr = root.getJSONArray("rates")
                dao.deleteAllRates()
                for (i in 0 until ratesArr.length()) {
                    val obj = ratesArr.getJSONObject(i)
                    val rate = SevaRateItem(
                        id = obj.optLong("id", 0),
                        serviceName = obj.optString("service_name", ""),
                        price = obj.optDouble("price", 0.0)
                    )
                    dao.insertRate(rate)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Reminders
    suspend fun checkAndNotifyDueToday(): Int = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        val pending = dao.getPendingDueToday(startOfDay, endOfDay)
        if (pending.isNotEmpty()) {
            sendReminderNotification(pending)
        }
        pending.size
    }

    private fun sendReminderNotification(dueWorks: List<SevaCustomerWork>) {
        val channelId = "seva_kendra_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Seva Kendra Due Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for Seva Kendra works due today"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Seva Kendra: ${dueWorks.size} Work Due Today"
        val names = dueWorks.take(3).joinToString(", ") { "${it.customerName} (${it.serviceName})" }
        val body = if (dueWorks.size > 3) "$names and ${dueWorks.size - 3} more" else names

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Due today: $body"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(7891, notification)
    }

    // Receipt Text Generation
    fun generateReceiptText(work: SevaCustomerWork): String {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(work.createdAt))
        val shopName = getShopName()
        val shopPhone = getShopPhone()

        val sb = StringBuilder()
        sb.appendLine("===============================")
        sb.appendLine("    $shopName")
        if (shopPhone.isNotBlank()) sb.appendLine("    Phone: $shopPhone")
        sb.appendLine("===============================")
        sb.appendLine("RECEIPT #SK-${work.id}")
        sb.appendLine("Date: $dateStr")
        sb.appendLine("-------------------------------")
        sb.appendLine("Customer: ${work.customerName}")
        if (work.phoneNumber.isNotBlank()) sb.appendLine("Phone: ${work.phoneNumber}")
        sb.appendLine("Service: ${work.serviceName}")
        sb.appendLine("Status: ${work.status}")
        sb.appendLine("-------------------------------")
        sb.appendLine("Total Amount: ₹${"%.2f".format(work.amount)}")
        sb.appendLine("Amount Paid:  ₹${"%.2f".format(work.paidAmount)}")
        sb.appendLine("Balance Due:  ₹${"%.2f".format(work.balanceDue)}")
        sb.appendLine("===============================")
        sb.appendLine("Thank you for choosing $shopName!")
        sb.appendLine("===============================")
        return sb.toString()
    }

    // Receipt Image Generation
    fun generateReceiptImage(work: SevaCustomerWork): File? {
        return try {
            val width = 720
            val height = 960
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            canvas.drawColor(Color.WHITE)

            // Border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 24f, 24f, borderPaint)

            // Header Banner
            val bannerPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(16f, 16f, width - 16f, 160f), 24f, 24f, bannerPaint)
            canvas.drawRect(RectF(16f, 130f, width - 16f, 160f), bannerPaint) // square bottom corners of header

            // Shop Name
            val shopNamePaint = Paint().apply {
                color = Color.WHITE
                textSize = 34f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(getShopName().take(30), width / 2f, 80f, shopNamePaint)

            // Shop Phone & Tagline
            val subPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 22f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val contactInfo = if (getShopPhone().isNotBlank()) "Ph: ${getShopPhone()} • ${getShopTagline()}" else getShopTagline()
            canvas.drawText(contactInfo.take(45), width / 2f, 120f, subPaint)

            // Receipt Title
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0284C7")
                textSize = 26f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("CUSTOMER PAYMENT RECEIPT", width / 2f, 210f, titlePaint)

            // Divider
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 2f
            }
            canvas.drawLine(40f, 235f, width - 40f, 235f, linePaint)

            // Details Left Labels and Right Values
            val labelPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 24f
                isAntiAlias = true
            }
            val valPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateStr = dateFormat.format(Date(work.createdAt))

            var y = 280f
            fun drawRow(label: String, value: String, isHighlight: Boolean = false) {
                if (isHighlight) {
                    val highlightPaint = Paint().apply {
                        color = Color.parseColor("#F8FAFC")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(RectF(30f, y - 30f, width - 30f, y + 14f), 10f, 10f, highlightPaint)
                }
                canvas.drawText(label, 45f, y, labelPaint)
                canvas.drawText(value, width - 45f, y, valPaint)
                y += 50f
            }

            drawRow("Receipt ID:", "#SK-${work.id}")
            drawRow("Date & Time:", dateStr)
            drawRow("Customer Name:", work.customerName)
            if (work.phoneNumber.isNotBlank()) {
                drawRow("Phone Number:", work.phoneNumber)
            }
            drawRow("Service:", work.serviceName.take(30))
            drawRow("Status:", work.status)

            canvas.drawLine(40f, y, width - 40f, y, linePaint)
            y += 40f

            // Financial Summary Box
            val amtBoxPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(40f, y - 20f, width - 40f, y + 170f), 16f, 16f, amtBoxPaint)

            drawRow("Total Amount:", "₹ ${"%.2f".format(work.amount)}")
            drawRow("Amount Paid:", "₹ ${"%.2f".format(work.paidAmount)}")

            val balancePaint = Paint().apply {
                color = if (work.balanceDue > 0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
                textSize = 28f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val balanceLabelPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 26f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("Balance Due:", 45f, y, balanceLabelPaint)
            canvas.drawText("₹ ${"%.2f".format(work.balanceDue)}", width - 45f, y, balancePaint)

            y += 80f

            // Footer Thank You Note
            val footerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 20f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Thank you for your visit!", width / 2f, height - 70f, footerPaint)
            canvas.drawText("Mamta Digital Assistant • Seva Kendra", width / 2f, height - 40f, footerPaint)

            // Save to cache dir
            val dir = File(context.cacheDir, "receipts")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "receipt_sk_${work.id}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    // Share Receipt
    fun shareReceipt(work: SevaCustomerWork) {
        val receiptText = generateReceiptText(work)
        val imageFile = generateReceiptImage(work)

        val intent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (imageFile != null && imageFile.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, receiptText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
            }
        }
        val chooser = Intent.createChooser(intent, "Share Seva Kendra Receipt").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }
}
