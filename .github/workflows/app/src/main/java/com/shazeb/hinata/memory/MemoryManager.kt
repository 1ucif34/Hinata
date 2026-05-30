package com.shazeb.hinata.memory

import android.content.Context
import com.shazeb.hinata.data.HinataDatabase
import com.shazeb.hinata.data.MemoryEntity

class MemoryManager(context: Context) {

    private val db = HinataDatabase.getDatabase(context)
    private val memoryDao = db.memoryDao()

    // Keywords that signal something is worth remembering
    private val memoryTriggers = listOf(
        "my name is", "i am", "i'm", "i like", "i love", "i hate",
        "i prefer", "i work", "my job", "my favorite", "i always",
        "i never", "remind me", "remember that", "don't forget",
        "my birthday", "i live", "my wife", "my husband", "my kids",
        "my mom", "my dad", "my friend", "i feel", "i want to"
    )

    // Check if message contains memorable information
    fun shouldRemember(message: String): Boolean {
        val lower = message.lowercase()
        return memoryTriggers.any { lower.contains(it) }
    }

    // Save a memory to database
    suspend fun saveMemory(content: String, category: String) {
        val keywords = extractKeywords(content)
        val memory = MemoryEntity(
            content = content,
            category = category,
            keywords = keywords
        )
        memoryDao.insertMemory(memory)
    }

    // Get relevant memories for current conversation
    suspend fun getRelevantMemories(userMessage: String): String {
        val words = userMessage.lowercase()
            .split(" ")
            .filter { it.length > 3 }

        val relevantMemories = mutableListOf<MemoryEntity>()

        for (word in words) {
            val found = memoryDao.searchMemories(word)
            relevantMemories.addAll(found)
        }

        // If no specific memories found get recent ones
        if (relevantMemories.isEmpty()) {
            return memoryDao.getAllMemories()
                .take(5)
                .joinToString("\n") { "- ${it.content}" }
        }

        return relevantMemories
            .distinctBy { it.id }
            .take(5)
            .joinToString("\n") { "- ${it.content}" }
    }

    // Auto extract memory from user message
    suspend fun autoExtractAndSave(userMessage: String) {
        if (!shouldRemember(userMessage)) return

        val lower = userMessage.lowercase()

        val category = when {
            lower.contains("my name is") ||
            lower.contains("i am") ||
            lower.contains("i live") -> "fact"

            lower.contains("i like") ||
            lower.contains("i love") ||
            lower.contains("i prefer") ||
            lower.contains("my favorite") -> "preference"

            lower.contains("i feel") ||
            lower.contains("i'm sad") ||
            lower.contains("i'm happy") ||
            lower.contains("i'm stressed") -> "emotion"

            else -> "general"
        }

        saveMemory(userMessage, category)
    }

    // Extract keywords from text for search indexing
    private fun extractKeywords(text: String): String {
        return text.lowercase()
            .split(" ")
            .filter { it.length > 3 }
            .distinct()
            .take(10)
            .joinToString(",")
    }

    // Get all memories as formatted string
    suspend fun getAllMemoriesFormatted(): String {
        val memories = memoryDao.getAllMemories()
        if (memories.isEmpty()) return "No memories yet."
        return memories.joinToString("\n") { "- ${it.content}" }
    }
}
