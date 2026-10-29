package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MarketResult(
    val query: String,
    val summary: String,
    val sourceName: String,
    val timestamp: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketsScreen(
    viewModel: AssistantViewModel,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val config by viewModel.config.collectAsStateWithLifecycle()

    var customSearchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var marketResult by remember { mutableStateOf<MarketResult?>(null) }
    var activeCommodity by remember { mutableStateOf<String?>(null) }

    fun queryMarket(label: String, searchQuery: String) {
        isLoading = true
        activeCommodity = label
        coroutineScope.launch {
            val provider = config.searchProvider
            val apiKey = config.searchApiKey
            val rawSearch = viewModel.toolController.webSearchService.search(searchQuery, provider, apiKey)

            val now = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val sourceName = when {
                provider.equals("brave", ignoreCase = true) -> "Brave Search Live Feed"
                apiKey.isNotBlank() -> "Tavily Market Data"
                else -> "Web Search Live Feed (DuckDuckGo)"
            }

            marketResult = MarketResult(
                query = label,
                summary = rawSearch.ifBlank { "कोई परिणाम नहीं मिला। कृपया दोबारा प्रयास करें।" },
                sourceName = sourceName,
                timestamp = now
            )
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mandatory Financial Advice Disclaimer
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = WarningAmber.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isHindi)
                        "सूचना: भाव में देरी हो सकती है और यह वित्तीय सलाह नहीं है (Prices may be delayed and this is not financial advice)."
                    else
                        "Notice: Prices may be delayed and this is not financial advice.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Quick Commodity Price Cards: Gold, Silver, Petrol
        Text(
            text = if (isHindi) "प्रमुख दैनिक भाव (Quick Rates):" else "Quick Market Rates:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gold Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (activeCommodity == "Gold") CyanAccent else SurfaceBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        queryMarket(
                            "Gold",
                            "current 24k and 22k gold price today in India per 10 grams live rate"
                        )
                    }
                    .testTag("market_gold_button")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isHindi) "सोना (Gold)" else "Gold (24K)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "10 ग्राम भाव",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            // Silver Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (activeCommodity == "Silver") CyanAccent else SurfaceBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        queryMarket(
                            "Silver",
                            "current silver price today in India per 1 kg live rate"
                        )
                    }
                    .testTag("market_silver_button")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isHindi) "चांदी (Silver)" else "Silver",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1 किलो भाव",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            // Petrol Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (activeCommodity == "Petrol") CyanAccent else SurfaceBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        queryMarket(
                            "Petrol",
                            "current petrol and diesel price today in Delhi Mumbai India"
                        )
                    }
                    .testTag("market_petrol_button")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isHindi) "पेट्रोल (Petrol)" else "Petrol",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "प्रति लीटर",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }
        }

        // Custom Search for Stock or Crypto
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
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isHindi) "स्टॉक या क्रिप्टो सर्च (Stock & Crypto)" else "Search Stock or Crypto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSearchQuery,
                        onValueChange = { customSearchQuery = it },
                        placeholder = { Text(if (isHindi) "शेयर या क्रिप्टो का नाम (e.g. NIFTY, Bitcoin)..." else "Stock or Crypto (e.g. Reliance, Bitcoin)...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("market_search_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    Button(
                        onClick = {
                            if (customSearchQuery.isNotBlank()) {
                                queryMarket(
                                    customSearchQuery.trim(),
                                    "${customSearchQuery.trim()} current price share market stock crypto live quote"
                                )
                            }
                        },
                        enabled = customSearchQuery.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("market_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "देखें" else "Check")
                    }
                }

                // Quick Stock/Crypto chips
                Text(
                    text = if (isHindi) "लोकप्रिय विकल्प:" else "Popular options:",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPicks = listOf(
                        "NIFTY 50",
                        "Sensex",
                        "Reliance",
                        "Tata Motors",
                        "TCS",
                        "Bitcoin (BTC)",
                        "Ethereum (ETH)",
                        "Solana (SOL)"
                    )
                    quickPicks.forEach { pick ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    customSearchQuery = pick
                                    queryMarket(pick, "$pick current share price or crypto live rate today")
                                }
                        ) {
                            Text(
                                text = pick,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Loading indicator
        if (isLoading) {
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
                        text = if (isHindi) "ताज़ा लाइव भाव प्राप्त किए जा रहे हैं..." else "Fetching live rates via search tool...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Market Result Card
        if (marketResult != null && !isLoading) {
            val res = marketResult!!
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📈 ${res.query} भाव व स्थिति",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyanAccent
                            )
                            Text(
                                text = "स्रोत: ${res.sourceName} • समय: ${res.timestamp}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }

                        IconButton(onClick = { viewModel.speakTextOutLoud(res.summary) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = CyanAccent
                            )
                        }
                    }

                    Text(
                        text = res.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Bottom reminder disclaimer
                    Text(
                        text = "⚠️ भाव केवल संदर्भ के लिए हैं और इनमें देरी हो सकती है। यह वित्तीय सलाह नहीं है।",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = WarningAmber
                    )
                }
            }
        }
    }
}
