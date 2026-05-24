package com.notilogger

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT packageName, COUNT(*) as count, MAX(timestamp) as lastTime FROM notifications GROUP BY packageName ORDER BY lastTime DESC")
    suspend fun getAppGroups(): List<AppGroup>
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllLogsRaw(): List<NotificationEntity>

    
    @Query("SELECT * FROM notifications WHERE packageName = :pkgName ORDER BY timestamp DESC")
    fun getLogsByPackage(pkgName: String): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE packageName = :pkgName")
    suspend fun deleteAppLogs(pkgName: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    // Telegram Bot methods
    @Insert
    suspend fun insertBot(bot: TelegramBot): Long

    @Update
    suspend fun updateBot(bot: TelegramBot)

    @Delete
    suspend fun deleteBot(bot: TelegramBot)

    @Query("SELECT * FROM telegram_bots ORDER BY id DESC")
    suspend fun getAllBots(): List<TelegramBot>

    @Query("SELECT * FROM telegram_bots WHERE id = :id")
    suspend fun getBotById(id: Long): TelegramBot?

    // Telegram Chat methods
    @Insert
    suspend fun insertChat(chat: TelegramChat): Long

    @Update
    suspend fun updateChat(chat: TelegramChat)

    @Delete
    suspend fun deleteChat(chat: TelegramChat)

    @Query("SELECT * FROM telegram_chats WHERE botId = :botId ORDER BY id DESC")
    suspend fun getChatsForBot(botId: Long): List<TelegramChat>

    @Query("SELECT * FROM telegram_chats WHERE id = :id")
    suspend fun getChatById(id: Long): TelegramChat?
}


data class AppGroup(
    val packageName: String,
    val count: Int,
    val lastTime: Long
)


@Database(entities = [NotificationEntity::class, TelegramBot::class, TelegramChat::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                
                System.loadLibrary("sqlcipher")

                // --- Encryption Setup ---
                // For testing, we are using a hardcoded password. 
                // Later, this will be fetched from user preferences.
                val passphrase = "test_password".toByteArray() 
                val factory = SupportOpenHelperFactory(passphrase)
                // ------------------------

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noti_logger_db"
                )
                .openHelperFactory(factory) // Attach the encryption factory
                .fallbackToDestructiveMigration() // Clear old data on schema change
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
