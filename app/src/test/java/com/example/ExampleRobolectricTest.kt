package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.FrontCameraCapturer
import com.example.service.TouchGuardService
import com.example.service.TouchGuardStatus
import com.example.tools.DeviceToolController
import com.example.tools.ScreenWakeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mamta", appName)
    }

    @Test
    fun `verify sensitive tools require safety confirmation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = DeviceToolController(context)

        assertTrue(controller.isDangerous("call_phone"))
        assertTrue(controller.isDangerous("send_sms"))
        assertTrue(controller.isDangerous("reply_notification"))
        assertTrue(controller.isDangerous("answer_call"))
        assertTrue(controller.isDangerous("end_call"))

        assertFalse(controller.isDangerous("flashlight"))
        assertFalse(controller.isDangerous("battery"))
        assertFalse(controller.isDangerous("open_app"))
        assertFalse(controller.isDangerous("web_search"))
        assertFalse(controller.isDangerous("wake_screen"))
    }

    @Test
    fun `verify screen lock wake helper is on by default and persists correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 1. By default, "Wake the screen when Mamta needs it" is ON
        assertTrue(ScreenWakeHelper.isWakeScreenEnabled(context))

        // 2. Can be toggled off and on
        ScreenWakeHelper.setWakeScreenEnabled(context, false)
        assertFalse(ScreenWakeHelper.isWakeScreenEnabled(context))

        ScreenWakeHelper.setWakeScreenEnabled(context, true)
        assertTrue(ScreenWakeHelper.isWakeScreenEnabled(context))
    }

    @Test
    fun `verify intrusion photos directory is strictly in app private storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = FrontCameraCapturer.getPhotosDir(context)

        // Must reside within context.filesDir (internal private storage)
        assertTrue("Intrusion photos dir must be within filesDir", dir.absolutePath.startsWith(context.filesDir.absolutePath))
        assertEquals(context.filesDir.resolve("intrusion_photos").absolutePath, dir.absolutePath)
    }

    @Test
    fun `verify touch guard initial state is disarmed`() {
        // Touch guard must be OFF by default
        assertFalse("Touch guard service must not be running initially", TouchGuardService.isRunning())
        assertEquals(TouchGuardStatus.Disarmed, TouchGuardService.status.value)
    }

    @Test
    fun `verify secure prefs stores and retrieves openai api key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securePrefs = com.example.data.SecurePrefs.getEncryptedPreferences(context)

        val testKey = "sk-proj-test123456789"
        securePrefs.edit().putString("cfg_openai_key", testKey).apply()

        val retrievedKey = securePrefs.getString("cfg_openai_key", "")
        assertEquals(testKey, retrievedKey)
    }

    @Test
    fun `verify empty openai api key returns informative error`() = kotlinx.coroutines.runBlocking {
        val client = com.example.network.AiClient()
        val response = client.sendPrompt(
            provider = "openai",
            apiKey = "",
            modelName = "gpt-4o-mini",
            history = emptyList(),
            language = "en"
        )

        assertTrue(response is com.example.network.AiResponse.Error)
        val error = response as com.example.network.AiResponse.Error
        assertTrue("Error should mention OpenAI API key", error.message.contains("OpenAI API key"))
    }

    @Test
    fun `verify journal database add list and delete operations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbHelper = com.example.data.journal.JournalDatabaseHelper(context)

        val entry = dbHelper.addEntry("My Day", "Today was productive", "😊")
        assertTrue(entry.id > 0)
        assertEquals("My Day", entry.title)
        assertEquals("Today was productive", entry.content)
        assertEquals("😊", entry.mood)

        val all = dbHelper.getAllEntries()
        assertTrue(all.any { it.id == entry.id })

        val deleted = dbHelper.deleteEntry(entry.id)
        assertTrue(deleted)

        val allAfter = dbHelper.getAllEntries()
        assertFalse(allAfter.any { it.id == entry.id })
    }

    @Test
    fun `verify music tool safety classification and command handling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = DeviceToolController(context)

        // play_music should NOT be considered dangerous
        assertFalse(controller.isDangerous("play_music"))

        // offline command parsing for music
        val ytResult = controller.tryHandleOfflineCommand("play kesariya on youtube music")
        assertTrue(ytResult != null)
        assertTrue(ytResult!!.second.contains("Kesariya", ignoreCase = true) || ytResult.second.contains("YouTube Music", ignoreCase = true))

        val spotResult = controller.tryHandleOfflineCommand("play tum hi ho on spotify")
        assertTrue(spotResult != null)
        assertTrue(spotResult!!.second.contains("Tum Hi Ho", ignoreCase = true) || spotResult.second.contains("Spotify", ignoreCase = true))
    }

    @Test
    fun `verify seva kendra customer work room database operations`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.sevakendra.SevaRepository(context)

        val work = com.example.data.sevakendra.SevaCustomerWork(
            customerName = "Ramesh Kumar",
            phoneNumber = "9876543210",
            serviceName = "Voter ID Correction",
            amount = 200.0,
            paidAmount = 100.0,
            status = com.example.data.sevakendra.SevaCustomerWork.STATUS_IN_PROGRESS,
            notes = "Photo submitted"
        )

        val id = repo.insertWork(work)
        assertTrue(id > 0)
        assertEquals(100.0, work.balanceDue, 0.01)

        val pendingList = repo.getWorksWithPendingBalance()
        assertTrue(pendingList.any { it.customerName == "Ramesh Kumar" && it.balanceDue == 100.0 })

        // Delete work
        repo.deleteWorkById(id)
        val pendingAfter = repo.getWorksWithPendingBalance()
        assertFalse(pendingAfter.any { it.id == id })
    }

    @Test
    fun `verify seva kendra rate list defaults and operations`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.sevakendra.SevaRepository(context)

        repo.ensureDefaultRates()
        val defaultRates = repo.allRates
        // Insert custom rate
        val custom = com.example.data.sevakendra.SevaRateItem(
            serviceName = "PVC Card Lamination",
            price = 40.0
        )
        val id = repo.insertRate(custom)
        assertTrue(id > 0)
    }

    @Test
    fun `verify seva kendra receipt generation and backup export restore`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.sevakendra.SevaRepository(context)

        repo.setShopName("Shree Ganesh Seva Kendra")
        repo.setShopPhone("9812345678")
        assertEquals("Shree Ganesh Seva Kendra", repo.getShopName())

        val testWork = com.example.data.sevakendra.SevaCustomerWork(
            id = 101,
            customerName = "Anita Sharma",
            phoneNumber = "9876500000",
            serviceName = "PAN Card Apply",
            amount = 150.0,
            paidAmount = 150.0,
            status = com.example.data.sevakendra.SevaCustomerWork.STATUS_DONE
        )

        // Receipt text verification
        val receiptText = repo.generateReceiptText(testWork)
        assertTrue(receiptText.contains("Shree Ganesh Seva Kendra"))
        assertTrue(receiptText.contains("Anita Sharma"))
        assertTrue(receiptText.contains("PAN Card Apply"))
        assertTrue(receiptText.contains("150.00"))

        // Receipt image file creation verification
        val imageFile = repo.generateReceiptImage(testWork)
        org.junit.Assert.assertNotNull(imageFile)
        assertTrue(imageFile!!.exists() && imageFile.length() > 0)

        // Backup export & restore verification
        repo.insertWork(testWork)
        val backupJson = repo.exportBackupJson()
        assertTrue(backupJson.contains("Shree Ganesh Seva Kendra"))
        assertTrue(backupJson.contains("Anita Sharma"))

        val importSuccess = repo.importBackupJson(backupJson)
        assertTrue(importSuccess)
    }

    @Test
    fun `verify image storage helper base64 decode and save to cache`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 1x1 transparent png in Base64
        val dummyPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        
        val bitmap = com.example.tools.ImageStorageHelper.decodeBase64ToBitmap(dummyPngBase64)
        org.junit.Assert.assertNotNull("Bitmap should be decoded from base64", bitmap)
        assertEquals(1, bitmap!!.width)
        assertEquals(1, bitmap.height)

        val cachedFile = com.example.tools.ImageStorageHelper.saveToPrivateCache(context, bitmap!!)
        org.junit.Assert.assertNotNull("Cached file should be created", cachedFile)
        assertTrue("Cached file must exist", cachedFile.exists())
        assertTrue("Cached file must have content", cachedFile.length() > 0)
    }

    @Test
    fun `verify ai client returns real error when image generation api key is empty`() = kotlinx.coroutines.runBlocking {
        val client = com.example.network.AiClient()
        val openaiResult = client.generateImage(
            provider = "openai",
            apiKey = "",
            modelName = "dall-e-3",
            prompt = "A futuristic city in watercolor",
            style = "Poster"
        )
        org.junit.Assert.assertNotNull(openaiResult.error)
        assertTrue(openaiResult.error!!.contains("OpenAI API key"))

        val geminiResult = client.generateImage(
            provider = "gemini",
            apiKey = "",
            modelName = "imagen-3.0-generate-002",
            prompt = "A futuristic city in watercolor",
            style = "Poster"
        )
        org.junit.Assert.assertNotNull(geminiResult.error)
        assertTrue(geminiResult.error!!.contains("Gemini API key"))
    }

    @Test
    fun `verify image generate tool is declared in AI client tools`() {
        val client = com.example.network.AiClient()
        val toolsMethod = client.javaClass.getDeclaredMethod("getOpenAiTools")
        toolsMethod.isAccessible = true
        val tools = toolsMethod.invoke(client) as org.json.JSONArray
        var foundImageTool = false
        for (i in 0 until tools.length()) {
            val obj = tools.getJSONObject(i)
            val function = obj.optJSONObject("function")
            if (function?.optString("name") == "image_generate") {
                foundImageTool = true
                val params = function.optJSONObject("parameters")
                val required = params?.optJSONArray("required")
                assertTrue(required?.toString()?.contains("prompt") == true)
                break
            }
        }
        assertTrue("image_generate tool declaration must be present in AI client tools", foundImageTool)
    }
}
