package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream

data class BarcodeInfo(
    val rawValue: String,
    val format: Int,
    val isUrl: Boolean,
    val isPayment: Boolean
)

object NeuralLensHelper {
    private const val TAG = "NeuralLensHelper"

    // 12-digit Aadhaar: e.g. 1234 5678 9012 or 123456789012
    private val AADHAAR_REGEX = Regex("""\b(\d{4})[\s-]?(\d{4})[\s-]?(\d{4})\b""")

    // 10-char PAN: e.g. ABCDE1234F
    private val PAN_REGEX = Regex("""\b([A-Z]{5})(\d{4})([A-Z])\b""", RegexOption.IGNORE_CASE)

    // 16-digit Card numbers: e.g. 1234-5678-9012-3456 or 1234567890123456
    private val CARD_REGEX = Regex("""\b(\d{4})[\s-]?(\d{4})[\s-]?(\d{4})[\s-]?(\d{4})\b""")

    // 9 to 18-digit bank account or other long number sequences
    private val LONG_NUMBER_REGEX = Regex("""\b\d{9,18}\b""")

    fun resizeAndCompress(
        bitmap: Bitmap,
        maxDimension: Int = 1280,
        quality: Int = 85
    ): Pair<Bitmap, String> {
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (width > height) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
        val bytes = outStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return Pair(scaledBitmap, base64)
    }

    fun maskSensitiveNumbers(text: String): String {
        var masked = text

        // Mask Aadhaar: XXXX-XXXX-1234
        masked = AADHAAR_REGEX.replace(masked) { match ->
            "XXXX-XXXX-${match.groupValues[3]}"
        }

        // Mask PAN: XXXXX1234X
        masked = PAN_REGEX.replace(masked) { match ->
            "XXXXX${match.groupValues[2]}${match.groupValues[3].uppercase()}"
        }

        // Mask Credit/Debit Cards: XXXX-XXXX-XXXX-1234
        masked = CARD_REGEX.replace(masked) { match ->
            "XXXX-XXXX-XXXX-${match.groupValues[4]}"
        }

        // Mask remaining long number sequences (9 to 18 digits)
        masked = LONG_NUMBER_REGEX.replace(masked) { match ->
            val num = match.value
            if (num.length >= 8) {
                val last4 = num.takeLast(4)
                "X".repeat(num.length - 4) + last4
            } else num
        }

        return masked
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from uri", e)
            null
        }
    }

    fun scanBarcodes(
        bitmap: Bitmap,
        onSuccess: (List<BarcodeInfo>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val results = barcodes.map { b ->
                        val raw = b.rawValue ?: b.displayValue.orEmpty()
                        val lower = raw.lowercase()
                        val isPayment = lower.startsWith("upi://") ||
                                lower.contains("paytm") ||
                                lower.contains("tez") ||
                                lower.contains("gpay") ||
                                lower.contains("phonepe") ||
                                lower.contains("bhim")
                        val isUrl = b.valueType == Barcode.TYPE_URL ||
                                lower.startsWith("http://") ||
                                lower.startsWith("https://")
                        BarcodeInfo(
                            rawValue = raw,
                            format = b.format,
                            isUrl = isUrl,
                            isPayment = isPayment
                        )
                    }
                    onSuccess(results)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
