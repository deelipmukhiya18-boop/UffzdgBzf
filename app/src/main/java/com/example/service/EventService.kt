package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class EventService : Service() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var prefs: SharedPreferences

    private var lowDone = false
    private var critDone = false
    private var fullDone = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { handleIntent(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        prefs = getSharedPreferences("mamta_events", MODE_PRIVATE)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("hi-IN")
                ttsReady = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mamta Voice Announcements",
                NotificationManager.IMPORTANCE_LOW
            )
            nm?.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("Mamta चालू है")
            .setContentText("फ़ोन की घटनाएँ सुन रही हूँ")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("EventService", "Could not start foreground service", e)
        }

        // Register hardware event filters
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(receiver, filter)

        // Register package event filters
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(receiver, packageFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) {}
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun getAppLabel(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun say(key: String, text: String) {
        val def = !(key == "saver_on" || key == "saver_off")
        if (!prefs.getBoolean("master", true)) return
        if (!prefs.getBoolean(key, def)) return

        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            if (am.mode != AudioManager.MODE_NORMAL) return
            if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL && !prefs.getBoolean("speak_silent", false)) return
        }

        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "event_$key")
        }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            Intent.ACTION_POWER_CONNECTED -> say("power_in", "चार्जर लग गया")
            Intent.ACTION_POWER_DISCONNECTED -> say("power_out", "चार्जर निकल गया")
            Intent.ACTION_BATTERY_CHANGED -> {
                val lv = intent.getIntExtra("level", -1)
                val sc = intent.getIntExtra("scale", 100)
                if (lv < 0 || sc <= 0) return
                val pct = lv * 100 / sc
                val plugged = intent.getIntExtra("plugged", 0)

                if (plugged == 0) {
                    fullDone = false
                    if (pct <= 20 && !lowDone) {
                        lowDone = true
                        say("low", "बैटरी $pct प्रतिशत रह गई है")
                    }
                    if (pct <= 10 && !critDone) {
                        critDone = true
                        say("crit", "बैटरी सिर्फ़ $pct प्रतिशत बची है, चार्ज कर लीजिए")
                    }
                } else {
                    lowDone = false
                    critDone = false
                    if (pct >= 100 && !fullDone) {
                        fullDone = true
                        say("full", "बैटरी फुल हो गई है")
                    }
                }
            }
            Intent.ACTION_HEADSET_PLUG -> {
                if (intent.getIntExtra("state", 0) == 1) {
                    say("hp_in", "हेडफ़ोन लग गए")
                } else {
                    say("hp_out", "हेडफ़ोन निकल गए")
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> say("bt_in", "ब्लूटूथ डिवाइस जुड़ गया")
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> say("bt_out", "ब्लूटूथ डिवाइस हट गया")
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                if (intent.getBooleanExtra("state", false)) {
                    say("air_on", "एयरप्लेन मोड चालू हुआ")
                } else {
                    say("air_off", "एयरप्लेन मोड बंद हुआ")
                }
            }
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm?.isPowerSaveMode == true) {
                    say("saver_on", "बैटरी सेवर चालू हुआ")
                } else {
                    say("saver_off", "बैटरी सेवर बंद हुआ")
                }
            }
            Intent.ACTION_PACKAGE_ADDED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    val pkg = intent.data?.schemeSpecificPart
                    if (!pkg.isNullOrBlank()) {
                        say("app_in", "नया ऐप इंस्टॉल हुआ: ${getAppLabel(pkg)}")
                    }
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    val pkg = intent.data?.schemeSpecificPart
                    if (!pkg.isNullOrBlank()) {
                        say("app_out", "ऐप हटा दिया गया: ${getAppLabel(pkg)}")
                    }
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "mamta_events_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning = false
    }
}
