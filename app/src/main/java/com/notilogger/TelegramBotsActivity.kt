package com.notilogger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TelegramBotsActivity : AppCompatActivity() {

    private lateinit var rvBots: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: BotAdapter
    private var botsList = mutableListOf<TelegramBot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_bots)

        rvBots = findViewById(R.id.rvBots)
        tvEmpty = findViewById(R.id.tvEmptyBots)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddBot)

        adapter = BotAdapter(botsList, 
            onClick = { bot ->
                val intent = Intent(this, BotDetailsActivity::class.java)
                intent.putExtra("BOT_ID", bot.id)
                startActivity(intent)
            },
            onDelete = { bot ->
                showDeleteConfirmation(bot)
            }
        )

        rvBots.layoutManager = LinearLayoutManager(this)
        rvBots.adapter = adapter

        fabAdd.setOnClickListener { showAddBotDialog() }

        loadBots()
    }

    override fun onResume() {
        super.onResume()
        loadBots()
    }

    private fun loadBots() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bots = AppDatabase.getDatabase(applicationContext).notificationDao().getAllBots()
            withContext(Dispatchers.Main) {
                botsList.clear()
                botsList.addAll(bots)
                adapter.updateData(botsList)
                tvEmpty.visibility = if (botsList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddBotDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_bot, null)
        val etName = view.findViewById<EditText>(R.id.etBotName)
        val etToken = view.findViewById<EditText>(R.id.etBotToken)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Telegram Bot")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val token = etToken.text.toString().trim()

                if (name.isNotEmpty() && token.isNotEmpty()) {
                    saveBot(name, token)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBot(name: String, token: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bot = TelegramBot(name = name, token = token)
            AppDatabase.getDatabase(applicationContext).notificationDao().insertBot(bot)
            loadBots()
        }
    }

    private fun showDeleteConfirmation(bot: TelegramBot) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Bot?")
            .setMessage("Are you sure you want to delete '${bot.name}' and all its chats?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).notificationDao().deleteBot(bot)
                    loadBots()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
