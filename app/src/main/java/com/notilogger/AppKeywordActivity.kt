package com.notilogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class AppKeywordActivity : AppCompatActivity() {

    private lateinit var rvKeywords: RecyclerView
    private lateinit var etKeyword: TextInputEditText
    private lateinit var switchCustom: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var layoutAdd: LinearLayout
    
    private lateinit var adapter: AppKeywordAdapter
    private var keywordList = mutableListOf<String>()
    private lateinit var packageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_keywords)

        packageName = intent.getStringExtra("PKG_NAME") ?: return
        val appName = intent.getStringExtra("APP_NAME") ?: "App"

        findViewById<TextView>(R.id.tvTitle).text = "$appName Keywords"

        rvKeywords = findViewById(R.id.rvKeywords)
        etKeyword = findViewById(R.id.etKeyword)
        switchCustom = findViewById(R.id.switchCustomFilter)
        tvStatus = findViewById(R.id.tvStatus)
        layoutAdd = findViewById(R.id.layoutAddKeyword)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAdd)

        val isCustomEnabled = KeywordManager.isCustomFilterEnabled(this, packageName)
        switchCustom.isChecked = isCustomEnabled
        
        rvKeywords.layoutManager = LinearLayoutManager(this)
        adapter = AppKeywordAdapter(keywordList) { position ->
            if (switchCustom.isChecked) {
                keywordList.removeAt(position)
                KeywordManager.setAppKeywords(this, packageName, keywordList)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, keywordList.size)
            }
        }
        rvKeywords.adapter = adapter

        loadKeywords(isCustomEnabled)

        switchCustom.setOnCheckedChangeListener { _, isChecked ->
            KeywordManager.setCustomFilterEnabled(this, packageName, isChecked)
            if (isChecked) {
                // If turning ON, check if app has any saved keywords. If not, copy globals.
                val savedAppKeywords = KeywordManager.getAppKeywords(this, packageName)
                if (savedAppKeywords.isEmpty()) {
                    val globals = KeywordManager.getGlobalKeywords(this)
                    KeywordManager.setAppKeywords(this, packageName, globals)
                }
            }
            loadKeywords(isChecked)
        }

        btnAdd.setOnClickListener {
            val newWord = etKeyword.text.toString().trim()
            if (newWord.isNotEmpty()) {
                if (!keywordList.contains(newWord)) {
                    keywordList.add(newWord)
                    KeywordManager.setAppKeywords(this, packageName, keywordList)
                    adapter.notifyItemInserted(keywordList.size - 1)
                    etKeyword.setText("")
                } else {
                    Toast.makeText(this, "Keyword already exists!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadKeywords(isCustom: Boolean) {
        keywordList.clear()
        if (isCustom) {
            keywordList.addAll(KeywordManager.getAppKeywords(this, packageName))
            tvStatus.text = "Using Custom App Keywords"
            layoutAdd.visibility = View.VISIBLE
        } else {
            keywordList.addAll(KeywordManager.getGlobalKeywords(this))
            tvStatus.text = "Using Global Keywords (View Only)"
            layoutAdd.visibility = View.GONE
        }
        adapter.notifyDataSetChanged()
    }

    inner class AppKeywordAdapter(
        private val list: List<String>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<AppKeywordAdapter.KeywordViewHolder>() {

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
            // Only show delete button if custom filter is ON
            holder.btnDelete.visibility = if (switchCustom.isChecked) View.VISIBLE else View.GONE
            holder.btnDelete.setOnClickListener {
                onDelete(holder.adapterPosition)
            }
            
            // Fade text if using global (disabled state)
            holder.tvKeyword.alpha = if (switchCustom.isChecked) 1.0f else 0.5f
        }

        override fun getItemCount() = list.size
    }
}