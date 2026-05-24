package com.notilogger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramForwarder {

    fun sendMessage(token: String, chatId: String, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlString = "https://api.telegram.org/bot$token/sendMessage"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "chat_id=${URLEncoder.encode(chatId, "UTF-8")}&text=${URLEncoder.encode(message, "UTF-8")}&parse_mode=HTML"
                
                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Log error if needed
                    println("Telegram API Error: $responseCode")
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
