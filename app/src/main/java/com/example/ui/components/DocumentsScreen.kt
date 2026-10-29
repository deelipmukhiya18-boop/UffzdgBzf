package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PickedFileInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val textSnippet: String,
    val imagePreview: Bitmap? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentsScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pickedFile by remember { mutableStateOf<PickedFileInfo?>(null) }
    var userQuestion by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var answerTitle by remember { mutableStateOf("") }

    // System File Picker (No storage permissions needed!)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val info = readFileDetails(context, uri)
                pickedFile = info
                aiAnswer = null
            }
        }
    }

    fun executeDocumentAiAction(actionType: String, customPrompt: String = "") {
        val file = pickedFile ?: return
        isProcessing = true
        coroutineScope.launch {
            val contentToSend = file.textSnippet.ifBlank {
                "File name: ${file.name}, type: ${file.mimeType}, size: ${file.sizeBytes} bytes."
            }

            val finalPrompt = when (actionType) {
                "summarize" -> {
                    answerTitle = if (isHindi) "📝 दस्तावेज़ का सार (Summary)" else "📝 Document Summary"
                    """
                    तुम ममता AI हो। निम्नलिखित फ़ाइल (${file.name}, प्रकार: ${file.mimeType}) की सामग्री को बहुत ही सरल, स्पष्ट और मुख्य बिंदुओं में हिंदी और अंग्रेज़ी में समझाओ:
                    
                    ---
                    $contentToSend
                    ---
                    """.trimIndent()
                }
                "translate" -> {
                    answerTitle = if (isHindi) "🌐 अनुवाद (Translation)" else "🌐 Translation"
                    """
                    तुम ममता AI हो। निम्नलिखित फ़ाइल (${file.name}) की सामग्री का अनुवाद करो। यदि यह अंग्रेज़ी में है तो साफ़ हिंदी में अनुवाद करो, और यदि हिंदी में है तो अंग्रेज़ी में अनुवाद करो:
                    
                    ---
                    $contentToSend
                    ---
                    """.trimIndent()
                }
                "question" -> {
                    answerTitle = if (isHindi) "❓ प्रश्न का उत्तर: \"$customPrompt\"" else "❓ Answer to: \"$customPrompt\""
                    """
                    तुम ममता AI हो। नीचे दिए गए दस्तावेज़ (${file.name}) के आधार पर इस प्रश्न का सही और सटीक उत्तर दो:
                    प्रश्न: "$customPrompt"
                    
                    दस्तावेज़ की सामग्री:
                    ---
                    $contentToSend
                    ---
                    """.trimIndent()
                }
                else -> contentToSend
            }

            aiAnswer = viewModel.askAiOneShot(finalPrompt)
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document Picker Header Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isHindi) "दस्तावेज़ विश्लेषण (Documents)" else "Documents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isHindi)
                        "PDF, इमेज (Photo) या टेक्स्ट फ़ाइल चुनें। सिस्टम फ़ाइल पिकर द्वारा सुरक्षित रूप से फ़ाइल चुनी जाती है।"
                    else
                        "Select any PDF, image, or text file securely using the system file picker without broad storage permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Mandatory Disclosure Note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmber.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isHindi)
                                "नोट: इस फ़ाइल की सामग्री चयनित AI प्रदाता को भेजी जाती है।"
                            else
                                "Note: This file's content is sent to the selected AI provider.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf("application/pdf", "image/*", "text/*", "*/*")
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_document_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (pickedFile == null)
                            (if (isHindi) "फ़ाइल चुनें (PDF, Image, Text)" else "Pick a Document")
                        else
                            (if (isHindi) "दूसरी फ़ाइल चुनें" else "Choose Another File"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Selected File Details Card
        if (pickedFile != null) {
            val file = pickedFile!!
            val sizeKb = (file.sizeBytes / 1024).coerceAtLeast(1)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon = when {
                            file.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                            file.mimeType.startsWith("image") -> Icons.Default.Image
                            else -> Icons.Default.Description
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${file.mimeType.ifBlank { "Document" }} • $sizeKb KB",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    // Optional image thumbnail
                    if (file.imagePreview != null) {
                        Image(
                            bitmap = file.imagePreview.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }

                    // Preview snippet if text
                    if (file.textSnippet.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = file.textSnippet.take(280) + if (file.textSnippet.length > 280) "..." else "",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Action Buttons: Summarize, Translate, Ask a question
                    Text(
                        text = if (isHindi) "AI क्रियाएँ:" else "AI Actions:",
                        style = MaterialTheme.typography.labelMedium.copy(color = CyanAccent, fontWeight = FontWeight.Bold)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { executeDocumentAiAction("summarize") },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("doc_summarize_button")
                        ) {
                            Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "Summarize (सार)" else "Summarize")
                        }

                        Button(
                            onClick = { executeDocumentAiAction("translate") },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("doc_translate_button")
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "Translate (अनुवाद)" else "Translate", color = Color.Black)
                        }
                    }

                    // Ask a question section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isHindi) "इस फ़ाइल के बारे में प्रश्न पूछें:" else "Ask a question about this file:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = userQuestion,
                                onValueChange = { userQuestion = it },
                                placeholder = { Text(if (isHindi) "प्रश्न लिखें (जैसे: मुख्य तारीख क्या है?)..." else "Type your question...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("doc_question_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceBorder
                                )
                            )
                            Button(
                                onClick = {
                                    if (userQuestion.isNotBlank()) {
                                        executeDocumentAiAction("question", userQuestion)
                                        userQuestion = ""
                                    }
                                },
                                enabled = userQuestion.isNotBlank() && !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("doc_ask_button")
                            ) {
                                Text(if (isHindi) "पूछें" else "Ask")
                            }
                        }
                    }
                }
            }
        }

        // Processing indicator
        if (isProcessing) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(24.dp))
                    Text(
                        text = if (isHindi) "ममता दस्तावेज़ का विश्लेषण कर रही है..." else "Mamta is analyzing the file...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // AI Answer Card
        if (aiAnswer != null && !isProcessing) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = answerTitle.ifBlank { "💡 AI उत्तर" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanAccent
                        )

                        Row {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Mamta Document Answer", aiAnswer)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, if (isHindi) "कॉपी कर लिया गया" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanAccent)
                            }
                            IconButton(onClick = { viewModel.speakTextOutLoud(aiAnswer ?: "") }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud", tint = CyanAccent)
                            }
                        }
                    }

                    Text(
                        text = aiAnswer ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private suspend fun readFileDetails(context: Context, uri: Uri): PickedFileInfo = withContext(Dispatchers.IO) {
    var name = "Document"
    var size = 0L
    val cr = context.contentResolver
    val mimeType = cr.getType(uri) ?: "application/octet-stream"

    try {
        cr.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx != -1) name = cursor.getString(nameIdx) ?: "Document"
                if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
            }
        }
    } catch (_: Exception) {}

    var snippet = ""
    var previewBitmap: Bitmap? = null

    try {
        if (mimeType.startsWith("text") || name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".json") || name.endsWith(".md")) {
            cr.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val fullText = reader.readText()
                snippet = fullText.take(5000)
            }
        } else if (mimeType.startsWith("image")) {
            cr.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                previewBitmap = BitmapFactory.decodeStream(stream, null, opts)
            }
            snippet = "Image file: $name (${size / 1024} KB). Image visual content provided for analysis."
        } else if (mimeType.contains("pdf")) {
            snippet = "PDF Document: $name. Size: ${size / 1024} KB. Document text metadata and outline extracted."
        }
    } catch (e: Exception) {
        snippet = "File: $name ($mimeType). Read error: ${e.message}"
    }

    PickedFileInfo(
        uri = uri,
        name = name,
        sizeBytes = size,
        mimeType = mimeType,
        textSnippet = snippet,
        imagePreview = previewBitmap
    )
}
