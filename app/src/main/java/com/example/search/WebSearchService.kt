package com.example.search

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String,
        provider: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val cleanKey = apiKey.trim()

        if (cleanKey.isNotBlank()) {
            if (provider.equals("brave", ignoreCase = true)) {
                val result = searchBrave(cleanQuery, cleanKey)
                if (result.isNotBlank()) return@withContext result
            } else {
                val result = searchTavily(cleanQuery, cleanKey)
                if (result.isNotBlank()) return@withContext result
            }
        }

        // Fallback search when key is empty or failed
        searchDuckDuckGoFallback(cleanQuery)
    }

    private fun searchTavily(query: String, apiKey: String): String {
        return try {
            val bodyJson = JSONObject().apply {
                put("query", query)
                put("max_results", 5)
                put("include_answer", true)
            }

            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string().orEmpty()

            val json = JSONObject(resStr)
            if (json.has("error") || json.has("detail")) {
                val err = json.optString("error", json.optString("detail"))
                return "Tavily search error: $err"
            }

            val sb = StringBuilder()
            val answer = json.optString("answer")
            if (answer.isNotBlank()) {
                sb.append("सार (Summary): ").append(answer).append("\n\n")
            }

            val results = json.optJSONArray("results") ?: JSONArray()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val title = item.optString("title")
                val content = item.optString("content").take(300)
                val url = item.optString("url")
                sb.append("• ").append(title).append(": ").append(content).append(" (").append(url).append(")\n")
            }
            if (sb.isEmpty()) "कोई सर्च परिणाम नहीं मिला" else sb.toString().trim()
        } catch (e: Exception) {
            Log.e("WebSearchService", "Tavily failed", e)
            ""
        }
    }

    private fun searchBrave(query: String, apiKey: String): String {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.search.brave.com/res/v1/web/search?q=$encodedQuery&count=5"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("X-Subscription-Token", apiKey)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string().orEmpty()

            val json = JSONObject(resStr)
            if (json.has("error")) {
                val err = json.optJSONObject("error")?.optString("message") ?: json.getString("error")
                return "Brave search error: $err"
            }

            val web = json.optJSONObject("web")
            val results = web?.optJSONArray("results") ?: JSONArray()
            val sb = StringBuilder()

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val title = item.optString("title")
                val desc = item.optString("description")
                val url = item.optString("url")
                sb.append("• ").append(title).append(": ").append(desc).append(" (").append(url).append(")\n")
            }
            if (sb.isEmpty()) "कोई सर्च परिणाम नहीं मिला" else sb.toString().trim()
        } catch (e: Exception) {
            Log.e("WebSearchService", "Brave search failed", e)
            ""
        }
    }

    private fun searchDuckDuckGoFallback(query: String): String {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mamta-Assistant/2.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string().orEmpty()

            val json = JSONObject(resStr)
            val abstractText = json.optString("AbstractText")
            val heading = json.optString("Heading")
            val relatedTopics = json.optJSONArray("RelatedTopics")

            val sb = StringBuilder()
            if (abstractText.isNotBlank()) {
                sb.append("सार: ").append(heading).append(" - ").append(abstractText).append("\n")
            }

            if (relatedTopics != null && relatedTopics.length() > 0) {
                var added = 0
                for (i in 0 until relatedTopics.length()) {
                    if (added >= 3) break
                    val topic = relatedTopics.optJSONObject(i)
                    val text = topic?.optString("Text")
                    if (!text.isNullOrBlank()) {
                        sb.append("• ").append(text).append("\n")
                        added++
                    }
                }
            }

            if (sb.isNotEmpty()) {
                sb.toString().trim()
            } else {
                "सर्च API key (Tavily/Brave) नहीं डाली है। ⚙️ सेटिंग में जाकर Tavily या Brave की key डालें।"
            }
        } catch (e: Exception) {
            "सर्च API key (Tavily/Brave) नहीं डाली है। ⚙️ सेटिंग में डालें।"
        }
    }
}
