package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class FlashCardItem(
    val front: String,
    val back: String
)

enum class StudyMode {
    EXPLAIN,
    QUIZ,
    FLASHCARDS,
    SUMMARY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var topicText by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(StudyMode.EXPLAIN) }
    var isLoading by remember { mutableStateOf(false) }
    var explanationResult by remember { mutableStateOf("") }

    // Quiz state
    var quizQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    // Flashcard state
    var flashcards by remember { mutableStateOf<List<FlashCardItem>>(emptyList()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }

    // Speech-to-text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spoken?.firstOrNull()
            if (!text.isNullOrBlank()) {
                topicText = text
            }
        }
    }

    fun triggerSpeech() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "विषय बोलिए (e.g. सौरमंडल)..." else "Speak a study topic...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {}
    }

    fun fetchExplanation(topic: String) {
        if (topic.isBlank()) return
        isLoading = true
        currentMode = StudyMode.EXPLAIN
        coroutineScope.launch {
            val prompt = """
                विषय: "$topic"
                तुम ममता AI शिक्षिका हो। इस विषय को बहुत ही सरल और स्पष्ट हिंदी में समझाओ:
                1. परिभाषा / मूल विचार (1-2 वाक्य में)
                2. इसे समझने के आसान चरण (Step 1, Step 2, Step 3)
                3. रोज़मर्रा की ज़िंदगी का एक व्यावहारिक उदाहरण (Real-world example)
                4. निष्कर्ष।
                भाषा बहुत ही सरल, रोचक और समझने में आसान होनी चाहिए।
            """.trimIndent()
            explanationResult = viewModel.askAiOneShot(prompt)
            isLoading = false
        }
    }

    fun fetchQuiz(topic: String) {
        if (topic.isBlank()) return
        isLoading = true
        currentMode = StudyMode.QUIZ
        quizFinished = false
        currentQuestionIndex = 0
        selectedOptionIndex = null
        score = 0
        coroutineScope.launch {
            val prompt = """
                विषय: "$topic"
                इस विषय पर ठीक 5 बहुविकल्पीय प्रश्न (Multiple Choice Questions) हिंदी में बनाओ।
                प्रत्येक प्रश्न के 4 विकल्प हों।
                कड़ाई से केवल वैध JSON Array प्रारूप में जवाब दो:
                [
                  {
                    "question": "प्रश्न यहाँ लिखें",
                    "options": ["विकल्प A", "विकल्प B", "विकल्प C", "विकल्प D"],
                    "correctIndex": 0,
                    "explanation": "सही उत्तर का कारण"
                  }
                ]
            """.trimIndent()
            val raw = viewModel.askAiOneShot(prompt)
            val parsed = parseQuizJson(raw, topic)
            quizQuestions = parsed
            isLoading = false
        }
    }

    fun fetchFlashcards(topic: String) {
        if (topic.isBlank()) return
        isLoading = true
        currentMode = StudyMode.FLASHCARDS
        currentCardIndex = 0
        isCardFlipped = false
        coroutineScope.launch {
            val prompt = """
                विषय: "$topic"
                इस विषय को याद करने के लिए 5 फ़्लैशकार्ड्स (Flashcards) हिंदी में बनाओ।
                कड़ाई से केवल वैध JSON Array प्रारूप में उत्तर दो:
                [
                  {
                    "front": "अवधारणा / मुख्य प्रश्न",
                    "back": "सरल व्याख्या और मुख्य उदाहरण"
                  }
                ]
            """.trimIndent()
            val raw = viewModel.askAiOneShot(prompt)
            val parsed = parseFlashcardsJson(raw, topic)
            flashcards = parsed
            isLoading = false
        }
    }

    fun fetchSummary(topic: String) {
        if (topic.isBlank()) return
        isLoading = true
        currentMode = StudyMode.SUMMARY
        coroutineScope.launch {
            val prompt = """
                विषय: "$topic"
                इस विषय का एक बहुत ही त्वरित और सटीक सार (Quick Summary) हिंदी में बुलेट पॉइंट्स में दो।
                मुख्य तथ्य और 3 महत्वपूर्ण बिंदु स्पष्ट लिखो।
            """.trimIndent()
            explanationResult = viewModel.askAiOneShot(prompt)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Topic Input Card
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
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isHindi) "पढ़ाई और व्हाइटबोर्ड (Study / Whiteboard)" else "Study / Whiteboard",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isHindi)
                        "कोई भी विषय लिखें या बोलें। ममता सरल हिंदी में स्टेप्स और उदाहरणों के साथ समझाएगी।"
                    else
                        "Type or speak any study topic. Mamta explains it in simple steps and clear examples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = topicText,
                        onValueChange = { topicText = it },
                        placeholder = { Text(if (isHindi) "विषय लिखें (जैसे: सौरमंडल, AI, प्रकाश संश्लेषण)..." else "Enter topic (e.g. Solar System, AI)...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_topic_input"),
                        trailingIcon = {
                            if (topicText.isNotEmpty()) {
                                IconButton(onClick = { topicText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    IconButton(
                        onClick = { triggerSpeech() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.2f))
                            .testTag("study_speech_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak",
                            tint = CyanAccent
                        )
                    }
                }

                // Quick Topic Chips
                Text(
                    text = if (isHindi) "सुझाए गए विषय:" else "Suggested topics:",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val topics = listOf(
                        "सौरमंडल (Solar System)",
                        "प्रकाश संश्लेषण",
                        "AI और मशीन लर्निंग",
                        "भारतीय संविधान",
                        "गुरुत्वाकर्षण बल",
                        "Photosynthesis"
                    )
                    topics.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    topicText = suggestion
                                    fetchExplanation(suggestion)
                                }
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Action Buttons Row: Explain, Quiz me, Flashcards, Summarize
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { fetchExplanation(topicText) },
                        enabled = topicText.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("study_explain_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "समझाओ" else "Explain")
                    }

                    Button(
                        onClick = { fetchQuiz(topicText) },
                        enabled = topicText.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("study_quiz_button")
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "Quiz me (5 प्रश्न)" else "Quiz me", color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = { fetchFlashcards(topicText) },
                        enabled = topicText.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("study_flashcards_button")
                    ) {
                        Icon(Icons.Default.ViewCarousel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "Flashcards" else "Flashcards")
                    }

                    OutlinedButton(
                        onClick = { fetchSummary(topicText) },
                        enabled = topicText.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("study_summarize_button")
                    ) {
                        Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isHindi) "Summarize" else "Summarize")
                    }
                }
            }
        }

        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = CyanAccent)
                    Text(
                        text = if (isHindi) "ममता तैयारी कर रही है..." else "Mamta is preparing your study guide...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Display results according to mode
        when (currentMode) {
            StudyMode.EXPLAIN, StudyMode.SUMMARY -> {
                if (explanationResult.isNotBlank() && !isLoading) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentMode == StudyMode.EXPLAIN) "📖 व्याख्या व उदाहरण:" else "📌 त्वरित सार (Summary):",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CyanAccent
                                )

                                IconButton(onClick = { viewModel.speakTextOutLoud(explanationResult) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Read aloud",
                                        tint = CyanAccent
                                    )
                                }
                            }

                            Text(
                                text = explanationResult,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            StudyMode.QUIZ -> {
                if (quizQuestions.isNotEmpty() && !isLoading) {
                    if (!quizFinished) {
                        val currentQ = quizQuestions[currentQuestionIndex]
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "प्रश्न ${currentQuestionIndex + 1} / ${quizQuestions.size}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "स्कोर: $score",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SuccessGreen
                                    )
                                }

                                Text(
                                    text = currentQ.question,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                currentQ.options.forEachIndexed { optIndex, optionText ->
                                    val isSelected = selectedOptionIndex == optIndex
                                    val isAnswered = selectedOptionIndex != null
                                    val isCorrect = optIndex == currentQ.correctIndex

                                    val bgColor = when {
                                        !isAnswered -> MaterialTheme.colorScheme.surface
                                        isCorrect -> SuccessGreen.copy(alpha = 0.2f)
                                        isSelected && !isCorrect -> DangerRed.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }

                                    val borderColor = when {
                                        !isAnswered -> SurfaceBorder
                                        isCorrect -> SuccessGreen
                                        isSelected && !isCorrect -> DangerRed
                                        else -> SurfaceBorder
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = bgColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !isAnswered) {
                                                selectedOptionIndex = optIndex
                                                if (optIndex == currentQ.correctIndex) {
                                                    score++
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "${'A' + optIndex}.",
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent
                                            )
                                            Text(
                                                text = optionText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // Show explanation and Next button once answered
                                if (selectedOptionIndex != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (selectedOptionIndex == currentQ.correctIndex) "✅ सही जवाब!" else "❌ गलत जवाब!",
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedOptionIndex == currentQ.correctIndex) SuccessGreen else DangerRed
                                            )
                                            Text(
                                                text = currentQ.explanation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (currentQuestionIndex + 1 < quizQuestions.size) {
                                                currentQuestionIndex++
                                                selectedOptionIndex = null
                                            } else {
                                                quizFinished = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (currentQuestionIndex + 1 < quizQuestions.size) "अगला प्रश्न ➡️" else "परिणाम देखें 🏆")
                                    }
                                }
                            }
                        }
                    } else {
                        // Quiz Finished Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "🎉 क्विज़ संपन्न!",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyanAccent
                                )
                                Text(
                                    text = "आपका कुल स्कोर: $score / ${quizQuestions.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (score >= 3) SuccessGreen else DangerRed
                                )
                                Text(
                                    text = if (score >= 4) "शानदार! आपने विषय को बहुत अच्छे से समझा है।" else "अच्छा प्रयास! एक बार फिर दोहराएँ।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        currentQuestionIndex = 0
                                        selectedOptionIndex = null
                                        score = 0
                                        quizFinished = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("क्विज़ फिर से दें")
                                }
                            }
                        }
                    }
                }
            }

            StudyMode.FLASHCARDS -> {
                if (flashcards.isNotEmpty() && !isLoading) {
                    val card = flashcards[currentCardIndex]
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "कार्ड ${currentCardIndex + 1} / ${flashcards.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyanAccent
                                )
                                Text(
                                    text = if (isCardFlipped) "🔄 उत्तर पक्ष (Back)" else "👆 कार्ड पलटने के लिए टैप करें",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Interactive Flippable Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { isCardFlipped = !isCardFlipped },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCardFlipped)
                                        PrimaryBlue.copy(alpha = 0.25f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    if (isCardFlipped) CyanAccent else SurfaceBorder
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flip,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = if (!isCardFlipped) card.front else card.back,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 24.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (!isCardFlipped) "(टैप करें उत्तर देखने के लिए)" else "(टैप करें प्रश्न देखने के लिए)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            // Navigation Row: Prev / Next
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (currentCardIndex > 0) {
                                            currentCardIndex--
                                            isCardFlipped = false
                                        }
                                    },
                                    enabled = currentCardIndex > 0,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("⬅️ पिछला")
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (currentCardIndex + 1 < flashcards.size) {
                                            currentCardIndex++
                                            isCardFlipped = false
                                        }
                                    },
                                    enabled = currentCardIndex + 1 < flashcards.size,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("अगला ➡️")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseQuizJson(raw: String, fallbackTopic: String): List<QuizQuestion> {
    try {
        val clean = extractJsonBlock(raw)
        val array = JSONArray(clean)
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val q = obj.getString("question")
            val optArr = obj.getJSONArray("options")
            val options = mutableListOf<String>()
            for (j in 0 until optArr.length()) {
                options.add(optArr.getString(j))
            }
            val correct = obj.getInt("correctIndex")
            val expl = obj.optString("explanation", "सही उत्तर।")
            list.add(QuizQuestion(q, options, correct, expl))
        }
        if (list.isNotEmpty()) return list
    } catch (_: Exception) {}

    // Fallback default questions if AI returns unformatted text
    return listOf(
        QuizQuestion(
            question = "$fallbackTopic का मुख्य उद्देश्य या परिभाषा क्या है?",
            options = listOf("यह एक प्राकृतिक या वैज्ञानिक अवधारणा है", "यह एक गणितीय समीकरण है", "यह सिर्फ एक काल्पनिक विचार है", "उपरोक्त में से कोई नहीं"),
            correctIndex = 0,
            explanation = "$fallbackTopic एक महत्वपूर्ण विषय है जो हमारे दैनिक जीवन व विज्ञान से जुड़ा है।"
        ),
        QuizQuestion(
            question = "$fallbackTopic के बारे में कौन सा कथन सही है?",
            options = listOf("यह हमेशा स्थिर रहता है", "यह पर्यावरण या तंत्र में परिवर्तन से प्रभावित होता है", "इसका कोई उपयोग नहीं है", "यह केवल प्रयोगशाला में होता है"),
            correctIndex = 1,
            explanation = "अधिकांश प्राकृतिक या तकनीकी प्रणालियाँ गतिशील होती हैं।"
        ),
        QuizQuestion(
            question = "दैनिक जीवन में $fallbackTopic का क्या महत्व है?",
            options = listOf("यह बुनियादी सिद्धांतों को समझने में मदद करता है", "यह उपयोगी नहीं है", "केवल परीक्षा के लिए है", "कोई प्रभाव नहीं"),
            correctIndex = 0,
            explanation = "बुनियादी सिद्धांतों की समझ से जीवन व तकनीकी ज्ञान बढ़ता है।"
        ),
        QuizQuestion(
            question = "$fallbackTopic के अध्ययन के लिए सबसे पहला कदम क्या है?",
            options = listOf("मूल अवधारणाओं को समझना", "केवल याद कर लेना", "बिना समझे पढ़ना", "छोड़ देना"),
            correctIndex = 0,
            explanation = "पहला कदम हमेशा बुनियादी बातों को स्पष्ट करना है।"
        ),
        QuizQuestion(
            question = "क्या $fallbackTopic आधुनिक तकनीक व विकास में महत्वपूर्ण है?",
            options = listOf("हाँ, अत्यधिक महत्वपूर्ण", "नहीं, बिल्कुल नहीं", "शायद कभी-कभार", "अस्पष्ट"),
            correctIndex = 0,
            explanation = "आधुनिक युग में हर वैज्ञानिक या सामाजिक विषय का बड़ा योगदान है।"
        )
    )
}

private fun parseFlashcardsJson(raw: String, fallbackTopic: String): List<FlashCardItem> {
    try {
        val clean = extractJsonBlock(raw)
        val array = JSONArray(clean)
        val list = mutableListOf<FlashCardItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val front = obj.getString("front")
            val back = obj.getString("back")
            list.add(FlashCardItem(front, back))
        }
        if (list.isNotEmpty()) return list
    } catch (_: Exception) {}

    return listOf(
        FlashCardItem("अवधारणा: $fallbackTopic", "$fallbackTopic की मूल परिभाषा और इसका आधार।"),
        FlashCardItem("चरण 1", "शुरुआती प्रक्रिया और सिद्धांत।"),
        FlashCardItem("चरण 2", "मध्यवर्ती प्रभाव और वास्तविक क्रिया।"),
        FlashCardItem("उदाहरण", "दैनिक जीवन में इसका प्रत्यक्ष प्रमाण या अनुप्रयोग।"),
        FlashCardItem("महत्वपूर्ण निष्कर्ष", "याद रखने योग्य सबसे मुख्य बिंदु।")
    )
}

private fun extractJsonBlock(text: String): String {
    val trimmed = text.trim()
    val firstBracket = trimmed.indexOf('[')
    val lastBracket = trimmed.lastIndexOf(']')
    return if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
        trimmed.substring(firstBracket, lastBracket + 1)
    } else {
        trimmed
    }
}
