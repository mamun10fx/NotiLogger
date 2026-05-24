package com.notilogger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BotDetailsActivity : AppCompatActivity() {

    private var botId: Long = -1
    private var currentBot: TelegramBot? = null
    
    private lateinit var tvTitle: TextView
    private lateinit var switchFilter: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var layoutButtons: LinearLayout
    private lateinit var rvChats: RecyclerView
    private lateinit var adapter: ChatAdapter
    private var chatsList = mutableListOf<TelegramChat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_details)

        botId = intent.getLongExtra("BOT_ID", -1)
        if (botId == -1L) { finish(); return }

        initViews()
        setupAdapter()
        loadBotData()

        findViewById<ExtendedFloatingActionButton>(R.id.fabAddChat).setOnClickListener {
            showAddChatDialog()
        }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvBotDetailsTitle)
        switchFilter = findViewById(R.id.switchBotFilter)
        tvStatus = findViewById(R.id.tvBotFilterStatus)
        layoutButtons = findViewById(R.id.layoutBotFilterButtons)
        rvChats = findViewById(R.id.rvChats)

        switchFilter.setOnClickListener {
            updateFilterToggle(switchFilter.isChecked)
        }

        findViewById<View>(R.id.btnBotApps).setOnClickListener {
            val intent = Intent(this, FilterActivity::class.java)
            intent.putExtra("LEVEL", FilterActivity.LEVEL_BOT)
            intent.putExtra("TARGET_ID", botId)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnBotKeywords).setOnClickListener {
            val intent = Intent(this, KeywordFilterActivity::class.java)
            intent.putExtra("LEVEL", KeywordFilterActivity.LEVEL_BOT)
            intent.putExtra("TARGET_ID", botId)
            startActivity(intent)
        }
    }

    private fun setupAdapter() {
        adapter = ChatAdapter(chatsList,
            onClick = { chat ->
                val intent = Intent(this, ChatDetailsActivity::class.java)
                intent.putExtra("CHAT_ID", chat.id)
                startActivity(intent)
            },
            onDelete = { chat ->
                showDeleteChatConfirmation(chat)
            }
        )
        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = adapter
    }

    private fun loadBotData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bot = AppDatabase.getDatabase(applicationContext).notificationDao().getBotById(botId)
            val chats = AppDatabase.getDatabase(applicationContext).notificationDao().getChatsForBot(botId)
            
            withContext(Dispatchers.Main) {
                currentBot = bot
                bot?.let {
                    tvTitle.text = it.name
                    switchFilter.isChecked = it.hasCustomFilter
                    layoutButtons.visibility = if (it.hasCustomFilter) View.VISIBLE else View.GONE
                    tvStatus.text = if (it.hasCustomFilter) "Using Bot-level Custom Settings" else "Using Global Settings"
                }
                chatsList.clear()
                chatsList.addAll(chats)
                adapter.updateData(chatsList)
            }
        }
    }

    private fun updateFilterToggle(enabled: Boolean) {
        currentBot?.let { bot ->
            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                var updatedBot = bot.copy(hasCustomFilter = enabled)
                
                // Pre-fill logic: if enabled and empty, copy from Global
                if (enabled && bot.selectedAppsJson == "[]" && bot.keywordsJson == "[]") {
                    val gson = com.google.gson.Gson()
                    val globalApps = FilterManager.getSelectedApps(applicationContext).toList()
                    val globalKeywords = KeywordManager.getGlobalKeywords(applicationContext)
                    updatedBot = updatedBot.copy(
                        filterMode = FilterManager.getMode(applicationContext),
                        selectedAppsJson = gson.toJson(globalApps),
                        keywordsJson = gson.toJson(globalKeywords)
                    )
                }
                
                dao.updateBot(updatedBot)
                withContext(Dispatchers.Main) { loadBotData() }
            }
        }
    }

    private fun showAddChatDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_chat, null)
        val etName = view.findViewById<EditText>(R.id.etChatName)
        val etID = view.findViewById<EditText>(R.id.etChatID)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Chat ID")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val chatId = etID.text.toString().trim()
                if (name.isNotEmpty() && chatId.isNotEmpty()) {
                    saveChat(name, chatId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveChat(name: String, chatId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val chat = TelegramChat(botId = botId, name = name, chatId = chatId)
            AppDatabase.getDatabase(applicationContext).notificationDao().insertChat(chat)
            withContext(Dispatchers.Main) { loadBotData() }
        }
    }

    private fun showDeleteChatConfirmation(chat: TelegramChat) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Chat?")
            .setMessage("Remove '${chat.name}' from forwarding list?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).notificationDao().deleteChat(chat)
                    withContext(Dispatchers.Main) { loadBotData() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
