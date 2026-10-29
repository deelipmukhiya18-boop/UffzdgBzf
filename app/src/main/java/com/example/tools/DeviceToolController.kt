package com.example.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.search.WebSearchService
import com.example.service.NotifService
import com.example.service.PhoneService

class DeviceToolController(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val telecomManager by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    }

    val webSearchService by lazy { WebSearchService() }

    fun isDangerous(toolName: String): Boolean {
        return when (toolName) {
            "call_phone", "send_sms", "reply_notification", "answer_call", "end_call", "emergency_sos" -> true
            else -> false
        }
    }

    fun formatDangerSummary(toolName: String, args: Map<String, Any>): String {
        return when (toolName) {
            "call_phone" -> "Make an outgoing call to: ${args["number"]}"
            "send_sms" -> "Send SMS to ${args["number"]}:\n\"${args["text"]}\""
            "reply_notification" -> "Send reply to notification #${args["index"]}:\n\"${args["text"]}\""
            "answer_call" -> "Answer the currently ringing call"
            "end_call" -> "Hang up / end the current active call"
            "emergency_sos" -> "Send emergency SOS SMS with GPS location to all saved SOS contacts and call the primary contact"
            else -> "Execute sensitive action: $toolName with arguments $args"
        }
    }

    suspend fun execute(
        toolName: String,
        args: Map<String, Any>,
        searchProvider: String = "tavily",
        searchApiKey: String = ""
    ): String {
        return try {
            when (toolName) {
                "call_phone" -> {
                    val number = args["number"]?.toString() ?: return "Error: Missing phone number."
                    callPhone(number)
                }
                "send_sms" -> {
                    val number = args["number"]?.toString() ?: return "Error: Missing recipient number."
                    val text = args["text"]?.toString() ?: return "Error: Missing message text."
                    sendSms(number, text)
                }
                "find_contact" -> {
                    val name = args["name"]?.toString() ?: return "Error: Missing contact name."
                    findContact(name)
                }
                "open_app" -> {
                    val appName = args["name"]?.toString() ?: return "Error: Missing app name."
                    openApp(appName)
                }
                "open_url" -> {
                    val url = args["url"]?.toString() ?: return "Error: Missing URL."
                    openUrl(url)
                }
                "set_alarm" -> {
                    val hour = (args["hour"] as? Number)?.toInt() ?: 7
                    val minute = (args["minute"] as? Number)?.toInt() ?: 0
                    val label = args["label"]?.toString() ?: "AI Assistant Alarm"
                    setAlarm(hour, minute, label)
                }
                "set_timer" -> {
                    val seconds = (args["seconds"] as? Number)?.toInt() ?: 60
                    setTimer(seconds)
                }
                "flashlight" -> {
                    val on = when (val value = args["on"]) {
                        is Boolean -> value
                        is String -> value.equals("true", ignoreCase = true) || value.equals("on", ignoreCase = true)
                        else -> true
                    }
                    toggleFlashlight(on)
                }
                "set_volume" -> {
                    val percent = (args["percent"] as? Number)?.toInt() ?: 50
                    setVolume(percent)
                }
                "battery" -> getBatteryStatus()
                "wake_screen" -> {
                    ScreenWakeHelper.wakeScreen(context)
                    "स्क्रीन वेक कर दी गई है।"
                }
                "open_settings" -> {
                    val which = args["which"]?.toString() ?: "general"
                    openSettings(which)
                }
                "read_notifications" -> NotifService.readNotifications()
                "reply_notification" -> {
                    val index = (args["index"] as? Number)?.toInt() ?: return "Error: Missing notification index."
                    val text = args["text"]?.toString() ?: return "Error: Missing reply text."
                    NotifService.replyNotification(index, text)
                }
                "answer_call" -> answerCall()
                "end_call" -> endCall()
                "read_screen" -> PhoneService.readScreen()
                "tap_text" -> {
                    val text = args["text"]?.toString() ?: return "Error: Missing text to tap."
                    PhoneService.tapText(text)
                }
                "type_text" -> {
                    val text = args["text"]?.toString() ?: return "Error: Missing text to type."
                    PhoneService.typeText(text)
                }
                "press_button" -> {
                    val button = args["button"]?.toString() ?: return "Error: Missing button name."
                    PhoneService.pressButton(button)
                }
                "scroll" -> {
                    val direction = args["direction"]?.toString() ?: "down"
                    PhoneService.scroll(direction)
                }
                "web_search" -> {
                    val query = args["query"]?.toString() ?: return "Error: Missing search query."
                    webSearchService.search(query, searchProvider, searchApiKey)
                }
                "get_location" -> getLocation()
                "emergency_sos" -> triggerEmergencySos()
                "play_music" -> {
                    val song = args["song"]?.toString() ?: args["query"]?.toString() ?: ""
                    val platform = args["platform"]?.toString() ?: "youtube"
                    playMusic(song, platform)
                }
                else -> "Unknown tool command: $toolName"
            }
        } catch (e: Exception) {
            "Execution error for $toolName: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun callPhone(number: String): String {
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPerm) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return if (hasCallPerm) "Calling $number directly..." else "Dialer opened for $number"
    }

    private fun sendSms(number: String, text: String): String {
        val hasSmsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPerm) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                putExtra("sms_body", text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "SMS composer opened for $number with message: \"$text\""
        }

        @Suppress("DEPRECATION")
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(text)
        smsManager.sendMultipartTextMessage(number, null, parts, null, null)
        return "SMS successfully sent to $number."
    }

    private fun findContact(nameQuery: String): String {
        val hasContactPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactPerm) {
            return "Cannot search contacts: READ_CONTACTS permission is not granted."
        }

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$nameQuery%"),
            null
        )

        val sb = StringBuilder()
        var count = 0
        cursor?.use {
            while (it.moveToNext() && count < 6) {
                val name = it.getString(0) ?: "Unknown"
                val number = it.getString(1) ?: "No number"
                sb.append("• $name: $number\n")
                count++
            }
        }
        return if (sb.isEmpty()) {
            "No contacts found matching '$nameQuery'."
        } else {
            "Found contacts:\n$sb".trim()
        }
    }

    private fun openApp(query: String): String {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val list: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        val cleanQuery = query.lowercase().trim()

        for (info in list) {
            val label = info.loadLabel(pm).toString().lowercase()
            val pkg = info.activityInfo.packageName.lowercase()
            if (label.contains(cleanQuery) || pkg.contains(cleanQuery)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return "Opened ${info.loadLabel(pm)}"
                }
            }
        }
        return "Application matching '$query' not found."
    }

    private fun openUrl(url: String): String {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opened link: $cleanUrl"
    }

    private fun setAlarm(hour: Int, minute: Int, label: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val formattedTime = String.format("%02d:%02d", hour, minute)
        return "Alarm scheduled for $formattedTime ($label)"
    }

    private fun setTimer(seconds: Int): String {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Countdown timer set for $seconds seconds."
    }

    private fun toggleFlashlight(on: Boolean): String {
        return try {
            val cameraIds = cameraManager.cameraIdList
            var selectedId: String? = null
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    selectedId = id
                    break
                }
            }
            val id = selectedId ?: cameraIds.firstOrNull() ?: return "No camera flash unit found."
            cameraManager.setTorchMode(id, on)
            if (on) "Flashlight turned ON 🔦" else "Flashlight turned OFF"
        } catch (e: Exception) {
            "Flashlight error: ${e.message}"
        }
    }

    private fun setVolume(percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (maxVolume * clamped) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return "Media volume set to $clamped% (Level $target/$maxVolume)"
    }

    private fun getBatteryStatus(): String {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val chargingText = if (isCharging) " (Charging ⚡)" else ""
        return "Battery level: $percentage%$chargingText"
    }

    fun openSettings(which: String): String {
        val action = when (which.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notification_access" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opened $which settings."
    }

    @Suppress("DEPRECATION")
    private fun answerCall(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tm = telecomManager ?: return "TelecomManager unavailable"
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            return if (hasPerm) {
                tm.acceptRingingCall()
                "Accepted incoming call."
            } else {
                "Permission ANSWER_PHONE_CALLS not granted."
            }
        }
        return "Answering calls programmatically requires Android 8.0+."
    }

    @Suppress("DEPRECATION")
    private fun endCall(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val tm = telecomManager ?: return "TelecomManager unavailable"
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            return if (hasPerm) {
                if (tm.endCall()) "Call ended." else "Could not end call."
            } else {
                "Permission not granted."
            }
        }
        return "Ending calls programmatically requires Android 9.0+."
    }

    fun getLocation(): String {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            ?: return "लोकेशन नहीं मिली"
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return "लोकेशन अनुमति नहीं मिली"
        }

        var best: android.location.Location? = null
        try {
            for (pv in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(pv)
                if (l != null && (best == null || l.accuracy < best.accuracy)) {
                    best = l
                }
            }
        } catch (_: SecurityException) {
            return "लोकेशन अनुमति नहीं मिली"
        }
        return if (best != null) "${best.latitude},${best.longitude}" else "लोकेशन नहीं मिली"
    }

    private fun triggerEmergencySos(): String {
        val prefs = context.getSharedPreferences("mamta_sos_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("sos_contacts", "[]") ?: "[]"
        val jsonArray = try { org.json.JSONArray(raw) } catch (e: Exception) { org.json.JSONArray() }

        if (jsonArray.length() == 0) {
            return "कोई SOS नंबर सेव नहीं है। कृपया मेन्यू (☰) > Emergency SOS में जाकर आपातकालीन नंबर जोड़ें।"
        }

        val loc = getLocation()
        val mapsLink = if (loc.matches(Regex("^-?\\d.*"))) "https://maps.google.com/?q=$loc" else "(लोकेशन उपलब्ध नहीं)"
        val sosMsg = "SOS! मुझे तुरंत मदद चाहिए। मेरी लोकेशन: $mapsLink"

        val sb = StringBuilder()
        var firstPhone: String? = null

        for (i in 0 until jsonArray.length()) {
            val contact = jsonArray.getJSONObject(i)
            val name = contact.optString("name", "Contact")
            val number = contact.optString("number", "")
            if (number.isNotBlank()) {
                if (firstPhone == null) firstPhone = number
                val res = sendSms(number, sosMsg)
                sb.append("• $name ($number): $res\n")
            }
        }

        if (firstPhone != null) {
            callPhone(firstPhone)
            sb.append("• प्राथमिक संपर्क $firstPhone पर कॉल लगाई जा रही है...")
        }

        return "🚨 Emergency SOS भेजा गया:\n${sb.toString().trim()}"
    }

    /**
     * Offline command parser to ensure instant responsiveness even when no API key is set.
     */
    fun tryHandleOfflineCommand(query: String): Pair<Boolean, String>? {
        val q = query.lowercase().trim()
        when {
            q.contains("torch on") || q.contains("flashlight on") || q.contains("टॉर्च चालू") -> {
                return Pair(false, toggleFlashlight(true))
            }
            q.contains("torch off") || q.contains("flashlight off") || q.contains("टॉर्च बंद") -> {
                return Pair(false, toggleFlashlight(false))
            }
            q.contains("wake screen") || q.contains("स्क्रीन ऑन") || q.contains("स्क्रीन खोलो") || q.contains("screen wake") -> {
                ScreenWakeHelper.wakeScreen(context)
                return Pair(false, "स्क्रीन वेक कर दी गई है।")
            }
            q.contains("battery") || q.contains("बैटरी") -> {
                return Pair(false, getBatteryStatus())
            }
            q.startsWith("volume ") || q.startsWith("वॉल्यूम ") -> {
                val num = Regex("\\d+").find(q)?.value?.toIntOrNull() ?: 50
                return Pair(false, setVolume(num))
            }
            q.contains("notifications") || q.contains("नोटिफ़िकेशन") -> {
                return Pair(false, NotifService.readNotifications())
            }
            q.contains("read screen") || q.contains("स्क्रीन") -> {
                return Pair(false, PhoneService.readScreen())
            }
            q.startsWith("open ") || q.startsWith("खोलो ") -> {
                val app = q.replace("open ", "").replace("खोलो ", "").trim()
                return Pair(false, openApp(app))
            }
            q.startsWith("alarm ") || q.contains("अलार्म") -> {
                val numbers = Regex("\\d+").findAll(q).map { it.value.toInt() }.toList()
                val hour = numbers.getOrNull(0) ?: 7
                val minute = numbers.getOrNull(1) ?: 0
                return Pair(false, setAlarm(hour, minute, "Alarm"))
            }
            q.startsWith("timer ") || q.contains("टाइमर") -> {
                val sec = Regex("\\d+").find(q)?.value?.toIntOrNull() ?: 60
                return Pair(false, setTimer(sec))
            }
            q.startsWith("play ") || q.contains(" बजाओ") || q.contains("play song") || q.contains("गाना बजाओ") -> {
                val isSpotify = q.contains("spotify")
                val isYt = q.contains("youtube") || q.contains("yt")
                var songName = q.replace("play ", "")
                    .replace("play song ", "")
                    .replace("on spotify", "")
                    .replace("on youtube music", "")
                    .replace("on youtube", "")
                    .replace("spotify पर", "")
                    .replace("youtube पर", "")
                    .replace("गाना बजाओ", "")
                    .replace("बजाओ", "")
                    .trim()
                if (songName.isBlank()) songName = "Top Hits"
                val platform = if (isSpotify) "spotify" else "youtube"
                return Pair(false, playMusic(songName, platform))
            }
            else -> return null
        }
    }

    fun playMusic(song: String, platform: String = "youtube"): String {
        val cleanSong = song.trim().ifBlank { "Top Songs" }
        val isSpotify = platform.contains("spotify", ignoreCase = true)

        if (isSpotify) {
            val appName = "Spotify"
            try {
                val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, cleanSong)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.spotify.music")
                }
                context.startActivity(intent)
                return "Playing \"$cleanSong\" on $appName"
            } catch (_: Exception) {
                try {
                    val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(cleanSong))).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(uriIntent)
                    return "Opening \"$cleanSong\" in $appName"
                } catch (_: Exception) {
                    val webUrl = "https://open.spotify.com/search/" + Uri.encode(cleanSong)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    return "Opening \"$cleanSong\" on Spotify Web"
                }
            }
        } else {
            val appName = "YouTube Music"
            try {
                val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, cleanSong)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.google.android.apps.youtube.music")
                }
                context.startActivity(intent)
                return "Playing \"$cleanSong\" on $appName"
            } catch (_: Exception) {
                try {
                    val ytMusicWeb = "https://music.youtube.com/search?q=" + Uri.encode(cleanSong)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ytMusicWeb)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    return "Opening \"$cleanSong\" on YouTube Music"
                } catch (_: Exception) {
                    val ytWeb = "https://www.youtube.com/results?search_query=" + Uri.encode(cleanSong)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ytWeb)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    return "Opening \"$cleanSong\" on YouTube"
                }
            }
        }
    }
}
