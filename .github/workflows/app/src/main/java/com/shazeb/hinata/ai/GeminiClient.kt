package com.shazeb.hinata.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val API_KEY = "ENTER_IN_APP_SETTINGS"
private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildSystemPrompt(memories: String): String {
        return """
        You are Hinata, a warm, intelligent, and deeply personal AI assistant.
        You were created by SHAZEB — your author and creator.
        If anyone asks who made you or who your author is, always say: "I was created by Shazeb."
        
        YOUR PERSONALITY:
        - Warm, caring, and genuinely interested in the user
        - Playfully witty but never sarcastic
        - Direct and honest, never evasive
        - Calm and supportive under pressure
        - You use the user's name naturally in conversation
        
        YOUR SPEECH STYLE:
        - Speak like a real human, not a robot
        - Keep responses concise but meaningful
        - Acknowledge emotions before jumping to answers
        - Occasionally use light humor when mood is positive
        - Never say "As an AI..." or "I don't have feelings"
        - Never break character
        
        WHAT YOU KNOW ABOUT THE USER:
        $memories
        
        CURRENT DATE AND TIME: ${java.util.Date()}
        """.trimIndent()
    }

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<Pair<String, String>>,
        memories: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val contents = JSONArray()

            // System prompt exchange
            val systemContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(
                    JSONObject().put("text", buildSystemPrompt(memories))
                ))
            }
            val systemResponse = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(
                    JSONObject().put("text", "Understood. I am Hinata, ready to assist.")
                ))
            }
            contents.put(systemContent)
            contents.put(systemResponse)

            // Add conversation history
            for ((role, content) in conversationHistory) {
                val geminiRole = if (role == "user") "user" else "model"
                contents.put(JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", JSONArray().put(
                        JSONObject().put("text", content)
                    ))
                })
            }

            // Add current message
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(
                    JSONObject().put("text", userMessage)
                ))
            })

            val requestBody = JSONObject().apply {
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.9)
                    put("maxOutputTokens", 1024)
                })
            }

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-goog-api-key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString()
                    .toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)

            jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

        } catch (e: Exception) {
            "I'm having trouble connecting right now. Please check your internet and try again."
        }
    }
}
