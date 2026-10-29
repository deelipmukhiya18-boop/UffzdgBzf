package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Headless front camera capturer for Touch Guard anti-theft photos.
 * Photos are saved ONLY into internal private app storage: filesDir/intrusion_photos/
 */
class FrontCameraCapturer(private val context: Context) {

    companion object {
        private const val TAG = "FrontCameraCapturer"
        const val SUBDIR = "intrusion_photos"

        fun getPhotosDir(context: Context): File {
            val dir = File(context.filesDir, SUBDIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

        fun getSavedPhotos(context: Context): List<File> {
            val dir = getPhotosDir(context)
            return dir.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private fun startBackgroundThread() {
        val thread = HandlerThread("CameraBackgroundThread")
        thread.start()
        backgroundThread = thread
        backgroundHandler = Handler(thread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun capturePhoto(onPhotoCaptured: (File?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted, skipping capture")
            onPhotoCaptured(null)
            return
        }

        startBackgroundThread()
        val handler = backgroundHandler ?: run {
            onPhotoCaptured(null)
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            stopBackgroundThread()
            onPhotoCaptured(null)
            return
        }

        try {
            var selectedCameraId: String? = null

            // Find front camera
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    selectedCameraId = id
                    break
                }
            }
            // Fallback to first camera if front camera not found
            if (selectedCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                selectedCameraId = cameraManager.cameraIdList[0]
            }

            if (selectedCameraId == null) {
                stopBackgroundThread()
                onPhotoCaptured(null)
                return
            }

            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            var cameraDevice: CameraDevice? = null
            var captureSession: CameraCaptureSession? = null

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        val dir = getPhotosDir(context)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val file = File(dir, "intrusion_${timestamp}.jpg")

                        FileOutputStream(file).use { fos ->
                            fos.write(bytes)
                            fos.flush()
                        }
                        Log.i(TAG, "Saved intrusion photo: ${file.absolutePath}")
                        onPhotoCaptured(file)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save intrusion photo", e)
                        onPhotoCaptured(null)
                    } finally {
                        image.close()
                        try {
                            captureSession?.close()
                            cameraDevice?.close()
                            imageReader.close()
                        } catch (_: Exception) {}
                        stopBackgroundThread()
                    }
                }
            }, handler)

            cameraManager.openCamera(selectedCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    try {
                        val surface = imageReader.surface
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSession = session
                                    try {
                                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                                        builder.addTarget(surface)
                                        session.capture(builder.build(), null, handler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error triggering still capture", e)
                                        cleanUp()
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.e(TAG, "Camera capture session configuration failed")
                                    cleanUp()
                                }

                                private fun cleanUp() {
                                    try {
                                        captureSession?.close()
                                        cameraDevice?.close()
                                        imageReader.close()
                                    } catch (_: Exception) {}
                                    stopBackgroundThread()
                                    onPhotoCaptured(null)
                                }
                            },
                            handler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating capture session", e)
                        camera.close()
                        imageReader.close()
                        stopBackgroundThread()
                        onPhotoCaptured(null)
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    imageReader.close()
                    stopBackgroundThread()
                    onPhotoCaptured(null)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    camera.close()
                    imageReader.close()
                    stopBackgroundThread()
                    onPhotoCaptured(null)
                }
            }, handler)

        } catch (e: Exception) {
            Log.e(TAG, "Error initiating camera capture", e)
            stopBackgroundThread()
            onPhotoCaptured(null)
        }
    }
}
