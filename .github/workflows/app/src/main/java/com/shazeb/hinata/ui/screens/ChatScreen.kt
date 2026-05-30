package com.shazeb.hinata.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.shazeb.hinata.ai.GeminiClient
import com.shazeb.hinata.memory.MemoryManager
import com.shazeb.hinata.voice.VoiceEngine
import kotlinx.coroutines.launch

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Core components
    val geminiClient = remember { GeminiClient() }
    val voiceEngine = remember { VoiceEngine(context) }
    val memoryManager = remember { MemoryManager(context) }

    // State
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap mic or type to talk to Hinata") }
    val listState = rememberLazyListState()

    // API key state
    var apiKey by remember {
        mutableStateOf(
            context.getSharedPreferences("hinata_prefs", Context.MODE_PRIVATE)
                .getString("gemini_api_key", "") ?: ""
        )
    }
    var showApiKeyDialog by remember { mutableStateOf(apiKey.isEmpty()) }
    var apiKeyInput by remember { mutableStateOf("") }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            statusText = "Listening..."
            voiceEngine.startListening()
        }
    }

    // Voice result handler
    voiceEngine.onSpeechResult = { text ->
        scope.launch {
            isListening = false
            statusText = "Processing..."
            messages = messages + Message(text, isUser = true)
            isLoading = true

            // Auto save memory
            memoryManager.autoExtractAndSave(text)

            // Get relevant memories
            val memories = memoryManager.getRelevantMemories(text)

            // Build history for context
            val history = messages.takeLast(10).map { msg ->
                Pair(if (msg.isUser) "user" else "assistant", msg.text)
            }

            // Get AI response
            val apiKeyToUse = context.getSharedPreferences("hinata_prefs", Context.MODE_PRIVATE)
                .getString("gemini_api_key", "") ?: ""

            val response = geminiClient.sendMessage(
                userMessage = text,
                conversationHistory = history,
                memories = memories,
                apiKey = apiKeyToUse
            )

            messages = messages + Message(response, isUser = false)
            isLoading = false
            isSpeaking = true
            statusText = "Hinata is speaking..."

            voiceEngine.speak(response) {
                isSpeaking = false
                statusText = "Tap mic or type to talk to Hinata"
            }

            // Scroll to bottom
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text(
                    "Enter Gemini API Key",
                    color = Color(0xFFFF6B9D),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Go to aistudio.google.com to get your free API key",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key", color = Color(0xFFFF6B9D)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6B9D),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (apiKeyInput.isNotEmpty()) {
                            context.getSharedPreferences("hinata_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("gemini_api_key", apiKeyInput)
                                .apply()
                            apiKey = apiKeyInput
                            showApiKeyDialog = false
                            messages = messages + Message(
                                "Hello! I'm Hinata, your personal AI assistant created by Shazeb. How can I help you today?",
                                isUser = false
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B9D)
                    )
                ) {
                    Text("Save & Start")
                }
            }
        )
    }

    // Main UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "✨ Hinata AI",
                        color = Color(0xFFFF6B9D),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Created by Shazeb",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Status text
            Text(
                text = statusText,
                color = Color(0xFFFF6B9D).copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A2E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text("Message Hinata...",
                            color = Color.Gray,
                            fontSize = 14.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6B9D),
                        unfocusedBorderColor = Color(0xFF333355)
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                if (inputText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isEmpty()) return@IconButton
                            inputText = ""
                            scope.launch {
                                messages = messages + Message(text, isUser = true)
                                isLoading = true
                                statusText = "Hinata is thinking..."

                                memoryManager.autoExtractAndSave(text)
                                val memories = memoryManager.getRelevantMemories(text)
                                val history = messages.takeLast(10).map { msg ->
                                    Pair(if (msg.isUser) "user" else "assistant", msg.text)
                                }
                                val
