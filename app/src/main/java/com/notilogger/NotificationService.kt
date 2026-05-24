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
            AppDatabase.getDatabase(applicationContext).notificationDao().insert(entry)
        }
    }
}
