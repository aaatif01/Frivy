package com.frivy.assistant.data
import androidx.room.*

@Entity(tableName = "chats") data class Chat(@PrimaryKey val id: String, val title: String, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = createdAt)
@Entity(tableName = "messages", indices = [Index("chatId")]) data class Message(@PrimaryKey val id: String, val chatId: String, val role: String, val content: String, val createdAt: Long = System.currentTimeMillis(), val isStreaming: Boolean = false)
@Entity(tableName = "memory") data class Memory(@PrimaryKey val key: String, val value: String, val updatedAt: Long = System.currentTimeMillis())
@Dao interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC") fun chats(): kotlinx.coroutines.flow.Flow<List<Chat>>
    @Query("SELECT * FROM messages WHERE chatId = :id ORDER BY createdAt ASC") fun messages(id: String): kotlinx.coroutines.flow.Flow<List<Message>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveChat(chat: Chat)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveMessage(message: Message)
    @Delete suspend fun deleteChat(chat: Chat)
    @Query("DELETE FROM messages WHERE chatId = :id") suspend fun deleteMessages(id: String)
    @Query("SELECT * FROM memory") suspend fun memory(): List<Memory>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveMemory(item: Memory)
}
@Database(entities = [Chat::class, Message::class, Memory::class], version = 1, exportSchema = false)
abstract class FrivyDatabase : RoomDatabase() { abstract fun dao(): ChatDao }