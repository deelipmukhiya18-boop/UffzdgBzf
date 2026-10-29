package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.service.EventService
import com.example.service.NotifService
import com.example.service.PhoneService
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import org.json.JSONArray
import org.json.JSONObject

import android.app.Activity
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import com.example.tools.ScreenWakeHelper
import com.example.ui.components.sevakendra.SevaKendraScreen
import com.example.ui.viewmodel.AssistantViewModel

enum class MenuSection {
    HOME,
    NEURAL_LENS,
    CREATE_IMAGE,
    SEVA_KENDRA,
    STUDY,
    JOURNAL,
    DOCUMENTS,
    MARKETS,
    MUSIC,
    RULES,
    EVENTS,
    PERMISSIONS,
    SOS,
    TOUCH_GUARD,
    SCREEN_LOCK
}

@Composable
fun AppMenuDialog(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    rules: List<String>,
    onAddRule: (String) -> Unit,
    onDeleteRule: (Int) -> Unit,
    onRequestPermission: (String) -> Unit,
    onTriggerSos: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentSection by remember { mutableStateOf(MenuSection.HOME) }

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
                // Header with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentSection != MenuSection.HOME) {
                            IconButton(onClick = { currentSection = MenuSection.HOME }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CyanAccent
                                )
                            }
                        }
                        Text(
                            text = when (currentSection) {
                                MenuSection.HOME -> if (isHindi) "☰ मुख्य मेन्यू" else "☰ Main Menu"
                                MenuSection.NEURAL_LENS -> if (isHindi) "📷 Neural Lens (विज़न)" else "📷 Neural Lens"
                                MenuSection.CREATE_IMAGE -> if (isHindi) "🎨 तस्वीर बनाएं" else "🎨 Create Image"
                                MenuSection.SEVA_KENDRA -> if (isHindi) "🏢 डिजिटल सेवा केंद्र" else "🏢 Seva Kendra"
                                MenuSection.STUDY -> if (isHindi) "📚 पढ़ाई व व्हाइटबोर्ड" else "📚 Study / Whiteboard"
                                MenuSection.JOURNAL -> if (isHindi) "📔 दैनिक जर्नल (डायरी)" else "📔 Daily Journal"
                                MenuSection.DOCUMENTS -> if (isHindi) "📄 दस्तावेज़ विश्लेषण" else "📄 Documents"
                                MenuSection.MARKETS -> if (isHindi) "📈 बाज़ार भाव (Markets)" else "📈 Markets"
                                MenuSection.MUSIC -> if (isHindi) "🎵 संगीत (Music)" else "🎵 Music Player"
                                MenuSection.RULES -> if (isHindi) "📏 Mamta Rules" else "📏 Assistant Rules"
                                MenuSection.EVENTS -> if (isHindi) "📢 घटनाएँ (बोलकर बताना)" else "📢 Voice Announcements"
                                MenuSection.PERMISSIONS -> if (isHindi) "🔐 अनुमतियाँ (Permissions)" else "🔐 Permissions"
                                MenuSection.SOS -> if (isHindi) "🚨 Emergency SOS" else "🚨 Emergency SOS"
                                MenuSection.TOUCH_GUARD -> if (isHindi) "🛡️ Touch Guard" else "🛡️ Touch Guard"
                                MenuSection.SCREEN_LOCK -> if (isHindi) "🔒 स्क्रीन लॉक व वेक" else "🔒 Screen Lock & Wake"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentSection) {
                    MenuSection.HOME -> {
                        MainMenuHome(
                            isHindi = isHindi,
                            onSelect = { currentSection = it }
                        )
                    }
                    MenuSection.NEURAL_LENS -> {
                        NeuralLensScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.CREATE_IMAGE -> {
                        CreateImageScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.SEVA_KENDRA -> {
                        SevaKendraScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.STUDY -> {
                        StudyScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.JOURNAL -> {
                        JournalScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.DOCUMENTS -> {
                        DocumentsScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.MARKETS -> {
                        MarketsScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.MUSIC -> {
                        MusicScreen(
                            viewModel = viewModel,
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.RULES -> {
                        RulesPanel(
                            isHindi = isHindi,
                            rules = rules,
                            onAddRule = onAddRule,
                            onDeleteRule = onDeleteRule
                        )
                    }
                    MenuSection.EVENTS -> {
                        EventsPanel(
                            context = context,
                            isHindi = isHindi
                        )
                    }
                    MenuSection.PERMISSIONS -> {
                        PermissionsPanel(
                            context = context,
                            isHindi = isHindi,
                            onRequestPermission = onRequestPermission
                        )
                    }
                    MenuSection.SOS -> {
                        SosPanel(
                            context = context,
                            isHindi = isHindi,
                            onTriggerSos = onTriggerSos
                        )
                    }
                    MenuSection.TOUCH_GUARD -> {
                        TouchGuardScreen(
                            isHindi = isHindi,
                            onBack = { currentSection = MenuSection.HOME }
                        )
                    }
                    MenuSection.SCREEN_LOCK -> {
                        ScreenLockPanel(
                            context = context,
                            isHindi = isHindi
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuHome(
    isHindi: Boolean,
    onSelect: (MenuSection) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isHindi) "🌟 मुख्य सुविधाएं व टूल्स" else "🌟 Features & Tools",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = CyanAccent,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        MenuCardButton(
            icon = Icons.Default.Image,
            title = if (isHindi) "🎨 Create Image (तस्वीर बनाएं)" else "🎨 Create Image",
            subtitle = if (isHindi) "OpenAI DALL-E व Gemini Imagen द्वारा पोस्टर, लोगो व फ़ोटो बनाएं" else "Generate posters, logos & photos via OpenAI or Gemini",
            tint = CyanAccent,
            onClick = { onSelect(MenuSection.CREATE_IMAGE) }
        )

        MenuCardButton(
            icon = Icons.Default.Store,
            title = if (isHindi) "🏢 Seva Kendra (डिजिटल सेवा केंद्र)" else "🏢 Seva Kendra (Digital Shop)",
            subtitle = if (isHindi) "ग्राहक, काम सूची, रेट लिस्ट, रसीद शेयरिंग व दैनिक हिसाब (Room DB)" else "Customer work list, rate list, WhatsApp receipt & daily summary",
            tint = CyanAccent,
            onClick = { onSelect(MenuSection.SEVA_KENDRA) }
        )

        MenuCardButton(
            icon = Icons.Default.School,
            title = if (isHindi) "📚 Study / Whiteboard" else "📚 Study / Whiteboard",
            subtitle = if (isHindi) "सरल हिंदी में समझें, 5 प्रश्नों का क्विज़, फ़्लैशकार्ड्स व समरी" else "AI topic teacher, 5-question quiz, flashcards & summary",
            tint = CyanAccent,
            onClick = { onSelect(MenuSection.STUDY) }
        )

        MenuCardButton(
            icon = Icons.Default.Book,
            title = if (isHindi) "📔 Journal (दैनिक डायरी)" else "📔 Daily Journal",
            subtitle = if (isHindi) "मूड के साथ स्थानीय नोट्स और AI द्वारा हफ्ते का सारांश" else "Local daily notes with mood & AI weekly summary",
            tint = SuccessGreen,
            onClick = { onSelect(MenuSection.JOURNAL) }
        )

        MenuCardButton(
            icon = Icons.Default.Description,
            title = if (isHindi) "📄 Documents (दस्तावेज़)" else "📄 Documents",
            subtitle = if (isHindi) "PDF, इमेज व टेक्स्ट फ़ाइल का सार, अनुवाद व प्रश्नोत्तर" else "Pick PDF, image or text: summarize, translate & Q&A",
            tint = PrimaryBlue,
            onClick = { onSelect(MenuSection.DOCUMENTS) }
        )

        MenuCardButton(
            icon = Icons.Default.ShowChart,
            title = if (isHindi) "📈 Markets (बाज़ार भाव)" else "📈 Markets",
            subtitle = if (isHindi) "सोना, चांदी, पेट्रोल, स्टॉक व क्रिप्टो के ताज़ा लाइव भाव" else "Gold, silver, petrol, stock & crypto live rates",
            tint = WarningAmber,
            onClick = { onSelect(MenuSection.MARKETS) }
        )

        MenuCardButton(
            icon = Icons.Default.MusicNote,
            title = if (isHindi) "🎵 Music (संगीत व गाने)" else "🎵 Music Player",
            subtitle = if (isHindi) "YouTube Music व Spotify पर गाना बजाएं (Intents द्वारा)" else "Play songs on YouTube Music or Spotify via Intents",
            tint = CyanAccent,
            onClick = { onSelect(MenuSection.MUSIC) }
        )

        HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = if (isHindi) "🛡️ सुरक्षा, सेटिंग्स व फ़ोन नियंत्रण" else "🛡️ Security & Phone Control",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        MenuCardButton(
            icon = Icons.Default.Shield,
            title = if (isHindi) "🛡️ Touch Guard (एंटी-थेफ्ट)" else "🛡️ Touch Guard (Anti-Theft)",
            subtitle = if (isHindi) "मोशन, स्क्रीन ऑन, चार्जर सेंसर, सायरन व फ्रंट फ़ोटो" else "Owner phone anti-theft siren, motion & photo",
            tint = CyanAccent,
            onClick = { onSelect(MenuSection.TOUCH_GUARD) }
        )
        MenuCardButton(
            icon = Icons.AutoMirrored.Filled.Rule,
            title = if (isHindi) "📏 Mamta Rules" else "📏 Assistant Rules",
            subtitle = if (isHindi) "हमेशा मानने वाले स्थायी नियम तय करें" else "Set permanent assistant behavior rules",
            onClick = { onSelect(MenuSection.RULES) }
        )
        MenuCardButton(
            icon = Icons.Default.Campaign,
            title = if (isHindi) "📢 घटनाएँ (बोलकर बताना)" else "📢 Spoken Event Alerts",
            subtitle = if (isHindi) "चार्जर, बैटरी, ब्लूटूथ, हेडफ़ोन की घोषणाएँ" else "Battery, charger, bluetooth & headphone speech",
            onClick = { onSelect(MenuSection.EVENTS) }
        )
        MenuCardButton(
            icon = Icons.Default.Lock,
            title = if (isHindi) "🔒 स्क्रीन लॉक व वेक (Screen Lock)" else "🔒 Screen Lock & Wake",
            subtitle = if (isHindi) "ममता को ज़रूरत पर स्क्रीन चालू करने की सेटिंग" else "Wake screen when Mamta needs it (native unlock)",
            tint = PrimaryBlue,
            onClick = { onSelect(MenuSection.SCREEN_LOCK) }
        )
        MenuCardButton(
            icon = Icons.Default.Lock,
            title = if (isHindi) "🔐 अनुमतियाँ (Permissions)" else "🔐 Permissions Manager",
            subtitle = if (isHindi) "डिफ़ॉल्ट असिस्टेंट, कॉल, SMS, लोकेशन और एक्सेसिबिलिटी" else "Default assistant, phone, SMS & accessibility access",
            onClick = { onSelect(MenuSection.PERMISSIONS) }
        )
        MenuCardButton(
            icon = Icons.Default.Warning,
            title = if (isHindi) "🚨 Emergency SOS" else "🚨 Emergency SOS",
            subtitle = if (isHindi) "आपातकालीन नंबर और लोकेशन SMS सेटअप" else "Configure SOS contacts and location dispatch",
            tint = DangerRed,
            onClick = { onSelect(MenuSection.SOS) }
        )
    }
}

@Composable
fun MenuCardButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color = CyanAccent,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
fun RulesPanel(
    isHindi: Boolean,
    rules: List<String>,
    onAddRule: (String) -> Unit,
    onDeleteRule: (Int) -> Unit
) {
    var newRuleText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isHindi)
                "ऐसे नियम जो Mamta हर बार मानेगी, जैसे \"जवाब छोटे रखना\" या \"मुझे सर कहना\"। चैट में \"अब से हमेशा...\" बोलकर भी जुड़वा सकते हैं।"
            else
                "Permanent rules that Mamta adheres to in every response (e.g. 'Keep answers short', 'Address me as Boss'). You can also dictate 'From now on always...' in chat.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newRuleText,
                onValueChange = { newRuleText = it },
                placeholder = { Text(if (isHindi) "नया नियम लिखिए..." else "Type new rule...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newRuleText.isNotBlank()) {
                        onAddRule(newRuleText.trim())
                        newRuleText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        if (rules.isEmpty()) {
            Text(
                text = if (isHindi) "अभी कोई कस्टम नियम नहीं है। Mamta अपने डिफ़ॉल्ट तरीके से काम कर रही है।" else "No custom rules saved yet.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            rules.forEachIndexed { index, rule ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onDeleteRule(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventsPanel(
    context: Context,
    isHindi: Boolean
) {
    val prefs = remember { context.getSharedPreferences("mamta_events", Context.MODE_PRIVATE) }
    var serviceRunning by remember { mutableStateOf(EventService.isRunning) }

    val eventToggles = remember {
        mutableStateMapOf<String, Boolean>().apply {
            val keys = listOf(
                "master", "speak_silent", "power_in", "power_out", "full", "low",
                "crit", "saver_on", "saver_off", "hp_in", "hp_out", "bt_in",
                "bt_out", "air_on", "air_off", "app_in", "app_out"
            )
            for (k in keys) {
                val def = !(k == "saver_on" || k == "saver_off" || k == "speak_silent")
                put(k, prefs.getBoolean(k, def))
            }
        }
    }

    fun savePref(key: String, value: Boolean) {
        eventToggles[key] = value
        prefs.edit().putBoolean(key, value).apply()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isHindi)
                "फ़ोन की विभिन्न घटनाओं पर Mamta अपने आप हिंदी में बोलकर बताती है। सेवा चालू होने पर बैकग्राउंड नोटिफ़िकेशन रहेगा।"
            else
                "Mamta will announce phone hardware events aloud in Hindi via TTS. Foreground service notification runs while active.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        // Master Service Control
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isHindi) "इवेंट वॉइस सेवा" else "Event Voice Service",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (serviceRunning) "✅ सेवा चालू है" else "⛔ सेवा बंद है",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (serviceRunning) SuccessGreen else WarningAmber
                        )
                    )
                }

                Button(
                    onClick = {
                        val intent = Intent(context, EventService::class.java)
                        if (serviceRunning) {
                            context.stopService(intent)
                            serviceRunning = false
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            serviceRunning = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (serviceRunning) DangerRed else SuccessGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (serviceRunning) "बंद करें" else "चालू करें")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        EventCheckboxItem("master", "📢 बोलकर बताओ (मास्टर स्विच)", eventToggles["master"] ?: true) { savePref("master", it) }
        EventCheckboxItem("speak_silent", "🔕 साइलेंट मोड में भी बोलो", eventToggles["speak_silent"] ?: false) { savePref("speak_silent", it) }

        HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))
        Text("🔋 बैटरी और चार्जर", fontWeight = FontWeight.Bold, color = CyanAccent)

        EventCheckboxItem("power_in", "चार्जर लगा", eventToggles["power_in"] ?: true) { savePref("power_in", it) }
        EventCheckboxItem("power_out", "चार्जर निकला", eventToggles["power_out"] ?: true) { savePref("power_out", it) }
        EventCheckboxItem("full", "बैटरी फुल", eventToggles["full"] ?: true) { savePref("full", it) }
        EventCheckboxItem("low", "बैटरी 20% से कम", eventToggles["low"] ?: true) { savePref("low", it) }
        EventCheckboxItem("crit", "बैटरी 10% से कम (गंभीर)", eventToggles["crit"] ?: true) { savePref("crit", it) }
        EventCheckboxItem("saver_on", "बैटरी सेवर चालू", eventToggles["saver_on"] ?: false) { savePref("saver_on", it) }
        EventCheckboxItem("saver_off", "बैटरी सेवर बंद", eventToggles["saver_off"] ?: false) { savePref("saver_off", it) }

        HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))
        Text("🎧 हेडफ़ोन और ब्लूटूथ", fontWeight = FontWeight.Bold, color = CyanAccent)

        EventCheckboxItem("hp_in", "हेडफ़ोन लगे", eventToggles["hp_in"] ?: true) { savePref("hp_in", it) }
        EventCheckboxItem("hp_out", "हेडफ़ोन निकले", eventToggles["hp_out"] ?: true) { savePref("hp_out", it) }
        EventCheckboxItem("bt_in", "ब्लूटूथ जुड़ा", eventToggles["bt_in"] ?: true) { savePref("bt_in", it) }
        EventCheckboxItem("bt_out", "ब्लूटूथ हटा", eventToggles["bt_out"] ?: true) { savePref("bt_out", it) }

        HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))
        Text("📱 नेटवर्क और सिस्टम", fontWeight = FontWeight.Bold, color = CyanAccent)

        EventCheckboxItem("air_on", "एयरप्लेन मोड चालू", eventToggles["air_on"] ?: true) { savePref("air_on", it) }
        EventCheckboxItem("air_off", "एयरप्लेन मोड बंद", eventToggles["air_off"] ?: true) { savePref("air_off", it) }
        EventCheckboxItem("app_in", "नया ऐप इंस्टॉल हुआ", eventToggles["app_in"] ?: true) { savePref("app_in", it) }
        EventCheckboxItem("app_out", "ऐप हटाया गया", eventToggles["app_out"] ?: true) { savePref("app_out", it) }
    }
}

@Composable
fun EventCheckboxItem(
    key: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
        )
    }
}

@Composable
fun PermissionsPanel(
    context: Context,
    isHindi: Boolean,
    onRequestPermission: (String) -> Unit
) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isBatteryOptimizedIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    } else true

    val perms = listOf(
        Triple("CALL_PHONE", "फ़ोन कॉल (Phone Calls)", Manifest.permission.CALL_PHONE),
        Triple("SEND_SMS", "SMS संदेश", Manifest.permission.SEND_SMS),
        Triple("READ_CONTACTS", "कॉन्टैक्ट (Contacts)", Manifest.permission.READ_CONTACTS),
        Triple("ACCESS_FINE_LOCATION", "GPS लोकेशन", Manifest.permission.ACCESS_FINE_LOCATION),
        Triple("RECORD_AUDIO", "माइक्रोफ़ोन (वॉइस इनपुट)", Manifest.permission.RECORD_AUDIO)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (isHindi) "सिर्फ़ वही अनुमति दें जो आपको चाहिए। Android सेटिंग से कभी भी वापस ले सकते हैं।" else "Grant permissions needed for your desired features.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        // Default Assistant Button
        val isDefaultAssistant = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT) == true
            } else false
        }

        PermissionCard(
            title = if (isHindi) "🌟 डिफ़ॉल्ट डिजिटल असिस्टेंट (Mamta Default Assistant)" else "🌟 Default Digital Assistant (Mamta)",
            statusGranted = isDefaultAssistant,
            buttonText = if (isHindi) "डिफ़ॉल्ट बनाएं" else "Make Default",
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                    if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT)
                        context.startActivity(intent)
                        return@PermissionCard
                    }
                }
                try {
                    context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            }
        )

        perms.forEach { (id, label, manifestPerm) ->
            val granted = ContextCompat.checkSelfPermission(context, manifestPerm) == PackageManager.PERMISSION_GRANTED
            PermissionCard(
                title = label,
                statusGranted = granted,
                buttonText = if (isHindi) "दें" else "Grant",
                onGrant = { onRequestPermission(manifestPerm) }
            )
        }

        // Accessibility Service
        val accGranted = PhoneService.isActive()
        PermissionCard(
            title = "♿ Accessibility सेवा (स्क्रीन पढ़ना/टैप)",
            statusGranted = accGranted,
            buttonText = if (isHindi) "दें" else "Grant",
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        )

        // Notification Access
        val notifGranted = NotifService.isActive()
        PermissionCard(
            title = "🔔 Notification Access (नोटिफ़िकेशन पढ़ना)",
            statusGranted = notifGranted,
            buttonText = if (isHindi) "दें" else "Grant",
            onGrant = {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        )

        // Battery Optimization
        PermissionCard(
            title = "🔋 बैटरी ऑप्टिमाइज़ेशन बंद (Background Running)",
            statusGranted = isBatteryOptimizedIgnored,
            buttonText = if (isHindi) "दें" else "Grant",
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    statusGranted: Boolean,
    buttonText: String = "दें",
    onGrant: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )

            if (statusGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "सक्रिय",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(buttonText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ScreenLockPanel(
    context: Context,
    isHindi: Boolean
) {
    var wakeScreenEnabled by remember {
        mutableStateOf(ScreenWakeHelper.isWakeScreenEnabled(context))
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "ममता को ज़रूरत होने पर स्क्रीन चालू करें" else "Wake the screen when Mamta needs it",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi)
                            "डिफ़ॉल्ट रूप से चालू (ON)। जब ममता कोई कार्य करती है तो स्क्रीन चालू होकर सिस्टम का अनलॉक प्रॉम्प्ट दिखाती है।"
                        else
                            "ON by default. Wakes screen and shows Android's native unlock prompt for protected actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = wakeScreenEnabled,
                    onCheckedChange = { checked ->
                        wakeScreenEnabled = checked
                        ScreenWakeHelper.setWakeScreenEnabled(context, checked)
                    },
                    modifier = Modifier.testTag("wake_screen_when_needed_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanAccent
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isHindi) "गोपनीयता व सुरक्षा गारंटी" else "Privacy & Security Guarantee",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isHindi)
                        "ममता आपके फ़ोन का PIN, पैटर्न या पासवर्ड कभी भी स्टोर या सेव नहीं करती है। जब भी स्क्रीन अनलॉक की ज़रूरत होती है, ममता सिर्फ़ Android के मूल सिस्टम डायलॉग को कॉल करती है।"
                    else
                        "Mamta NEVER stores, requests, or handles your phone's PIN, pattern, or password anywhere. All authentication is strictly delegated to Android's own native unlock dialog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (context is Activity) {
                    Button(
                        onClick = {
                            ScreenWakeHelper.wakeScreenAndShowUnlockPrompt(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_screen_wake_button")
                    ) {
                        Text(
                            text = if (isHindi) "स्क्रीन वेक और सिस्टम अनलॉक टेस्ट करें" else "Test Screen Wake & Native Unlock",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SosPanel(
    context: Context,
    isHindi: Boolean,
    onTriggerSos: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("mamta_sos_prefs", Context.MODE_PRIVATE) }
    var contactsJson by remember { mutableStateOf(prefs.getString("sos_contacts", "[]") ?: "[]") }
    var countryCode by remember { mutableStateOf(prefs.getString("country_code", "+91") ?: "+91") }

    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }

    val contactsList = remember(contactsJson) {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val arr = JSONArray(contactsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Pair(obj.optString("name", "Contact"), obj.optString("number", "")))
            }
        } catch (_: Exception) {}
        list
    }

    fun saveContacts(newList: List<Pair<String, String>>) {
        val arr = JSONArray()
        for ((name, num) in newList) {
            val obj = JSONObject().apply {
                put("name", name)
                put("number", num)
            }
            arr.put(obj)
        }
        val str = arr.toString()
        contactsJson = str
        prefs.edit().putString("sos_contacts", str).apply()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isHindi)
                "आपात स्थिति में इन नंबरों पर Google Maps लोकेशन के साथ SMS भेजा जाएगा और पहले नंबर पर कॉल लगाई जाएगी।"
            else
                "In an emergency, SOS SMS with your GPS location link will be dispatched to these contacts, and the primary contact will be dialed automatically.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        // Country code
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("देश कोड (Country Code):", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            OutlinedTextField(
                value = countryCode,
                onValueChange = {
                    countryCode = it
                    prefs.edit().putString("country_code", it).apply()
                },
                singleLine = true,
                modifier = Modifier.width(90.dp)
            )
        }

        // Add contact form
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("+ आपातकालीन संपर्क जोड़ें", fontWeight = FontWeight.Bold, color = CyanAccent)
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("नाम (e.g. Papa, Mom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newNumber,
                    onValueChange = { newNumber = it },
                    placeholder = { Text("फ़ोन नंबर (e.g. 9876543210)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newNumber.isNotBlank()) {
                            var num = newNumber.trim().replace(Regex("[^0-9+]"), "")
                            if (!num.startsWith("+") && num.length == 10) {
                                num = "$countryCode$num"
                            }
                            val updated = contactsList + Pair(newName.ifBlank { "SOS Contact" }, num)
                            saveContacts(updated)
                            newName = ""
                            newNumber = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ संपर्क जोड़ें")
                }
            }
        }

        // Contacts list
        if (contactsList.isEmpty()) {
            Text("अभी कोई SOS संपर्क नहीं जोड़ा गया है।", color = TextSecondary)
        } else {
            contactsList.forEachIndexed { idx, (name, num) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(num, color = TextSecondary, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            val updated = contactsList.toMutableList().apply { removeAt(idx) }
                            saveContacts(updated)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SOS trigger test button
        Button(
            onClick = onTriggerSos,
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("🚨 इमरजेंसी SOS भेजें (Test Emergency SOS)", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
