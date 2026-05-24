package com.notilogger

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatDetailsActivity : AppCompatActivity() {

    private var chatId: Long = -1
    private var currentChat: TelegramChat? = null

    private lateinit var tvTitle: TextView
    private lateinit var switchFilter: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var layoutButtons: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_details)

        chatId = intent.getLongExtra("CHAT_ID", -1)
        if (chatId == -1L) { finish(); return }

        initViews()
        loadChatData()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvChatDetailsTitle)
        switchFilter = findViewById(R.id.switchChatFilter)
        tvStatus = findViewById(R.id.tvChatFilterStatus)
        layoutButtons = findViewById(R.id.layoutChatFilterButtons)

        switchFilter.setOnClickListener {
            updateFilterToggle(switchFilter.isChecked)
        }

        findViewById<View>(R.id.btnChatApps).setOnClickListener {
            val intent = Intent(this, FilterActivity::class.java)
            intent.putExtra("LEVEL", FilterActivity.LEVEL_CHAT)
            intent.putExtra("TARGET_ID", chatId)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnChatKeywords).setOnClickListener {
            val intent = Intent(this, KeywordFilterActivity::class.java)
            intent.putExtra("LEVEL", KeywordFilterActivity.LEVEL_CHAT)
            intent.putExtra("TARGET_ID", chatId)
            startActivity(intent)
        }
    }

    private fun loadChatData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val chat = AppDatabase.getDatabase(applicationContext).notificationDao().getChatById(chatId)
            withContext(Dispatchers.Main) {
                currentChat = chat
                chat?.let {
                    tvTitle.text = it.name
                    switchFilter.isChecked = it.hasCustomFilter
                    layoutButtons.visibility = if (it.hasCustomFilter) View.VISIBLE else View.GONE
                    tvStatus.text = if (it.hasCustomFilter) "Using Chat-level Custom Settings" else "Using Bot/Global Settings"
                }
            }
        }
    }

    private fun updateFilterToggle(enabled: Boolean) {
        currentChat?.let { chat ->
            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                var updatedChat = chat.copy(hasCustomFilter = enabled)
                
                // Pre-fill logic: if enabled and empty, copy from Bot (if exists) or Global
                if (enabled && chat.selectedAppsJson == "[]" && chat.keywordsJson == "[]") {
                    val gson = com.google.gson.Gson()
                    val bot = dao.getBotById(chat.botId)
                    
                    if (bot != null && bot.hasCustomFilter) {
                        // Copy from Bot
                        updatedChat = updatedChat.copy(
                            filterMode = bot.filterMode,
                            selectedAppsJson = bot.selectedAppsJson,
                            keywordsJson = bot.keywordsJson
                        )
                    } else {
                        // Copy from Global
                        val globalApps = FilterManager.getSelectedApps(applicationContext).toList()
                        val globalKeywords = KeywordManager.getGlobalKeywords(applicationContext)
                        updatedChat = updatedChat.copy(
                            filterMode = FilterManager.getMode(applicationContext),
                            selectedAppsJson = gson.toJson(globalApps),
                            keywordsJson = gson.toJson(globalKeywords)
                        )
                    }
                }
                
                dao.updateChat(updatedChat)
                withContext(Dispatchers.Main) { loadChatData() }
            }
        }
    }
}
