package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.network.AiResponse
import com.example.tools.BarcodeInfo
import com.example.tools.NeuralLensHelper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LensMode {
    WHAT_IS_THIS,
    READ_TEXT,
    DOCUMENT,
    QR_BARCODE,
    ASK
}

@Composable
fun NeuralLensScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val config by viewModel.config.collectAsState()

    var selectedMode by remember { mutableStateOf(LensMode.WHAT_IS_THIS) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Analysis State
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var barcodesFound by remember { mutableStateOf<List<BarcodeInfo>>(emptyList()) }
    var isTranslated by remember { mutableStateOf(false) }
    var translatedResult by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    // "Ask" mode state
    var customQuestion by remember { mutableStateOf("") }

    // Link opening confirmation dialog
    var pendingLinkToOpen by remember { mutableStateOf<BarcodeInfo?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                if (isHindi) "कैमरा अनुमति नहीं दी गई" else "Camera permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = NeuralLensHelper.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                capturedBitmap = bitmap
                analysisResult = null
                errorMessage = null
                barcodesFound = emptyList()
                isTranslated = false
                translatedResult = null
            }
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spoken?.firstOrNull()
            if (!text.isNullOrBlank()) {
                customQuestion = text
            }
        }
    }

    // Function to run analysis
    fun executeAnalysis(bitmap: Bitmap, mode: LensMode) {
        scope.launch {
            isAnalyzing = true
            errorMessage = null
            analysisResult = null
            barcodesFound = emptyList()
            isTranslated = false
            translatedResult = null

            if (mode == LensMode.QR_BARCODE) {
                NeuralLensHelper.scanBarcodes(
                    bitmap = bitmap,
                    onSuccess = { list ->
                        barcodesFound = list
                        isAnalyzing = false
                        if (list.isEmpty()) {
                            val msg = if (isHindi) "कोई QR कोड या बारकोड नहीं मिला।" else "No QR code or barcode found in this image."
                            analysisResult = msg
                            if (config.speakAnswers) viewModel.speakText(msg)
                        } else {
                            val summary = if (isHindi) "${list.size} कोड मिले" else "${list.size} code(s) detected"
                            if (config.speakAnswers) viewModel.speakText(summary)
                        }
                    },
                    onError = { e ->
                        isAnalyzing = false
                        errorMessage = "Barcode scan error: ${e.localizedMessage ?: e.message}"
                    }
                )
                return@launch
            }

            // Resizing & compressing for AI vision
            val (_, base64) = withContext(Dispatchers.Default) {
                NeuralLensHelper.resizeAndCompress(bitmap, 1280)
            }

            val prompt = when (mode) {
                LensMode.WHAT_IS_THIS -> {
                    if (isHindi) {
                        "तस्वीर में क्या है पहचानें। किसी भी वस्तु, पौधे, उत्पाद, जानवर, भोजन या स्थान के बारे में सरल और उपयोगी जानकारी हिंदी में दें।"
                    } else {
                        "Identify what is in this photo. Explain any objects, plants, products, animals, food, or places simply and helpfully."
                    }
                }
                LensMode.READ_TEXT -> {
                    if (isHindi) {
                        "इस फ़ोटो में लिखे हुए सभी टेक्स्ट को पढ़ें (OCR करें)। टेक्स्ट को ठीक वैसे ही निकालें जैसा लिखा है।"
                    } else {
                        "Perform OCR on this image. Extract all legible text clearly and accurately as written."
                    }
                }
                LensMode.DOCUMENT -> {
                    if (isHindi) {
                        "इस दस्तावेज़ या फ़ॉर्म को ध्यान से पढ़ें:\n1. संक्षिप्त सारांश (Summary)\n2. मुख्य फ़ील्ड्स (Key Fields): नाम, तिथियां (Dates), राशियां (Amounts), विषय।\nपहचान या बैंक नंबरों को कभी पूरा न लिखें, उन्हें मास्क रखें।"
                    } else {
                        "Analyze this document or form image:\n1. Short Summary (2-3 sentences)\n2. Key Fields (Names, Dates, Amounts, Subject/Ref).\nNever reveal full identity or bank numbers; keep them masked."
                    }
                }
                LensMode.ASK -> {
                    val q = customQuestion.trim().ifBlank {
                        if (isHindi) "इस तस्वीर के बारे में मुख्य बातें बताएं" else "Tell me what you see in this photo"
                    }
                    if (isHindi) {
                        "यूज़र का इस तस्वीर से संबंधित सवाल है: '$q'। इसका सटीक, संक्षिप्त और स्पष्ट उत्तर दें।"
                    } else {
                        "The user asks about this image: '$q'. Answer specifically, accurately, and concisely."
                    }
                }
                LensMode.QR_BARCODE -> ""
            }

            val response = viewModel.analyzeImageVision(
                imageBase64 = base64,
                prompt = prompt
            )

            isAnalyzing = false
            when (response) {
                is AiResponse.Text -> {
                    var finalText = response.content
                    if (mode == LensMode.DOCUMENT) {
                        finalText = NeuralLensHelper.maskSensitiveNumbers(finalText)
                    }
                    analysisResult = finalText
                    if (config.speakAnswers) {
                        viewModel.speakText(finalText)
                    }
                }
                is AiResponse.Error -> {
                    errorMessage = response.message
                }
                else -> {
                    errorMessage = "Unexpected response from AI provider."
                }
            }
        }
    }

    // Auto-trigger analysis when bitmap is set (except in ASK mode if user hasn't typed a question yet)
    LaunchedEffect(capturedBitmap, selectedMode) {
        val bmp = capturedBitmap
        if (bmp != null) {
            if (selectedMode != LensMode.ASK || customQuestion.isNotBlank()) {
                executeAnalysis(bmp, selectedMode)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("neural_lens_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanAccent
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isHindi) "Neural Lens (विज़न)" else "Neural Lens",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Quick Gallery Pick
            IconButton(
                onClick = {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("neural_lens_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Pick from gallery",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Mode Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LensModeChip(
                title = if (isHindi) "यह क्या है?" else "What is this?",
                icon = Icons.Default.HelpOutline,
                selected = selectedMode == LensMode.WHAT_IS_THIS,
                onClick = {
                    selectedMode = LensMode.WHAT_IS_THIS
                }
            )
            LensModeChip(
                title = if (isHindi) "टेक्स्ट पढ़ें" else "Read text",
                icon = Icons.Default.TextFields,
                selected = selectedMode == LensMode.READ_TEXT,
                onClick = {
                    selectedMode = LensMode.READ_TEXT
                }
            )
            LensModeChip(
                title = if (isHindi) "दस्तावेज़" else "Document",
                icon = Icons.Default.Description,
                selected = selectedMode == LensMode.DOCUMENT,
                onClick = {
                    selectedMode = LensMode.DOCUMENT
                }
            )
            LensModeChip(
                title = if (isHindi) "QR / बारकोड" else "QR / Barcode",
                icon = Icons.Default.QrCodeScanner,
                selected = selectedMode == LensMode.QR_BARCODE,
                onClick = {
                    selectedMode = LensMode.QR_BARCODE
                }
            )
            LensModeChip(
                title = if (isHindi) "सवाल पूछें" else "Ask",
                icon = Icons.Default.HelpOutline,
                selected = selectedMode == LensMode.ASK,
                onClick = {
                    selectedMode = LensMode.ASK
                }
            )
        }

        // Document mode security warning notice
        if (selectedMode == LensMode.DOCUMENT) {
            Surface(
                color = WarningAmber.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Do not scan ID cards, bank or card numbers. The photo is sent to the selected AI provider.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = WarningAmber,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Main View Area: Camera Preview OR Captured Photo + Results
        val currentBmp = capturedBitmap
        if (currentBmp == null) {
            // Camera Live Preview Area
            if (!hasCameraPermission) {
                CameraPermissionPrompt(
                    isHindi = isHindi,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                                } catch (e: Exception) {
                                    Log.e("NeuralLens", "Camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Controls on Camera Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery button
                        IconButton(
                            onClick = {
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("lens_gallery_bottom_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White
                            )
                        }

                        // Shutter Capture button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, PrimaryBlue, CircleShape)
                                .clickable {
                                    val capture = imageCapture
                                    if (capture != null) {
                                        capture.takePicture(
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                    val bmp = imageProxy.toBitmap()
                                                    imageProxy.close()
                                                    capturedBitmap = bmp
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    Log.e("NeuralLens", "Capture failed", exception)
                                                    Toast.makeText(
                                                        context,
                                                        "Capture failed: ${exception.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                }
                                .testTag("camera_capture_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            )
                        }

                        // Flip Camera button
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("flip_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Flip Camera",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Captured Photo & Results Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Thumbnail with Retake & Change button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = currentBmp.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )

                    // Retake button on top of thumbnail
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.clickable {
                                capturedBitmap = null
                                analysisResult = null
                                errorMessage = null
                                barcodesFound = emptyList()
                                isTranslated = false
                                translatedResult = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retake",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isHindi) "दुबारा लें" else "Retake",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // If Ask mode: question input box
                if (selectedMode == LensMode.ASK) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customQuestion,
                            onValueChange = { customQuestion = it },
                            placeholder = {
                                Text(
                                    if (isHindi) "इस तस्वीर के बारे में पूछें..." else "Ask a question about this photo...",
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lens_ask_question_input")
                        )

                        // Mic button
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-US")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "सवाल पूछिए..." else "Ask your question...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("lens_ask_mic_button")
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Input", tint = CyanAccent)
                        }

                        // Send question button
                        IconButton(
                            onClick = {
                                if (customQuestion.isNotBlank()) {
                                    executeAnalysis(currentBmp, LensMode.ASK)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                                .testTag("lens_ask_send_button")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Ask", tint = Color.White)
                        }
                    }
                }

                // Loading State
                if (isAnalyzing) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CyanAccent,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = if (isHindi) "तस्वीर का विश्लेषण हो रहा है..." else "Analyzing photo with Mamta AI...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                // Error State
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DangerRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = DangerRed)
                                Text(
                                    text = if (isHindi) "त्रुटि (Error)" else "Analysis Error",
                                    style = MaterialTheme.typography.titleSmall.copy(color = DangerRed, fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            Button(
                                onClick = { executeAnalysis(currentBmp, selectedMode) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isHindi) "पुनः प्रयास करें" else "Try Again")
                            }
                        }
                    }
                }

                // Barcodes Result View
                if (selectedMode == LensMode.QR_BARCODE && barcodesFound.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isHindi) "स्कैन किए गए कोड्स:" else "Detected Codes:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                        )
                        barcodesFound.forEach { barcode ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (barcode.isPayment) {
                                        Surface(
                                            color = DangerRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Payment Warning",
                                                    tint = DangerRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Payment link detected. Never auto-opened for your safety.",
                                                    color = DangerRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = barcode.rawValue,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (barcode.isUrl) {
                                            Button(
                                                onClick = {
                                                    pendingLinkToOpen = barcode
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (barcode.isPayment) WarningAmber else PrimaryBlue
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isHindi) "लिंक खोलें" else "Open Link")
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Barcode", barcode.rawValue))
                                                Toast.makeText(context, if (isHindi) "कॉपी किया गया" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isHindi) "कॉपी" else "Copy")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Text Analysis Result View
                val res = analysisResult
                if (res != null) {
                    val displayText = if (isTranslated && translatedResult != null) translatedResult!! else res

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isTranslated) (if (isHindi) "🌐 अनुवाद (Translation)" else "🌐 Translated")
                                    else when (selectedMode) {
                                        LensMode.WHAT_IS_THIS -> if (isHindi) "🔍 पहचान (Identity)" else "🔍 Identity"
                                        LensMode.READ_TEXT -> if (isHindi) "📄 पहचाना गया टेक्स्ट (OCR)" else "📄 Extracted Text"
                                        LensMode.DOCUMENT -> if (isHindi) "📋 दस्तावेज़ सारांश (Document)" else "📋 Document Summary"
                                        LensMode.ASK -> if (isHindi) "💬 उत्तर (Answer)" else "💬 Answer"
                                        LensMode.QR_BARCODE -> "QR / Barcode"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent
                                    )
                                )

                                // Copy button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Neural Lens", displayText))
                                        Toast.makeText(context, if (isHindi) "कॉपी किया गया" else "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            )

                            // Translate button for "Read text" mode
                            if (selectedMode == LensMode.READ_TEXT) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (isTranslated) {
                                                isTranslated = false
                                            } else {
                                                if (translatedResult != null) {
                                                    isTranslated = true
                                                } else {
                                                    scope.launch {
                                                        isTranslating = true
                                                        val targetLang = if (isHindi) "Hindi" else "English"
                                                        val transPrompt = "Translate the following extracted text to $targetLang accurately. Return only the translated text:\n\n$res"
                                                        val transRes = viewModel.askAiOneShot(transPrompt)
                                                        isTranslating = false
                                                        if (transRes.isNotBlank()) {
                                                            translatedResult = transRes
                                                            isTranslated = true
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isTranslating) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        } else {
                                            Icon(imageVector = Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (isTranslated) (if (isHindi) "मूल टेक्स्ट देखें" else "Show Original")
                                            else (if (isHindi) "हिंदी/अंग्रेजी में अनुवाद करें" else "Translate to Hindi/English")
                                        )
                                    }
                                }
                            }

                            // Action buttons: "Save to Journal" and "Share"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val modeName = when (selectedMode) {
                                            LensMode.WHAT_IS_THIS -> "Object"
                                            LensMode.READ_TEXT -> "OCR"
                                            LensMode.DOCUMENT -> "Document"
                                            LensMode.ASK -> "Visual Q&A"
                                            LensMode.QR_BARCODE -> "Barcode"
                                        }
                                        viewModel.journalDb.addEntry(
                                            title = "Neural Lens ($modeName)",
                                            content = displayText,
                                            mood = "📷"
                                        )
                                        Toast.makeText(
                                            context,
                                            if (isHindi) "📔 दैनिक जर्नल में सुरक्षित कर दिया गया!" else "Saved to Daily Journal!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lens_save_to_journal_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHindi) "जर्नल में जोड़ें" else "Save to Journal")
                                }

                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Mamta Neural Lens Result:\n\n$displayText")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Result"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lens_share_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHindi) "शेयर करें" else "Share")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Link opening confirmation dialog
    pendingLinkToOpen?.let { linkInfo ->
        AlertDialog(
            onDismissRequest = { pendingLinkToOpen = null },
            title = {
                Text(
                    text = if (linkInfo.isPayment) (if (isHindi) "⚠️ भुगतान लिंक चेतावनी" else "⚠️ Payment Link Warning")
                    else (if (isHindi) "🔗 लिंक खोलें?" else "🔗 Open Link?"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (linkInfo.isPayment) {
                        Text(
                            text = if (isHindi) "सुरक्षा चेतावनी: यह एक भुगतान (Payment/UPI) लिंक है। ममता इसे कभी स्वतः नहीं खोलती। क्या आप वाकई इसे खोलना चाहते हैं?"
                            else "Security Warning: This is a payment (UPI/Wallet) link. Mamta never auto-opens payment links for your protection. Do you still wish to proceed?",
                            color = DangerRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (isHindi) "क्या आप इस लिंक को अपने ब्राउज़र में खोलना चाहते हैं?"
                            else "Do you want to open this link in your browser?"
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = linkInfo.rawValue,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = CyanAccent)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = try { Uri.parse(linkInfo.rawValue) } catch (_: Exception) { null }
                        if (uri != null) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        pendingLinkToOpen = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (linkInfo.isPayment) DangerRed else PrimaryBlue
                    )
                ) {
                    Text(if (linkInfo.isPayment) (if (isHindi) "हाँ, जोखिम समझकर खोलें" else "Proceed anyway")
                    else (if (isHindi) "खोलें" else "Open"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingLinkToOpen = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun LensModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PrimaryBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) PrimaryBlue else SurfaceBorder
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("lens_mode_chip_$title")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) CyanAccent else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun CameraPermissionPrompt(
    isHindi: Boolean,
    onRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission",
                        tint = CyanAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (isHindi) "कैमरा अनुमति आवश्यक है" else "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = if (isHindi)
                        "ममता Neural Lens कैमरे की मदद से वस्तुएं पहचानती है, टेक्स्ट पढ़ती है, दस्तावेज़ और QR कोड स्कैन करती है। बैकग्राउंड में कुछ भी रिकॉर्ड नहीं होता; केवल बटन दबाने पर ही फ़ोटो ली जाती है।"
                    else
                        "Mamta Neural Lens uses your camera to identify objects, read text, scan documents, and read QR codes. Nothing is captured in the background; a photo is taken only when you tap the capture button.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_button")
                ) {
                    Text(
                        text = if (isHindi) "कैमरा अनुमति दें" else "Grant Camera Permission",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
