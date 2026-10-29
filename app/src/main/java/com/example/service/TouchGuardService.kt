package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.tools.ScreenWakeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

sealed class TouchGuardStatus {
    object Disarmed : TouchGuardStatus()
    data class Arming(val secondsLeft: Int) : TouchGuardStatus()
    object Armed : TouchGuardStatus()
    data class Triggered(val reason: String, val photoCaptured: Boolean = false) : TouchGuardStatus()
}

class TouchGuardService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "TouchGuardService"
        const val CHANNEL_ID = "mamta_touch_guard_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_ARM = "com.example.touchguard.ACTION_START_ARM"
        const val ACTION_DISARM = "com.example.touchguard.ACTION_DISARM"

        private val _status = MutableStateFlow<TouchGuardStatus>(TouchGuardStatus.Disarmed)
        val status: StateFlow<TouchGuardStatus> = _status.asStateFlow()

        fun isRunning(): Boolean {
            return _status.value !is TouchGuardStatus.Disarmed
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null
    private var sirenJob: Job? = null
    private var ttsRepeatJob: Job? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var audioManager: AudioManager

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var lastAccX: Float? = null
    private var lastAccY: Float? = null
    private var lastAccZ: Float? = null
    private var isArmed = false
    private var isTriggered = false

    private val hardwareEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isArmed || isTriggered) return
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    triggerIntrusion("Screen turned on / स्क्रीन ऑन हुई")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    triggerIntrusion("Charger pulled out / चार्जर हटाया गया")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        initTts()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Touch Guard Anti-Theft",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Monitors device for unauthorized movement, screen unlock, or charger removal"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.forLanguageTag("hi-IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.language = Locale.ENGLISH
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val audioAttrs = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts.setAudioAttributes(audioAttrs)
                    }
                    isTtsReady = true
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ARM

        if (action == ACTION_DISARM) {
            disarm()
            return START_NOT_STICKY
        }

        // Start Foreground Service with Camera type for Android 14+
        val initialNotif = buildNotification(
            title = "Touch Guard Arming...",
            content = "10 seconds delay. Place your phone down.",
            isTriggered = false
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotif)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground with CAMERA type", e)
        }

        startArmingCountdown()
        return START_STICKY
    }

    private fun startArmingCountdown() {
        isArmed = false
        isTriggered = false
        countdownJob?.cancel()

        countdownJob = serviceScope.launch {
            for (sec in 10 downTo 1) {
                _status.value = TouchGuardStatus.Arming(sec)
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                        title = "🛡️ Touch Guard Arming ($sec s)",
                        content = "Put the phone down. Arming in $sec seconds...",
                        isTriggered = false
                    )
                )
                delay(1000)
            }

            // Arming complete
            isArmed = true
            _status.value = TouchGuardStatus.Armed
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(
                    title = "🛡️ Touch Guard Armed & Guarding",
                    content = "Protected: Movement, Screen-on & Charger disconnect monitored.",
                    isTriggered = false
                )
            )

            // Register screen on & charger disconnect receivers
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            registerReceiver(hardwareEventReceiver, filter)

            // Register accelerometer
            accelerometer?.let {
                sensorManager.registerListener(
                    this@TouchGuardService,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

            // Speak confirmation
            if (isTtsReady) {
                textToSpeech?.speak("टच गार्ड सक्रिय है", TextToSpeech.QUEUE_FLUSH, null, "arm_ready")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isArmed || isTriggered || event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val lastX = lastAccX
            val lastY = lastAccY
            val lastZ = lastAccZ

            lastAccX = x
            lastAccY = y
            lastAccZ = z

            if (lastX == null || lastY == null || lastZ == null) return

            val deltaX = abs(x - lastX)
            val deltaY = abs(y - lastY)
            val deltaZ = abs(z - lastZ)
            val totalDelta = deltaX + deltaY + deltaZ

            val currentMag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val gravityDelta = abs(currentMag - 9.8f)

            // Motion threshold: phone was picked up or moved noticeably
            if (totalDelta > 3.2f || gravityDelta > 3.0f) {
                triggerIntrusion("Phone picked up / फ़ोन उठाया गया")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerIntrusion(reason: String) {
        if (isTriggered) return
        isTriggered = true
        isArmed = false
        _status.value = TouchGuardStatus.Triggered(reason, photoCaptured = false)
        Log.w(TAG, "Touch Guard intrusion triggered: $reason")

        // Wake screen immediately
        ScreenWakeHelper.wakeScreen(this)

        // Update notification to alert
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = "🚨 INTRUSION DETECTED!",
                content = reason,
                isTriggered = true
            )
        )

        // 1. Capture photo with front camera silently and save to private storage
        try {
            FrontCameraCapturer(this).capturePhoto { photoFile ->
                if (photoFile != null) {
                    _status.value = TouchGuardStatus.Triggered(reason, photoCaptured = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed front camera capture", e)
        }

        // 2. Play loud siren on Alarm stream
        startLoudSiren()

        // 3. Speak warning in Hindi out loud
        startHindiWarningLoop()
    }

    private fun startLoudSiren() {
        sirenJob?.cancel()
        sirenJob = serviceScope.launch(Dispatchers.Default) {
            // Maximize alarm stream volume
            try {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            } catch (_: Exception) {}

            val sampleRate = 22050
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize.coerceAtLeast(sampleRate * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_ALARM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize.coerceAtLeast(sampleRate * 2),
                    AudioTrack.MODE_STREAM
                )
            }

            try {
                audioTrack.play()
                val buffer = ShortArray(1024)
                var phase = 0.0

                while (isActive && isTriggered) {
                    // Police/emergency warble between 750 Hz and 1300 Hz
                    val cycle = (System.currentTimeMillis() % 1000) / 1000.0
                    val freq = 750.0 + 550.0 * sin(cycle * Math.PI)
                    val phaseIncrement = (2.0 * Math.PI * freq) / sampleRate

                    for (i in buffer.indices) {
                        buffer[i] = (sin(phase) * Short.MAX_VALUE * 0.95).toInt().toShort()
                        phase += phaseIncrement
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack siren error", e)
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun startHindiWarningLoop() {
        ttsRepeatJob?.cancel()
        ttsRepeatJob = serviceScope.launch {
            val warningText = "यह फ़ोन किसी और का है, कृपया इसे छोड़ दें"
            while (isActive && isTriggered) {
                if (isTtsReady && textToSpeech != null) {
                    textToSpeech?.speak(
                        warningText,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "intrusion_alert"
                    )
                }
                delay(4500)
            }
        }
    }

    fun disarm() {
        isArmed = false
        isTriggered = false
        countdownJob?.cancel()
        sirenJob?.cancel()
        ttsRepeatJob?.cancel()

        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}

        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}

        try {
            unregisterReceiver(hardwareEventReceiver)
        } catch (_: Exception) {}

        _status.value = TouchGuardStatus.Disarmed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildNotification(title: String, content: String, isTriggered: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disarmIntent = Intent(this, TouchGuardService::class.java).apply {
            action = ACTION_DISARM
        }
        val disarmPendingIntent = PendingIntent.getService(
            this,
            1,
            disarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(if (isTriggered) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_lock_power_off,
                "Disarm",
                disarmPendingIntent
            )

        if (isTriggered) {
            builder.setColor(0xFFFF1744.toInt())
            builder.setColorized(true)
        }

        return builder.build()
    }

    override fun onDestroy() {
        disarm()
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
