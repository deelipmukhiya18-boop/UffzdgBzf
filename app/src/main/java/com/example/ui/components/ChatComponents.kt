package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import com.example.data.model.ToolCallInfo
import com.example.data.model.ToolStatus
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryGlow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WarningAmber

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.tools.ImageStorageHelper

@Composable
fun MessageItem(message: ChatMessage) {
    if (!message.imageBase64.isNullOrBlank() || !message.imageFilePath.isNullOrBlank()) {
        GeneratedChatImageBubble(message = message)
    } else {
        when (message.role) {
            MessageRole.USER -> UserBubble(text = message.content)
            MessageRole.ASSISTANT -> AssistantBubble(text = message.content)
            MessageRole.TOOL -> {
                message.toolCall?.let { toolInfo ->
                    ToolCallBadge(toolInfo = toolInfo)
                } ?: AssistantBubble(text = message.content)
            }
            MessageRole.SYSTEM -> {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GeneratedChatImageBubble(message: ChatMessage) {
    val context = LocalContext.current
    val bitmap: Bitmap? = remember(message.imageBase64, message.imageFilePath) {
        if (!message.imageBase64.isNullOrBlank()) {
            ImageStorageHelper.decodeBase64ToBitmap(message.imageBase64)
        } else if (!message.imageFilePath.isNullOrBlank()) {
            BitmapFactory.decodeFile(message.imageFilePath)
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.2f))
                .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Assistant",
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(10.dp)
                .testTag("assistant_image_bubble")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated AI Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = ImageStorageHelper.saveToGallery(
                                    context = context,
                                    bitmap = bitmap,
                                    title = "Mamta_${System.currentTimeMillis()}"
                                )
                                Toast.makeText(
                                    context,
                                    if (uri != null) "Saved to Pictures/Mamta in Gallery!" else "Saved locally.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_image_save_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                ImageStorageHelper.shareImage(context, bitmap, message.content)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_image_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp)
                        }
                    }
                } else {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun UserBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryBlue, PrimaryGlow)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("user_message_bubble")
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    fontSize = 15.sp
                )
            )
        }
    }
}

@Composable
fun AssistantBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.2f))
                .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Assistant",
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("assistant_message_bubble")
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 21.sp,
                    fontSize = 15.sp
                )
            )
        }
    }
}

@Composable
fun ToolCallBadge(toolInfo: ToolCallInfo) {
    var expanded by remember { mutableStateOf(false) }

    val icon: ImageVector = when (toolInfo.name) {
        "call_phone" -> Icons.Default.Phone
        "send_sms" -> Icons.Default.Sms
        "find_contact" -> Icons.Default.Phone
        "open_app" -> Icons.Default.Apps
        "flashlight" -> Icons.Default.FlashlightOn
        "set_alarm", "set_timer" -> Icons.Default.Alarm
        "battery" -> Icons.Default.BatteryChargingFull
        "open_settings" -> Icons.Default.Settings
        "read_notifications", "reply_notification" -> Icons.Default.Notifications
        "read_screen" -> Icons.Default.ScreenSearchDesktop
        "tap_text", "type_text", "press_button", "scroll" -> Icons.Default.TouchApp
        "web_search" -> Icons.Default.Search
        "get_location" -> Icons.Default.LocationOn
        "save_rule" -> Icons.AutoMirrored.Filled.Rule
        "emergency_sos" -> Icons.Default.Warning
        else -> Icons.Default.Settings
    }

    val (statusColor, statusIcon) = when (toolInfo.status) {
        ToolStatus.RUNNING -> Pair(CyanAccent, null)
        ToolStatus.SUCCESS -> Pair(SuccessGreen, Icons.Default.CheckCircle)
        ToolStatus.WAITING_APPROVAL -> Pair(WarningAmber, Icons.Default.HourglassEmpty)
        ToolStatus.DENIED -> Pair(DangerRed, Icons.Default.Block)
        ToolStatus.FAILED -> Pair(DangerRed, Icons.Default.Error)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .clickable { expanded = !expanded }
                .testTag("tool_badge_${toolInfo.name}")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = toolInfo.name,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Tool: ${toolInfo.name}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = when (toolInfo.status) {
                                    ToolStatus.RUNNING -> "Running..."
                                    ToolStatus.SUCCESS -> "Completed"
                                    ToolStatus.WAITING_APPROVAL -> "Awaiting Permission"
                                    ToolStatus.DENIED -> "Action Denied"
                                    ToolStatus.FAILED -> "Execution Failed"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (toolInfo.status == ToolStatus.RUNNING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CyanAccent
                            )
                        } else if (statusIcon != null) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = expanded || !toolInfo.result.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (toolInfo.arguments.isNotEmpty()) {
                            Text(
                                text = "Inputs: ${toolInfo.arguments}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (!toolInfo.result.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = toolInfo.result,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(isHindi: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(CyanAccent.copy(alpha = 0.3f))
                .border(1.dp, CyanAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Thinking",
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = CyanAccent
                )
                Text(
                    text = if (isHindi) "सोच रहा हूँ..." else "Thinking...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SuggestionChipsRow(
    isHindi: Boolean,
    onChipClick: (String) -> Unit
) {
    val chips = if (isHindi) {
        listOf(
            "🌐 आज की ताज़ा खबर",
            "🌦️ आज का मौसम कैसा है?",
            "📈 सोने का भाव क्या है?",
            "🔦 टॉर्च चालू करो",
            "🔋 बैटरी कितनी है?",
            "⏰ 7 बजे का अलार्म लगाओ",
            "📱 YouTube खोलो",
            "🔔 नोटिफ़िकेशन पढ़ो",
            "♿ स्क्रीन पढ़ो",
            "🔊 वॉल्यूम 80% करो",
            "📞 198 पर कॉल करो"
        )
    } else {
        listOf(
            "🌐 Latest news today",
            "🌦️ How is the weather today?",
            "📈 Gold price today",
            "🔦 Turn on flashlight",
            "🔋 Check battery",
            "⏰ Set alarm for 7:00 AM",
            "📱 Open YouTube",
            "🔔 Check notifications",
            "♿ Read my screen",
            "🔊 Set volume to 80%",
            "📞 Call 198"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { chipText ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onChipClick(chipText) }
                    .testTag("suggestion_chip_$chipText")
            ) {
                Text(
                    text = chipText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
