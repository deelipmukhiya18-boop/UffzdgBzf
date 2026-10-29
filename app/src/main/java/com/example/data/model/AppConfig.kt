package com.example.data.model

data class AppConfig(
    val provider: String = "gemini", // "gemini", "claude", or "openai"
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-sonnet-5-5",
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.8-flash",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    val searchProvider: String = "tavily", // "tavily" or "brave"
    val searchApiKey: String = "",
    val speakAnswers: Boolean = true,
    // Image Generation settings
    val imageProvider: String = "openai", // "openai" or "gemini"
    val openaiImageModel: String = "dall-e-3",
    val geminiImageModel: String = "imagen-3.0-generate-002"
)
