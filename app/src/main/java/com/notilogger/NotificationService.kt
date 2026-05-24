package com.notilogger

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        
        if (sbn.isOngoing) return 

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
	
	
	if (!FilterManager.shouldLog(applicationContext, sbn.packageName)) return
        
        // Keyword Filtering
        if (KeywordManager.shouldBlockNotification(applicationContext, sbn.packageName, title, text)) return
        
        if (title.isBlank() && text.isBlank()) return

        val entry = NotificationEntity(
            packageName = sbn.packageName,
            title = title,
            content = text,
            timestamp = sbn.postTime
        )

        
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.notificationDao().insert(entry)

            // --- Telegram Forwarding Hierarchy ---
            val bots = db.notificationDao().getAllBots()
            for (bot in bots) {
                val chats = db.notificationDao().getChatsForBot(bot.id)
                for (chat in chats) {
                    if (shouldForwardToChat(bot, chat, sbn.packageName, title, text)) {
                        val message = "<b>[${getAppName(sbn.packageName)}]</b>\n" +
                                "<b>${title}</b>\n\n" +
                                "${text}"
                        TelegramForwarder.sendMessage(bot.token, chat.chatId, message)
                    }
                }
            }
        }
    }

    private fun shouldForwardToChat(bot: TelegramBot, chat: TelegramChat, pkg: String, title: String, content: String): Boolean {
        // Hierarchy: Chat > Bot > Global
        
        // 1. Check Chat Custom Filter
        if (chat.hasCustomFilter) {
            return evaluateFilter(chat.filterMode, chat.selectedAppsJson, chat.keywordsJson, pkg, title, content)
        }

        // 2. Check Bot Custom Filter
        if (bot.hasCustomFilter) {
            return evaluateFilter(bot.filterMode, bot.selectedAppsJson, bot.keywordsJson, pkg, title, content)
        }

        // 3. Fallback to Global
        val passesAppFilter = FilterManager.shouldLog(applicationContext, pkg)
        val passesKeywordFilter = !KeywordManager.shouldBlockNotification(applicationContext, pkg, title, content)
        return passesAppFilter && passesKeywordFilter
    }

    private fun evaluateFilter(mode: Int, appsJson: String, keywordsJson: String, pkg: String, title: String, content: String): Boolean {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        
        val selectedApps: List<String> = gson.fromJson(appsJson, type)
        val keywords: List<String> = gson.fromJson(keywordsJson, type)

        // App Filter
        val isAppSelected = selectedApps.contains(pkg)
        val appMatches = if (mode == FilterManager.MODE_WHITELIST) isAppSelected else !isAppSelected
        
        if (!appMatches) return false

        // Keyword Filter
        val textToSearch = "$title $content".lowercase()
        for (keyword in keywords) {
            if (keyword.isNotEmpty() && textToSearch.contains(keyword.lowercase())) {
                return false // Blocked by keyword
            }
        }
        
        return true
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
