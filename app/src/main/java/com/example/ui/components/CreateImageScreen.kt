package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tools.ImageStorageHelper
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@Composable
fun CreateImageScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by viewModel.config.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Poster") }
    val styles = listOf("Poster", "Logo", "Photo", "Cartoon")

    // Provider selection: OpenAI Images or Gemini image generation
    var provider by remember { mutableStateOf(config.imageProvider.ifBlank { "openai" }) }
    var openAiModel by remember { mutableStateOf(config.openaiImageModel.ifBlank { "dall-e-3" }) }
    var geminiModel by remember { mutableStateOf(config.geminiImageModel.ifBlank { "imagen-3.0-generate-002" }) }

    // Generation state
    var isGenerating by remember { mutableStateOf(false) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var revisedPrompt by remember { mutableStateOf<String?>(null) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    // Speech Recognizer launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                promptText = spoken
            }
        }
    }

    fun startSpeech() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "इमेज का विवरण बोलें..." else "Describe your image...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice input not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun runGeneration() {
        if (promptText.isBlank()) return
        val activeModel = if (provider.equals("gemini", ignoreCase = true)) geminiModel else openAiModel
        val key = if (provider.equals("gemini", ignoreCase = true)) config.geminiApiKey else config.openaiApiKey

        if (key.isBlank()) {
            errorMessage = if (provider.equals("gemini", ignoreCase = true)) {
                "Gemini API key is missing. Please set it in Settings first."
            } else {
                "OpenAI API key is missing. Please set it in Settings first."
            }
            return
        }

        isGenerating = true
        errorMessage = null
        saveStatusMsg = null

        // Save selected provider & model names back to config
        viewModel.updateConfig(
            config.copy(
                imageProvider = provider,
                openaiImageModel = openAiModel,
                geminiImageModel = geminiModel
            )
        )

        scope.launch {
            val result = viewModel.generateImageDirect(
                provider = provider,
                prompt = promptText.trim(),
                style = selectedStyle,
                modelName = activeModel
            )
            isGenerating = false
            if (result.error != null) {
                errorMessage = result.error
            } else if (!result.base64Data.isNullOrBlank()) {
                val bmp = ImageStorageHelper.decodeBase64ToBitmap(result.base64Data)
                if (bmp != null) {
                    generatedBitmap = bmp
                    revisedPrompt = result.revisedPrompt
                } else {
                    errorMessage = "Failed to decode generated image bitmap."
                }
            } else {
                errorMessage = "No image data returned from provider."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("create_image_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "तस्वीर बनाएं (Create Image)" else "Create Image",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI Image Generator (OpenAI / Gemini)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Mandatory Cost / AI Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarningAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Images are created by the selected AI provider and may cost money on your API account.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Provider Selector
            Text(
                text = if (isHindi) "AI प्रदाता (Provider):" else "AI Provider:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OpenAI Provider Card
                val isOpenAi = provider.equals("openai", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOpenAi) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isOpenAi) CyanAccent else SurfaceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { provider = "openai" }
                        .testTag("provider_openai_button")
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "OpenAI Images",
                            fontWeight = if (isOpenAi) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOpenAi) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "DALL-E 3 / DALL-E 2",
                            fontSize = 10.sp,
                            color = if (isOpenAi) Color.White.copy(alpha = 0.8f) else TextSecondary
                        )
                    }
                }

                // Gemini Provider Card
                val isGemini = provider.equals("gemini", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGemini) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isGemini) CyanAccent else SurfaceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { provider = "gemini" }
                        .testTag("provider_gemini_button")
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Gemini Imagen",
                            fontWeight = if (isGemini) FontWeight.Bold else FontWeight.Medium,
                            color = if (isGemini) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Imagen 3",
                            fontSize = 10.sp,
                            color = if (isGemini) Color.White.copy(alpha = 0.8f) else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Editable Model Name field with mandatory hint
            if (provider.equals("openai", ignoreCase = true)) {
                OutlinedTextField(
                    value = openAiModel,
                    onValueChange = { openAiModel = it },
                    label = { Text("OpenAI Image Model Name") },
                    supportingText = { Text("check the provider's docs for the current model name (e.g. dall-e-3)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("openai_model_input")
                )
            } else {
                OutlinedTextField(
                    value = geminiModel,
                    onValueChange = { geminiModel = it },
                    label = { Text("Gemini Image Model Name") },
                    supportingText = { Text("check the provider's docs for the current model name (e.g. imagen-3.0-generate-002)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_model_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Style Chips Row (Poster, Logo, Photo, Cartoon)
            Text(
                text = if (isHindi) "शैली चुनें (Style):" else "Select Style:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(styles) { style ->
                    val isSelected = selectedStyle == style
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) CyanAccent else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) CyanAccent else SurfaceBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedStyle = style }
                            .testTag("style_chip_${style.lowercase()}")
                    ) {
                        Text(
                            text = style,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0F172A) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prompt Box (Typing or Voice)
            Text(
                text = if (isHindi) "प्रॉम्प्ट लिखें या बोलें:" else "Describe your image:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(if (isHindi) "उदा. एक सुंदर भारतीय मंदिर का सूर्यास्त पोस्टर..." else "e.g. A serene mountain landscape poster at sunrise with glowing mist...")
                },
                trailingIcon = {
                    IconButton(
                        onClick = { startSpeech() },
                        modifier = Modifier.testTag("image_prompt_mic_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = CyanAccent)
                    }
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_prompt_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Generate Button
            Button(
                onClick = { runGeneration() },
                enabled = !isGenerating && promptText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_image_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (isHindi) "तस्वीर बनाई जा रही है..." else "Generating image...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "तस्वीर बनाएं (Generate)" else "Generate Image", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Real Error Message Display
            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DangerRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Save status notification
            saveStatusMsg?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }

            // Generated Image Result
            generatedBitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Generated image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        revisedPrompt?.let { rp ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Prompt: $rp",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Action Buttons: Save to gallery, Share, Regenerate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Save to Gallery
                            Button(
                                onClick = {
                                    val uri = ImageStorageHelper.saveToGallery(
                                        context = context,
                                        bitmap = bmp,
                                        title = "Mamta_${System.currentTimeMillis()}"
                                    )
                                    saveStatusMsg = if (uri != null) {
                                        "Saved to Gallery in Pictures/Mamta!"
                                    } else {
                                        "Saved image locally."
                                    }
                                    Toast.makeText(context, saveStatusMsg, Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_gallery_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "गैलरी में सेव" else "Save", fontSize = 12.sp)
                            }

                            // Share
                            Button(
                                onClick = {
                                    ImageStorageHelper.shareImage(context, bmp, promptText)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_image_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "शेयर" else "Share", fontSize = 12.sp)
                            }

                            // Regenerate
                            OutlinedButton(
                                onClick = { runGeneration() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("regenerate_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "दोबारा" else "Regen", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
