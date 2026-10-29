package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.FrontCameraCapturer
import com.example.service.TouchGuardService
import com.example.service.TouchGuardStatus
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TouchGuardScreen(
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val guardStatus by TouchGuardService.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var photoList by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedPhotoForPreview by remember { mutableStateOf<File?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            startTouchGuardService(context)
        }
    }

    fun refreshPhotos() {
        scope.launch(Dispatchers.IO) {
            val photos = FrontCameraCapturer.getSavedPhotos(context)
            withContext(Dispatchers.Main) {
                photoList = photos
            }
        }
    }

    LaunchedEffect(guardStatus) {
        refreshPhotos()
    }

    val isArmedOrArming = guardStatus !is TouchGuardStatus.Disarmed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("touch_guard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanAccent
                    )
                }
                Text(
                    text = if (isHindi) "🛡️ टच गार्ड (एंटी-थेफ्ट)" else "🛡️ Touch Guard (Anti-Theft)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Arming Master Switch Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "टच गार्ड सक्षम करें (Enable Touch Guard)" else "Enable Touch Guard",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi)
                            "डिफ़ॉल्ट रूप से बंद। चालू करने पर 10 सेकंड बाद फ़ोन की सुरक्षा शुरू होती है।"
                        else
                            "OFF by default. 10-second delay allows you to put the phone down.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = isArmedOrArming,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                startTouchGuardService(context)
                            }
                        } else {
                            disarmTouchGuard(context)
                        }
                    },
                    modifier = Modifier.testTag("enable_touch_guard_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanAccent
                    )
                )
            }
        }

        // Guard Status Card
        GuardStatusCard(
            status = guardStatus,
            isHindi = isHindi,
            onDisarm = { disarmTouchGuard(context) }
        )

        // Instructions Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "सुरक्षा कैसे काम करती है:" else "How Touch Guard protects your phone:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint(
                    text = if (isHindi)
                        "1. ऑन करने के 10 सेकंड बाद सुरक्षा एक्टिव होगी, ताकि आप फ़ोन रख सकें।"
                    else
                        "1. 10-second delay gives you time to set the phone down safely."
                )
                BulletPoint(
                    text = if (isHindi)
                        "2. फ़ोन उठाने (मोशन), स्क्रीन ऑन करने, या चार्जर निकालने पर तुरंत अलार्म बजेगा।"
                    else
                        "2. Triggers on: Phone picked up (motion), screen turned on, or charger pulled out."
                )
                BulletPoint(
                    text = if (isHindi)
                        "3. फ्रंट कैमरे से चुपके से फ़ोटो लेकर केवल ऐप के प्राइवेट स्टोरेज में सुरक्षित होगी।"
                    else
                        "3. Takes a silent front-camera photo saved ONLY in app-private storage."
                )
                BulletPoint(
                    text = if (isHindi)
                        "4. ज़ोरदार सायरन और हिंदी चेतावनी बोलेगा: \"यह फ़ोन किसी और का है, कृपया इसे छोड़ दें\"।"
                    else
                        "4. Speaks loud Hindi warning and sounds high-volume siren on Alarm stream."
                )
            }
        }

        // Intrusion Photos Section
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
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isHindi)
                        "घुसपैठ की तस्वीरें (${photoList.size})"
                    else
                        "Intrusion Photos (${photoList.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (photoList.isNotEmpty()) {
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            photoList.forEach { it.delete() }
                            refreshPhotos()
                        }
                    },
                    modifier = Modifier.testTag("delete_all_photos_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Delete All Photos",
                        tint = DangerRed
                    )
                }
            }
        }

        if (photoList.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "कोई घुसपैठ नहीं हुई। फ़ोन सुरक्षित है।" else "No intrusion attempts detected. Phone is secure.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                photoList.forEach { photoFile ->
                    PhotoItemRow(
                        file = photoFile,
                        onView = { selectedPhotoForPreview = photoFile },
                        onDelete = {
                            photoFile.delete()
                            refreshPhotos()
                        }
                    )
                }
            }
        }
    }

    // Full photo preview dialog
    selectedPhotoForPreview?.let { file ->
        PhotoPreviewDialog(
            file = file,
            onDismiss = { selectedPhotoForPreview = null },
            onDelete = {
                file.delete()
                selectedPhotoForPreview = null
                refreshPhotos()
            }
        )
    }
}

@Composable
fun GuardStatusCard(
    status: TouchGuardStatus,
    isHindi: Boolean,
    onDisarm: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when (status) {
            is TouchGuardStatus.Disarmed -> SurfaceBorder
            is TouchGuardStatus.Arming -> WarningAmber
            is TouchGuardStatus.Armed -> SuccessGreen
            is TouchGuardStatus.Triggered -> DangerRed
        },
        label = "status_border"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (status) {
                is TouchGuardStatus.Disarmed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                        Column {
                            Text(
                                text = if (isHindi) "टच गार्ड बंद है (Disarmed)" else "Touch Guard Disarmed",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isHindi) "सुरक्षा चालू करने के लिए ऊपर स्विच ऑन करें" else "Turn on switch above to activate anti-theft guard",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                is TouchGuardStatus.Arming -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WarningAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { status.secondsLeft / 10f },
                                modifier = Modifier.size(36.dp),
                                color = WarningAmber,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "${status.secondsLeft}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = WarningAmber
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "⏳ तैयार हो रहा है (${status.secondsLeft}s)" else "⏳ Arming in ${status.secondsLeft} seconds...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = WarningAmber
                            )
                            Text(
                                text = if (isHindi) "फ़ोन नीचे रख दें। 10 सेकंड बाद सुरक्षा चालू होगी।" else "Put the phone down now. Arming countdown active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = onDisarm,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.testTag("disarm_button")
                        ) {
                            Text(if (isHindi) "रद्द करें" else "Disarm")
                        }
                    }
                }

                is TouchGuardStatus.Armed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SuccessGreen
                                )
                            }
                            Column {
                                Text(
                                    text = if (isHindi) "🛡️ सुरक्षा सक्रिय है (ARMED)" else "🛡️ Guard is Active & ARMED",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessGreen
                                )
                                Text(
                                    text = if (isHindi) "फ़ोन उठाने या स्क्रीन ऑन करने पर अलार्म बजेगा" else "Watching accelerometer, screen & charger",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = onDisarm,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.testTag("disarm_button")
                        ) {
                            Text(if (isHindi) "डिसआर्म" else "Disarm")
                        }
                    }
                }

                is TouchGuardStatus.Triggered -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = DangerRed
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "🚨 घुसपैठ पकड़ी गई!" else "🚨 INTRUSION DETECTED!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DangerRed
                                )
                                Text(
                                    text = status.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = if (isHindi)
                                "सायरन बज रहा है और चेतावनी बोली जा रही है। फ्रंट कैमरा फ़ोटो ले ली गई है।"
                            else
                                "Loud alarm siren is sounding. Front camera intrusion photo captured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber
                        )

                        Button(
                            onClick = onDisarm,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("disarm_button")
                        ) {
                            Text(
                                text = if (isHindi) "🔕 अलार्म बंद करें (Disarm Alarm)" else "🔕 Disarm Alarm",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoItemRow(
    file: File,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(file.absolutePath) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    val formattedDate = remember(file.lastModified()) {
        try {
            SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(file.lastModified()))
        } catch (_: Exception) {
            file.name
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Intrusion thumbnail",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Size: ${(file.length() / 1024)} KB • Private storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_photo_${file.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Photo",
                    tint = DangerRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    file: File,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(file.absolutePath) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    val formattedDate = remember(file.lastModified()) {
        try {
            SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(file.lastModified()))
        } catch (_: Exception) {
            file.name
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full Intrusion Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Unable to load image bitmap",
                        color = DangerRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = CyanAccent, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun startTouchGuardService(context: Context) {
    val intent = Intent(context, TouchGuardService::class.java).apply {
        action = TouchGuardService.ACTION_START_ARM
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun disarmTouchGuard(context: Context) {
    val intent = Intent(context, TouchGuardService::class.java).apply {
        action = TouchGuardService.ACTION_DISARM
    }
    context.startService(intent)
}
