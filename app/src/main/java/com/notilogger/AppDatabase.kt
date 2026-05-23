package com.notilogger

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow 

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
}


data class AppGroup(
    val packageName: String,
    val count: Int,
    val lastTime: Long
)


@Database(entities = [NotificationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noti_logger_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
