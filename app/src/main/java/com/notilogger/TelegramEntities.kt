package com.notilogger

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_bots")
data class TelegramBot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val token: String,
    val hasCustomFilter: Boolean = false,
    val filterMode: Int = 0, // 0 = Blacklist, 1 = Whitelist
    val selectedAppsJson: String = "[]",
    val keywordsJson: String = "[]"
)

@Entity(
    tableName = "telegram_chats",
    foreignKeys = [
        ForeignKey(
            entity = TelegramBot::class,
            parentColumns = ["id"],
            childColumns = ["botId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TelegramChat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val botId: Long,
    val name: String,
    val chatId: String,
    val hasCustomFilter: Boolean = false,
    val filterMode: Int = 0,
    val selectedAppsJson: String = "[]",
    val keywordsJson: String = "[]"
)
