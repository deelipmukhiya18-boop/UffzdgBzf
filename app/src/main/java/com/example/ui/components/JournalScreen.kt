package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Summarize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.journal.JournalEntry
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<JournalEntry>>(emptyList()) }
    var newNoteContent by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("😊 प्रसन्न") }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }

    // Summary state
    var isSummarizing by remember { mutableStateOf(false) }
    var weeklySummary by remember { mutableStateOf<String?>(null) }

    fun refreshEntries() {
        entries = viewModel.journalDb.getAllEntries()
    }

    LaunchedEffect(Unit) {
        refreshEntries()
    }

    val moodOptions = listOf(
        "😊 प्रसन्न (Happy)",
        "😌 शांत (Calm)",
        "😐 सामान्य (Neutral)",
        "😔 उदास (Sad)",
        "🔥 प्रेरित (Motivated)",
        "😴 थका (Tired)"
    )

    fun handleSummarizeWeek() {
        val weekEntries = viewModel.journalDb.getWeekEntries()
        if (weekEntries.isEmpty()) {
            weeklySummary = if (isHindi)
                "पिछले 7 दिनों में कोई डायरी नोट नहीं मिला। कृपया पहले कुछ नोट्स लिखें।"
            else
                "No journal notes found for the past week. Please add some notes first."
            return
        }

        isSummarizing = true
        coroutineScope.launch {
            val sb = StringBuilder()
            weekEntries.forEach { e ->
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(e.timestamp))
                sb.append("• [$dateStr] (मूड: ${e.mood}): ${e.content}\n")
            }

            val prompt = """
                तुम ममता AI हो। यूज़र के पिछले हफ्ते के ये व्यक्तिगत जर्नल/डायरी नोट्स हैं:
                $sb
                
                कृपया इनका एक आत्मीय, उत्साहवर्धक और विचारशील साप्ताहिक सारांश (Weekly Reflection) हिंदी में दो:
                1. इस हफ्ते का मुख्य मिजाज़ और घटनाएँ
                2. क्या अच्छा रहा और क्या ध्यान देने योग्य है
                3. आने वाले हफ्ते के लिए 1 प्रेरणादायक सलाह
            """.trimIndent()

            weeklySummary = viewModel.askAiOneShot(prompt)
            isSummarizing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // New Note Input Card
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
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isHindi) "दैनिक जर्नल (Daily Journal)" else "Daily Journal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isHindi)
                        "अपने विचार, दिन की मुख्य बातें या भावनाएं यहाँ सुरक्षित रखें। नोट्स केवल आपके फ़ोन में रहते हैं।"
                    else
                        "Save your thoughts and reflections locally. All notes remain strictly on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = newNoteContent,
                    onValueChange = { newNoteContent = it },
                    placeholder = { Text(if (isHindi) "आज क्या हुआ? अपने विचार यहाँ लिखें..." else "What's on your mind today? Write here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("journal_input_content"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                // Mood selector
                Text(
                    text = if (isHindi) "मनोदशा (Mood):" else "Mood:",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    moodOptions.forEach { mood ->
                        val isSelected = selectedMood == mood
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else SurfaceBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedMood = mood }
                        ) {
                            Text(
                                text = mood,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (newNoteContent.isNotBlank()) {
                                viewModel.journalDb.addEntry(
                                    title = "",
                                    content = newNoteContent.trim(),
                                    mood = selectedMood
                                )
                                newNoteContent = ""
                                refreshEntries()
                            }
                        },
                        enabled = newNoteContent.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("journal_save_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "नोट सेव करें" else "Save Note")
                    }

                    OutlinedButton(
                        onClick = { handleSummarizeWeek() },
                        enabled = !isSummarizing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("journal_summarize_week_button")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "हफ्ते का सार (AI)" else "Summarize my week")
                    }
                }
            }
        }

        // Weekly Summary Result
        if (isSummarizing) {
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
                        text = if (isHindi) "ममता आपके हफ्ते के नोट्स का विश्लेषण कर रही है..." else "Mamta is analyzing your week's notes...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else if (weeklySummary != null) {
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanAccent)
                            Text(
                                text = if (isHindi) "✨ हफ्ते का सारांश (Weekly Reflection)" else "✨ Weekly Reflection",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyanAccent
                            )
                        }

                        IconButton(onClick = { viewModel.speakTextOutLoud(weeklySummary ?: "") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = CyanAccent
                            )
                        }
                    }

                    Text(
                        text = weeklySummary ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = { weeklySummary = null },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isHindi) "छिपाएं" else "Dismiss")
                    }
                }
            }
        }

        // Saved Entries List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isHindi) "पुराने नोट्स (${entries.size})" else "Journal Notes (${entries.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (entries.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isHindi) "अभी कोई नोट नहीं लिखा गया है। ऊपर से पहला नोट जोड़ें!" else "No journal entries yet. Add your first note above!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            entries.forEach { entry ->
                val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (entry.mood.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Text(
                                            text = entry.mood,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = CyanAccent
                                        )
                                    }
                                }
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }

                            IconButton(
                                onClick = { entryToDelete = entry },
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

                        Text(
                            text = entry.content,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (entryToDelete != null) {
        val toDelete = entryToDelete!!
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(if (isHindi) "नोट हटाएं?" else "Delete Note?") },
            text = { Text(if (isHindi) "क्या आप इस जर्नल नोट को हमेशा के लिए हटाना चाहते हैं?" else "Are you sure you want to permanently delete this journal note?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.journalDb.deleteEntry(toDelete.id)
                        entryToDelete = null
                        refreshEntries()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { entryToDelete = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
