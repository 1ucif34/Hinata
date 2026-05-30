package com.shazeb.hinata.data

import android.content.Context
import androidx.room.*

// Message entity - stores every conversation message
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String,        // "user" or "assistant"
    val content: String,     // the message text
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String    // groups messages by conversation
)

// Memory entity - stores long term memories about user
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,     // "User's name is Shazeb"
    val category: String,    // fact/preference/emotion
    val keywords: String,    // comma separated for search
    val createdAt: Long = System.currentTimeMillis(),
    val timesRetrieved: Int = 0
)

// Database access functions for messages
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSessionMessages(sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentMessages(): List<MessageEntity>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE timestamp < :cutoff")
    suspend fun deleteOldMessages(cutoff: Long)
}

// Database access functions for memories
@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timesRetrieved DESC")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE keywords LIKE '%' || :keyword || '%'")
    suspend fun searchMemories(keyword: String): List<MemoryEntity>

    @Insert
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)
}

// Main database class
@Database(
    entities = [MessageEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HinataDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: HinataDatabase? = null

        fun getDatabase(context: Context): HinataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HinataDatabase::class.java,
                    "hinata_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
