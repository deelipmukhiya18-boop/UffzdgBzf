package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppConfig
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    initialConfig: AppConfig,
    isHindi: Boolean,
    onSave: (AppConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var provider by remember { mutableStateOf(initialConfig.provider) }
    var claudeKey by remember { mutableStateOf(initialConfig.claudeApiKey) }
    var claudeModel by remember { mutableStateOf(initialConfig.claudeModel) }
    var geminiKey by remember { mutableStateOf(initialConfig.geminiApiKey) }
    var geminiModel by remember { mutableStateOf(initialConfig.geminiModel) }
    var openaiKey by remember { mutableStateOf(initialConfig.openaiApiKey) }
    var openaiModel by remember { mutableStateOf(initialConfig.openaiModel) }
    var searchProvider by remember { mutableStateOf(initialConfig.searchProvider) }
    var searchKey by remember { mutableStateOf(initialConfig.searchApiKey) }
    var speakAnswers by remember { mutableStateOf(initialConfig.speakAnswers) }
    var imageProvider by remember { mutableStateOf(initialConfig.imageProvider.ifBlank { "openai" }) }
    var openaiImageModel by remember { mutableStateOf(initialConfig.openaiImageModel.ifBlank { "dall-e-3" }) }
    var geminiImageModel by remember { mutableStateOf(initialConfig.geminiImageModel.ifBlank { "imagen-3.0-generate-002" }) }

    var showClaudeKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenaiKey by remember { mutableStateOf(false) }
    var showSearchKey by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isHindi) "⚙️ सेटिंग (Settings)" else "⚙️ Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 1: AI Engine
                SectionHeader(
                    icon = Icons.Default.Psychology,
                    title = if (isHindi) "1. बात करने वाला AI" else "1. Conversational AI Model"
                )

                Text(
                    text = if (isHindi) "कौन सा AI चलाना है:" else "Select AI Provider:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderOptionButton(
                        title = "Gemini",
                        selected = provider == "gemini",
                        onClick = { provider = "gemini" },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderOptionButton(
                        title = "OpenAI",
                        selected = provider == "openai",
                        onClick = { provider = "openai" },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderOptionButton(
                        title = "Claude",
                        selected = provider == "claude",
                        onClick = { provider = "claude" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Claude Settings
                Text(
                    text = "Claude API key:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = claudeKey,
                    onValueChange = { claudeKey = it },
                    placeholder = { Text("sk-ant-...", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    visualTransformation = if (showClaudeKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showClaudeKey = !showClaudeKey }) {
                            Icon(
                                imageVector = if (showClaudeKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag("claude_api_key_field")
                )
                Text(
                    text = if (isHindi) "console.anthropic.com से मिलती है" else "Available from console.anthropic.com",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isHindi) "Claude मॉडल का नाम:" else "Claude Model Name:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = claudeModel,
                    onValueChange = { claudeModel = it },
                    placeholder = { Text("claude-sonnet-5-5") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 12.dp)
                        .testTag("claude_model_field")
                )

                // Gemini Settings
                Text(
                    text = "Gemini API key:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    placeholder = { Text("AIza...", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Icon(
                                imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag("gemini_api_key_field")
                )
                Text(
                    text = if (isHindi) "aistudio.google.com से मिलती है" else "Available from aistudio.google.com",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isHindi) "Gemini मॉडल का नाम:" else "Gemini Model Name:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = geminiModel,
                    onValueChange = { geminiModel = it },
                    placeholder = { Text("gemini-3.8-flash") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 12.dp)
                        .testTag("gemini_model_field")
                )

                // OpenAI Settings
                Text(
                    text = "OpenAI API key:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = openaiKey,
                    onValueChange = { openaiKey = it },
                    placeholder = { Text("sk-proj-...", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    visualTransformation = if (showOpenaiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOpenaiKey = !showOpenaiKey }) {
                            Icon(
                                imageVector = if (showOpenaiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag("openai_api_key_field")
                )
                Text(
                    text = if (isHindi) "platform.openai.com से मिलती है" else "Available from platform.openai.com",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isHindi) "OpenAI मॉडल का नाम:" else "OpenAI Model Name:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = openaiModel,
                    onValueChange = { openaiModel = it },
                    placeholder = { Text("gpt-4o-mini") },
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "check platform.openai.com/docs/models for the current name",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp)
                        .testTag("openai_model_field")
                )

                HorizontalDivider(color = SurfaceBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 2: Web Search
                SectionHeader(
                    icon = Icons.Default.Search,
                    title = if (isHindi) "2. इंटरनेट सर्च (खबर, मौसम, भाव वगैरह)" else "2. Live Web Search (News, Weather, Prices)"
                )

                Text(
                    text = if (isHindi) "सर्च सेवा:" else "Search Provider:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProviderOptionButton(
                        title = "Tavily",
                        selected = searchProvider == "tavily",
                        onClick = { searchProvider = "tavily" },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderOptionButton(
                        title = "Brave Search",
                        selected = searchProvider == "brave",
                        onClick = { searchProvider = "brave" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isHindi) "सर्च API key:" else "Search API Key:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = searchKey,
                    onValueChange = { searchKey = it },
                    placeholder = { Text("tvly-... या Brave key", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    visualTransformation = if (showSearchKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showSearchKey = !showSearchKey }) {
                            Icon(
                                imageVector = if (showSearchKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag("search_api_key_field")
                )
                Text(
                    text = "Tavily: tavily.com, Brave: brave.com/search/api",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = SurfaceBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 3: Voice
                SectionHeader(
                    icon = Icons.Default.RecordVoiceOver,
                    title = if (isHindi) "3. आवाज़ (Voice)" else "3. Voice Response"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { speakAnswers = !speakAnswers }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = speakAnswers,
                        onCheckedChange = { speakAnswers = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isHindi) "जवाब बोलकर सुनाओ" else "Speak responses aloud",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isHindi) "आवाज़ फ़ोन की अपनी है, इसके लिए key नहीं चाहिए" else "Uses built-in on-device TTS engine",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                HorizontalDivider(color = SurfaceBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 4: Image Generation
                SectionHeader(
                    icon = Icons.Default.Image,
                    title = if (isHindi) "4. तस्वीर निर्माण (Image Generation)" else "4. Image Generation"
                )

                Text(
                    text = if (isHindi) "इमेज AI प्रोवाइडर चुनें:" else "Select Image Provider:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderOptionButton(
                        title = "OpenAI Images",
                        selected = imageProvider == "openai",
                        onClick = { imageProvider = "openai" },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderOptionButton(
                        title = "Gemini Imagen",
                        selected = imageProvider == "gemini",
                        onClick = { imageProvider = "gemini" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (imageProvider == "openai") {
                    Text(
                        text = if (isHindi) "OpenAI इमेज मॉडल:" else "OpenAI Image Model:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    OutlinedTextField(
                        value = openaiImageModel,
                        onValueChange = { openaiImageModel = it },
                        placeholder = { Text("dall-e-3") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .testTag("openai_image_model_field")
                    )
                    Text(
                        text = "check the provider's docs for the current model name",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        text = if (isHindi) "Gemini Imagen मॉडल:" else "Gemini Imagen Model:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    OutlinedTextField(
                        value = geminiImageModel,
                        onValueChange = { geminiImageModel = it },
                        placeholder = { Text("imagen-3.0-generate-002") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .testTag("gemini_image_model_field")
                    )
                    Text(
                        text = "check the provider's docs for the current model name",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextSecondary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "Images are created by the selected AI provider and may cost money on your API account.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = CyanAccent),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = AppConfig(
                                provider = provider,
                                claudeApiKey = claudeKey.trim(),
                                claudeModel = claudeModel.trim().ifBlank { "claude-sonnet-5-5" },
                                geminiApiKey = geminiKey.trim(),
                                geminiModel = geminiModel.trim().ifBlank { "gemini-3.8-flash" },
                                openaiApiKey = openaiKey.trim(),
                                openaiModel = openaiModel.trim().ifBlank { "gpt-4o-mini" },
                                searchProvider = searchProvider,
                                searchApiKey = searchKey.trim(),
                                speakAnswers = speakAnswers,
                                imageProvider = imageProvider,
                                openaiImageModel = openaiImageModel.trim().ifBlank { "dall-e-3" },
                                geminiImageModel = geminiImageModel.trim().ifBlank { "imagen-3.0-generate-002" }
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_settings_button")
                    ) {
                        Text(
                            text = if (isHindi) "सेव करें" else "Save Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_settings_button")
                    ) {
                        Text(
                            text = if (isHindi) "बंद करें" else "Close",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun ProviderOptionButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrimaryBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                if (selected) PrimaryBlue else SurfaceBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) CyanAccent else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
