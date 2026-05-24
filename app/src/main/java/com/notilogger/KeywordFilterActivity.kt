package com.notilogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeywordFilterActivity : AppCompatActivity() {

    companion object {
        const val LEVEL_GLOBAL = 0
        const val LEVEL_BOT = 1
        const val LEVEL_CHAT = 2
        const val LEVEL_APP = 3 // For individual app custom keywords
    }

    private var level: Int = LEVEL_GLOBAL
    private var targetId: Long = -1
    private var packageName: String? = null

    private lateinit var rvKeywords: RecyclerView
    private lateinit var etKeyword: TextInputEditText
    private lateinit var adapter: KeywordAdapter
    private var keywordList = mutableListOf<String>()
    private val gson = com.google.gson.Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyword_filter)

        level = intent.getIntExtra("LEVEL", LEVEL_GLOBAL)
        targetId = intent.getLongExtra("TARGET_ID", -1)
        packageName = intent.getStringExtra("PKG_NAME")

        rvKeywords = findViewById(R.id.rvKeywords)
        etKeyword = findViewById(R.id.etKeyword)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAdd)
        val tvTitle = findViewById<TextView>(R.id.tvKeywordTitle)
        val tvDesc = findViewById<TextView>(R.id.tvKeywordDesc)

        tvTitle.text = when(level) {
            LEVEL_BOT -> "Bot Keyword Filter"
            LEVEL_CHAT -> "Chat Keyword Filter"
            LEVEL_APP -> "${intent.getStringExtra("APP_NAME")} Keywords"
            else -> "Keywords Filtering"
        }
        
        tvDesc.text = if (level == LEVEL_GLOBAL) 
            "Notifications containing these words will be blocked globally." 
            else "Notifications containing these words will be blocked for this selection."

        adapter = KeywordAdapter(keywordList) { position ->
            keywordList.removeAt(position)
            saveKeywords()
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, keywordList.size)
        }

        rvKeywords.layoutManager = LinearLayoutManager(this)
        rvKeywords.adapter = adapter

        btnAdd.setOnClickListener {
            val newWord = etKeyword.text.toString().trim()
            if (newWord.isNotEmpty()) {
                if (!keywordList.contains(newWord)) {
                    keywordList.add(newWord)
                    saveKeywords()
                    adapter.notifyItemInserted(keywordList.size - 1)
                    etKeyword.setText("")
                } else {
                    Toast.makeText(this, "Keyword already exists!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadKeywords()
    }

    private fun loadKeywords() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = when (level) {
                LEVEL_GLOBAL -> KeywordManager.getGlobalKeywords(this@KeywordFilterActivity)
                LEVEL_BOT -> {
                    val json = AppDatabase.getDatabase(applicationContext).notificationDao().getBotById(targetId)?.keywordsJson ?: "[]"
                    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, type)
                }
                LEVEL_CHAT -> {
                    val json = AppDatabase.getDatabase(applicationContext).notificationDao().getChatById(targetId)?.keywordsJson ?: "[]"
                    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, type)
                }
                LEVEL_APP -> {
                    packageName?.let { KeywordManager.getAppKeywords(this@KeywordFilterActivity, it) } ?: emptyList()
                }
                else -> emptyList()
            }

            withContext(Dispatchers.Main) {
                keywordList.clear()
                keywordList.addAll(list)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun saveKeywords() {
        lifecycleScope.launch(Dispatchers.IO) {
            val json = gson.toJson(keywordList)
            when (level) {
                LEVEL_GLOBAL -> KeywordManager.setGlobalKeywords(this@KeywordFilterActivity, keywordList)
                LEVEL_BOT -> {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.getBotById(targetId)?.let { dao.updateBot(it.copy(keywordsJson = json)) }
                }
                LEVEL_CHAT -> {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.getChatById(targetId)?.let { dao.updateChat(it.copy(keywordsJson = json)) }
                }
                LEVEL_APP -> {
                    packageName?.let { KeywordManager.setAppKeywords(this@KeywordFilterActivity, it, keywordList) }
                }
            }
        }
    }

    inner class KeywordAdapter(
        private val list: List<String>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder>() {

        inner class KeywordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvKeyword: TextView = view.findViewById(R.id.tvKeyword)
            val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_keyword, parent, false)
            return KeywordViewHolder(view)
        }

        override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
            holder.tvKeyword.text = list[position]
            holder.btnDelete.setOnClickListener {
                onDelete(holder.adapterPosition)
            }
        }

        override fun getItemCount() = list.size
    }
}
