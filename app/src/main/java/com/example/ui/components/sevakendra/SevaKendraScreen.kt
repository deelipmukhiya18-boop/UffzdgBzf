package com.example.ui.components.sevakendra

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.sevakendra.SevaCustomerWork
import com.example.data.sevakendra.SevaRateItem
import com.example.data.sevakendra.SevaRepository
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SevaTab(val titleEn: String, val titleHi: String) {
    WORKS("Works", "ग्राहक व कार्य"),
    RATES("Rate List", "रेट लिस्ट"),
    SUMMARY("Daily Summary", "दैनिक हिसाब"),
    SETTINGS("Shop & Backup", "दुकान व बैकअप")
}

@Composable
fun SevaKendraScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { viewModel.sevaRepository }

    var selectedTab by remember { mutableStateOf(SevaTab.WORKS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showAddWorkDialog by remember { mutableStateOf(false) }
    var editingWork by remember { mutableStateOf<SevaCustomerWork?>(null) }
    var workToDelete by remember { mutableStateOf<SevaCustomerWork?>(null) }

    var showAddRateDialog by remember { mutableStateOf(false) }
    var editingRate by remember { mutableStateOf<SevaRateItem?>(null) }
    var rateToDelete by remember { mutableStateOf<SevaRateItem?>(null) }

    // Ask AI modal for guidelines
    var aiGuideService by remember { mutableStateOf<String?>(null) }
    var aiGuideLoading by remember { mutableStateOf(false) }
    var aiGuideText by remember { mutableStateOf<String?>(null) }

    val allWorks by repository.allWorks.collectAsState(initial = emptyList())
    val allRates by repository.allRates.collectAsState(initial = emptyList())

    // Initial default rate & reminder checks
    LaunchedEffect(Unit) {
        repository.ensureDefaultRates()
        repository.checkAndNotifyDueToday()
    }

    // Filtered works list
    val filteredWorks = remember(allWorks, searchQuery, selectedStatusFilter) {
        allWorks.filter { work ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                work.customerName.contains(searchQuery, ignoreCase = true) ||
                        work.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                        work.serviceName.contains(searchQuery, ignoreCase = true)
            }
            val matchesStatus = selectedStatusFilter == null || work.status == selectedStatusFilter
            matchesQuery && matchesStatus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("seva_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "सेवा केंद्र (Seva Kendra)" else "Seva Kendra",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = repository.getShopName().ifBlank { "Digital Seva Shop" },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }

                if (selectedTab == SevaTab.WORKS) {
                    Button(
                        onClick = { showAddWorkDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_work_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "नया काम" else "Add Work", fontSize = 13.sp)
                    }
                }
            }
        }

        // Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SevaTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = if (isHindi) tab.titleHi else tab.titleEn,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) CyanAccent else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("seva_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Body Content by Tab
        when (selectedTab) {
            SevaTab.WORKS -> {
                WorksListContent(
                    isHindi = isHindi,
                    works = filteredWorks,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedStatusFilter = selectedStatusFilter,
                    onSelectStatusFilter = { selectedStatusFilter = it },
                    onEditWork = { editingWork = it },
                    onDeleteWork = { workToDelete = it },
                    onShareReceipt = { repository.shareReceipt(it) },
                    onAskAiGuide = { serviceName ->
                        aiGuideService = serviceName
                        aiGuideLoading = true
                        aiGuideText = null
                        scope.launch {
                            val prompt = "Provide a clean, bulleted checklist and requirements for '$serviceName' service in an Indian digital service center (Seva Kendra / CSC). Explain required documents, eligibility, and steps in simple Hindi/English. Do not ask for personal details."
                            val guide = viewModel.askAiOneShot(prompt)
                            aiGuideText = guide
                            aiGuideLoading = false
                        }
                    }
                )
            }
            SevaTab.RATES -> {
                RateListContent(
                    isHindi = isHindi,
                    rates = allRates,
                    onAddRate = { showAddRateDialog = true },
                    onEditRate = { editingRate = it },
                    onDeleteRate = { rateToDelete = it }
                )
            }
            SevaTab.SUMMARY -> {
                DailySummaryContent(
                    isHindi = isHindi,
                    allWorks = allWorks,
                    onCheckDueReminders = {
                        scope.launch {
                            val count = repository.checkAndNotifyDueToday()
                            val msg = if (count > 0) {
                                if (isHindi) "आज $count कार्य की तारीख है! नोटिफ़िकेशन भेजा गया।" else "$count works due today! Reminder notification posted."
                            } else {
                                if (isHindi) "आज किसी भी लंबित कार्य की अंतिम तारीख नहीं है।" else "No pending works due today."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            SevaTab.SETTINGS -> {
                ShopSettingsContent(
                    context = context,
                    isHindi = isHindi,
                    repository = repository
                )
            }
        }
    }

    // Add / Edit Work Dialog
    if (showAddWorkDialog || editingWork != null) {
        AddEditWorkDialog(
            isHindi = isHindi,
            work = editingWork,
            rateList = allRates,
            onDismiss = {
                showAddWorkDialog = false
                editingWork = null
            },
            onSave = { savedWork ->
                scope.launch {
                    if (editingWork != null) {
                        repository.updateWork(savedWork)
                    } else {
                        repository.insertWork(savedWork)
                    }
                    showAddWorkDialog = false
                    editingWork = null
                }
            }
        )
    }

    // Delete Work Confirmation Dialog
    workToDelete?.let { work ->
        AlertDialog(
            onDismissRequest = { workToDelete = null },
            title = { Text(if (isHindi) "कार्य हटाएं?" else "Delete Work?") },
            text = {
                Text(
                    if (isHindi)
                        "क्या आप '${work.customerName} - ${work.serviceName}' को हटाना चाहते हैं?"
                    else
                        "Are you sure you want to delete '${work.customerName} - ${work.serviceName}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteWork(work)
                            workToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_delete_work_button")
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { workToDelete = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Add / Edit Rate Dialog
    if (showAddRateDialog || editingRate != null) {
        AddEditRateDialog(
            isHindi = isHindi,
            rate = editingRate,
            onDismiss = {
                showAddRateDialog = false
                editingRate = null
            },
            onSave = { savedRate ->
                scope.launch {
                    if (editingRate != null) {
                        repository.updateRate(savedRate)
                    } else {
                        repository.insertRate(savedRate)
                    }
                    showAddRateDialog = false
                    editingRate = null
                }
            }
        )
    }

    // Delete Rate Confirmation Dialog
    rateToDelete?.let { rate ->
        AlertDialog(
            onDismissRequest = { rateToDelete = null },
            title = { Text(if (isHindi) "रेट हटाएं?" else "Delete Rate?") },
            text = { Text("Delete service '${rate.serviceName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteRate(rate)
                            rateToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_delete_rate_button")
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rateToDelete = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Ask AI Service Guide Dialog
    aiGuideService?.let { serviceName ->
        Dialog(
            onDismissRequest = { aiGuideService = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Service Guide: $serviceName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { aiGuideService = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Close")
                        }
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "🔒 Privacy Note: No customer data was sent. Only generic guidelines for '$serviceName' are fetched.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = CyanAccent,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (aiGuideLoading) {
                        Text(
                            text = if (isHindi) "AI से आवश्यक दस्तावेज़ व नियम लोड हो रहे हैं..." else "Loading document checklist & steps from AI...",
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Text(
                            text = aiGuideText ?: "No response received.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { aiGuideService = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(if (isHindi) "ठीक है" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun WorksListContent(
    isHindi: Boolean,
    works: List<SevaCustomerWork>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatusFilter: String?,
    onSelectStatusFilter: (String?) -> Unit,
    onEditWork: (SevaCustomerWork) -> Unit,
    onDeleteWork: (SevaCustomerWork) -> Unit,
    onShareReceipt: (SevaCustomerWork) -> Unit,
    onAskAiGuide: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // MANDATORY Privacy Warning Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = WarningAmber.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi)
                        "⚠️ सूचना: यहाँ आधार नंबर, पैन नंबर, बैंक खाता या पासवर्ड कभी भी दर्ज न करें।"
                    else
                        "⚠️ Note: Do not store ID numbers (Aadhaar, PAN, Bank, Passwords) here.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(if (isHindi) "ग्राहक नाम, फ़ोन या काम खोजें..." else "Search customer, phone or service...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("seva_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    text = if (isHindi) "सभी (${works.size})" else "All (${works.size})",
                    isSelected = selectedStatusFilter == null,
                    onClick = { onSelectStatusFilter(null) },
                    testTag = "status_filter_all"
                )
            }
            items(SevaCustomerWork.ALL_STATUSES) { status ->
                FilterChip(
                    text = status,
                    isSelected = selectedStatusFilter == status,
                    onClick = { onSelectStatusFilter(status) },
                    testTag = "status_filter_${status.lowercase().replace(" ", "_")}"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Works List
        if (works.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "कोई कार्य नहीं मिला" else "No work entries found",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (isHindi) "+ नया काम बटन दबाकर ग्राहक जोड़ें" else "Tap '+ Add Work' to register customer",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(works, key = { it.id }) { work ->
                    WorkItemCard(
                        isHindi = isHindi,
                        work = work,
                        onEdit = { onEditWork(work) },
                        onDelete = { onDeleteWork(work) },
                        onShareReceipt = { onShareReceipt(work) },
                        onAskAi = { onAskAiGuide(work.serviceName) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else SurfaceBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun WorkItemCard(
    isHindi: Boolean,
    work: SevaCustomerWork,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareReceipt: () -> Unit,
    onAskAi: () -> Unit
) {
    val statusColor = when (work.status) {
        SevaCustomerWork.STATUS_DONE -> SuccessGreen
        SevaCustomerWork.STATUS_DELIVERED -> CyanAccent
        SevaCustomerWork.STATUS_IN_PROGRESS -> WarningAmber
        else -> PrimaryBlue
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val createdStr = dateFormat.format(Date(work.createdAt))
    val dueStr = dateFormat.format(Date(work.dueDate))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("work_item_${work.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Customer Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = work.customerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (work.phoneNumber.isNotBlank()) {
                            Text(
                                text = work.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = work.status,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Service Name
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = work.serviceName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Clearly labeled "Ask AI" button for service guidelines
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAskAi() }
                            .testTag("ask_ai_service_button_${work.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Ask AI", fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary: Total, Paid, Balance Due
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(if (isHindi) "कुल शुल्क" else "Total", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("₹${"%.0f".format(work.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text(if (isHindi) "प्राप्त (Paid)" else "Paid", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("₹${"%.0f".format(work.paidAmount)}", fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
                Column {
                    Text(if (isHindi) "बकाया (Balance)" else "Balance Due", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        "₹${"%.0f".format(work.balanceDue)}",
                        fontWeight = FontWeight.Bold,
                        color = if (work.balanceDue > 0) DangerRed else TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isHindi) "अंतिम तारीख" else "Due Date", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(dueStr, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (work.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${work.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Actions: Share Receipt (WhatsApp), Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#SK-${work.id} • $createdStr",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Share Receipt Button
                    Button(
                        onClick = onShareReceipt,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("share_receipt_button_${work.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "रसीद शेयर" else "Receipt", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_work_button_${work.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_work_button_${work.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWorkDialog(
    isHindi: Boolean,
    work: SevaCustomerWork?,
    rateList: List<SevaRateItem>,
    onDismiss: () -> Unit,
    onSave: (SevaCustomerWork) -> Unit
) {
    var customerName by remember { mutableStateOf(work?.customerName ?: "") }
    var phoneNumber by remember { mutableStateOf(work?.phoneNumber ?: "") }
    var serviceName by remember { mutableStateOf(work?.serviceName ?: "") }
    var amountText by remember { mutableStateOf(if (work != null) work.amount.toString() else "") }
    var paidText by remember { mutableStateOf(if (work != null) work.paidAmount.toString() else "") }
    var status by remember { mutableStateOf(work?.status ?: SevaCustomerWork.STATUS_PENDING) }
    var notes by remember { mutableStateOf(work?.notes ?: "") }
    var dueDate by remember { mutableStateOf(work?.dueDate ?: System.currentTimeMillis()) }

    var expandedRatesDropdown by remember { mutableStateOf(false) }
    var expandedStatusDropdown by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val paid = paidText.toDoubleOrNull() ?: 0.0
    val balance = (amount - paid).coerceAtLeast(0.0)

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (work == null)
                            (if (isHindi) "नया ग्राहक व काम जोड़ें" else "Add Customer Work")
                        else
                            (if (isHindi) "कार्य संपादित करें" else "Edit Customer Work"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                // Privacy Rule Notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "🔒 सुरक्षा नियम: यहाँ आधार नंबर, पैन नंबर, बैंक खाता या पासवर्ड कभी न लिखें। (Do not store ID numbers here.)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = WarningAmber,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer Name
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text(if (isHindi) "ग्राहक का नाम *" else "Customer Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_input_customer_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(if (isHindi) "फ़ोन नंबर (व्हाट्सएप)" else "Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_input_phone")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Rate List Quick Picker Chips
                if (rateList.isNotEmpty()) {
                    Text(
                        text = if (isHindi) "रेट लिस्ट से चुनें (स्वचालित राशि भरेगी):" else "Pick from Rate List (Auto-fills amount):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(rateList) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, SurfaceBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        serviceName = item.serviceName
                                        amountText = item.price.toString()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.serviceName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("₹${"%.0f".format(item.price)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Service Name (Free text)
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text(if (isHindi) "सेवा / काम का नाम *" else "Service Name *") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_input_service_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount & Paid Amount Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (isHindi) "कुल राशि (₹) *" else "Total (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("seva_input_amount")
                    )

                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { paidText = it },
                        label = { Text(if (isHindi) "प्राप्त राशि (₹)" else "Paid (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("seva_input_paid")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calculated Balance Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHindi) "बकाया राशि (Balance Due):" else "Balance Due:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "₹${"%.2f".format(balance)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (balance > 0) DangerRed else SuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Dropdown
                Text(
                    text = if (isHindi) "कार्य स्थिति (Status):" else "Status:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SevaCustomerWork.ALL_STATUSES) { s ->
                        val isSel = status == s
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { status = s }
                        ) {
                            Text(
                                text = s,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Due Date Quick Selection
                Text(
                    text = if (isHindi) "अंतिम तारीख (Due Date): ${dateFormat.format(Date(dueDate))}" else "Due Date: ${dateFormat.format(Date(dueDate))}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cal = Calendar.getInstance()
                    OutlinedButton(
                        onClick = { dueDate = cal.timeInMillis },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "आज (Today)" else "Today", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                            dueDate = c.timeInMillis
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "कल (Tomorrow)" else "Tomorrow", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
                            dueDate = c.timeInMillis
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+3 Days", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                            dueDate = c.timeInMillis
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+7 Days", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "टिप्पणी / विवरण (No ID numbers)" else "Notes (No ID numbers)") },
                    placeholder = { Text("e.g. Photo submitted, form signed") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_input_notes")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save & Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "रद्द करें" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (customerName.isNotBlank() && serviceName.isNotBlank()) {
                                val saved = SevaCustomerWork(
                                    id = work?.id ?: 0,
                                    customerName = customerName.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    serviceName = serviceName.trim(),
                                    amount = amount,
                                    paidAmount = paid,
                                    status = status,
                                    dueDate = dueDate,
                                    createdAt = work?.createdAt ?: System.currentTimeMillis(),
                                    notes = notes.trim()
                                )
                                onSave(saved)
                            }
                        },
                        enabled = customerName.isNotBlank() && serviceName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("seva_save_work_button")
                    ) {
                        Text(if (isHindi) "सुरक्षित करें" else "Save Work")
                    }
                }
            }
        }
    }
}

@Composable
fun RateListContent(
    isHindi: Boolean,
    rates: List<SevaRateItem>,
    onAddRate: () -> Unit,
    onEditRate: (SevaRateItem) -> Unit,
    onDeleteRate: (SevaRateItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isHindi) "सेवा मूल्य सूची (Rate List)" else "Service Rate List",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isHindi) "सेवाओं के दाम यहाँ से बदल सकते हैं" else "Manage standard prices for quick billing",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onAddRate,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_rate_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isHindi) "नई सेवा" else "Add Rate", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (rates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isHindi) "कोई रेट सूची नहीं है" else "No rate list configured", color = TextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rates, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rate_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.serviceName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyanAccent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "₹ ${"%.0f".format(item.price)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(onClick = { onEditRate(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(onClick = { onDeleteRate(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditRateDialog(
    isHindi: Boolean,
    rate: SevaRateItem?,
    onDismiss: () -> Unit,
    onSave: (SevaRateItem) -> Unit
) {
    var serviceName by remember { mutableStateOf(rate?.serviceName ?: "") }
    var priceText by remember { mutableStateOf(if (rate != null) rate.price.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (rate == null) (if (isHindi) "नई सेवा जोड़ें" else "Add Service") else (if (isHindi) "सेवा संपादित करें" else "Edit Service"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text(if (isHindi) "सेवा का नाम" else "Service Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_rate_name_input")
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (isHindi) "मूल्य (₹)" else "Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_rate_price_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (serviceName.isNotBlank() && price >= 0) {
                        onSave(SevaRateItem(id = rate?.id ?: 0, serviceName = serviceName.trim(), price = price))
                    }
                },
                enabled = serviceName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("save_rate_button")
            ) {
                Text(if (isHindi) "सुरक्षित करें" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@Composable
fun DailySummaryContent(
    isHindi: Boolean,
    allWorks: List<SevaCustomerWork>,
    onCheckDueReminders: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("dd-MMM", Locale.getDefault())

    // Today boundaries
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis

    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val endOfToday = cal.timeInMillis

    // Today's works
    val todayWorks = allWorks.filter { it.createdAt in startOfToday..endOfToday }
    val todayTotalAmount = todayWorks.sumOf { it.amount }
    val todayTotalPaid = todayWorks.sumOf { it.paidAmount }
    val todayTotalPending = todayWorks.sumOf { it.balanceDue }

    // Last 7 days breakdown
    val weeklyDays = remember(allWorks) {
        val list = mutableListOf<DaySummaryItem>()
        for (i in 0 until 7) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val sDay = c.timeInMillis

            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            val eDay = c.timeInMillis

            val dayWorks = allWorks.filter { it.createdAt in sDay..eDay }
            val label = if (i == 0) (if (isHindi) "आज" else "Today") else if (i == 1) (if (isHindi) "कल" else "Yesterday") else dayFormat.format(Date(sDay))

            list.add(
                DaySummaryItem(
                    label = label,
                    dateStr = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(sDay)),
                    workCount = dayWorks.size,
                    totalAmount = dayWorks.sumOf { it.amount },
                    totalPaid = dayWorks.sumOf { it.paidAmount },
                    totalPending = dayWorks.sumOf { it.balanceDue }
                )
            )
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Today Title & Reminder Check
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isHindi) "आज का हिसाब (Today's Summary)" else "Today's Summary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormat.format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            OutlinedButton(
                onClick = onCheckDueReminders,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("check_due_reminders_button")
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isHindi) "आज की ड्यू जांचें" else "Check Reminders", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 KPI Cards in 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryKpiCard(
                modifier = Modifier.weight(1f),
                title = if (isHindi) "आज के कुल कार्य" else "Today's Work",
                value = "${todayWorks.size}",
                accentColor = PrimaryBlue,
                testTag = "kpi_today_work_count"
            )
            SummaryKpiCard(
                modifier = Modifier.weight(1f),
                title = if (isHindi) "कुल बिलिंग (₹)" else "Total Billing (₹)",
                value = "₹ ${"%.0f".format(todayTotalAmount)}",
                accentColor = CyanAccent,
                testTag = "kpi_today_total_amount"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryKpiCard(
                modifier = Modifier.weight(1f),
                title = if (isHindi) "कुल प्राप्त (Paid)" else "Total Paid (₹)",
                value = "₹ ${"%.0f".format(todayTotalPaid)}",
                accentColor = SuccessGreen,
                testTag = "kpi_today_paid_amount"
            )
            SummaryKpiCard(
                modifier = Modifier.weight(1f),
                title = if (isHindi) "कुल बकाया (Pending)" else "Total Pending (₹)",
                value = "₹ ${"%.0f".format(todayTotalPending)}",
                accentColor = if (todayTotalPending > 0) DangerRed else TextSecondary,
                testTag = "kpi_today_pending_amount"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly List of Days with Totals
        Text(
            text = if (isHindi) "साप्ताहिक रिपोर्ट (Weekly Breakdown)" else "Weekly Summary (Last 7 Days)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isHindi) "हर दिन का कार्य व वसूली विवरण" else "Day-by-day works and collections",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        weeklyDays.forEach { day ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = day.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${day.dateStr} • ${day.workCount} works",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isHindi) "बिलिंग" else "Billed", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextSecondary)
                            Text("₹${"%.0f".format(day.totalAmount)}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isHindi) "प्राप्त" else "Paid", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextSecondary)
                            Text("₹${"%.0f".format(day.totalPaid)}", fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isHindi) "बकाया" else "Pending", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = TextSecondary)
                            Text(
                                "₹${"%.0f".format(day.totalPending)}",
                                fontWeight = FontWeight.Bold,
                                color = if (day.totalPending > 0) DangerRed else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DaySummaryItem(
    val label: String,
    val dateStr: String,
    val workCount: Int,
    val totalAmount: Double,
    val totalPaid: Double,
    val totalPending: Double
)

@Composable
fun SummaryKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accentColor: Color,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier.testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}

@Composable
fun ShopSettingsContent(
    context: Context,
    isHindi: Boolean,
    repository: SevaRepository
) {
    val scope = rememberCoroutineScope()

    var shopName by remember { mutableStateOf(repository.getShopName()) }
    var shopPhone by remember { mutableStateOf(repository.getShopPhone()) }
    var shopTagline by remember { mutableStateOf(repository.getShopTagline()) }
    var backupStatusMsg by remember { mutableStateOf<String?>(null) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = repository.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray())
                    }
                    backupStatusMsg = if (isHindi) "बैकअप सफलतापूर्वक सुरक्षित किया गया!" else "Backup exported successfully!"
                } catch (e: Exception) {
                    backupStatusMsg = "Export failed: ${e.message}"
                }
            }
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val sb = StringBuilder()
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                            sb.appendLine(line)
                        }
                    }
                    val ok = repository.importBackupJson(sb.toString())
                    if (ok) {
                        shopName = repository.getShopName()
                        shopPhone = repository.getShopPhone()
                        shopTagline = repository.getShopTagline()
                        backupStatusMsg = if (isHindi) "बैकअप सफलतापूर्वक रीस्टोर किया गया!" else "Backup restored successfully!"
                    } else {
                        backupStatusMsg = if (isHindi) "बैकअप फ़ाइल अमान्य है।" else "Invalid backup file format."
                    }
                } catch (e: Exception) {
                    backupStatusMsg = "Import failed: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Shop Details Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "दुकान विवरण (रसीद पर छपेगा)" else "Shop Profile (Printed on receipts)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isHindi) "यह नाम और फ़ोन नंबर रसीद में दिखेगा" else "This info appears on customer receipts",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(if (isHindi) "दुकान / केंद्र का नाम" else "Shop Name") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_shop_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = shopPhone,
                    onValueChange = { shopPhone = it },
                    label = { Text(if (isHindi) "दुकान का फ़ोन नंबर" else "Shop Contact Phone") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_shop_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = shopTagline,
                    onValueChange = { shopTagline = it },
                    label = { Text(if (isHindi) "टैगलाइन / सेवा सूची" else "Tagline / Services Line") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seva_shop_tagline_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        repository.setShopName(shopName)
                        repository.setShopPhone(shopPhone)
                        repository.setShopTagline(shopTagline)
                        Toast.makeText(context, if (isHindi) "दुकान विवरण सुरक्षित!" else "Shop details saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_shop_settings_button")
                ) {
                    Text(if (isHindi) "दुकान विवरण सेव करें" else "Save Shop Details")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backup and Restore Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "बैकअप एवं रीस्टोर (JSON Backup)" else "Backup & Restore",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isHindi)
                        "सभी ग्राहकों, कार्य और रेट लिस्ट का सुरक्षित JSON बैकअप बनाएं या पुराना बैकअप रीस्टोर करें।"
                    else
                        "Export a full JSON backup file of all customers, works and rates or restore from previous backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                backupStatusMsg?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("SevaKendra_Backup_$timeStr.json")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_button")
                    ) {
                        Text(if (isHindi) "बैकअप एक्सपोर्ट" else "Export Backup")
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_button")
                    ) {
                        Text(if (isHindi) "रीस्टोर इम्पोर्ट" else "Import Backup")
                    }
                }
            }
        }
    }
}
