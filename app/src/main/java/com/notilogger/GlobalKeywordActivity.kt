package com.notilogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class GlobalKeywordActivity : AppCompatActivity() {

    private lateinit var rvKeywords: RecyclerView
    private lateinit var etKeyword: TextInputEditText
    private lateinit var adapter: KeywordAdapter
    private var keywordList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_keywords)

        rvKeywords = findViewById(R.id.rvKeywords)
        etKeyword = findViewById(R.id.etKeyword)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAdd)

        keywordList.addAll(KeywordManager.getGlobalKeywords(this))

        adapter = KeywordAdapter(keywordList) { position ->
            keywordList.removeAt(position)
            KeywordManager.setGlobalKeywords(this, keywordList)
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
                    KeywordManager.setGlobalKeywords(this, keywordList)
                    adapter.notifyItemInserted(keywordList.size - 1)
                    etKeyword.setText("")
                } else {
                    Toast.makeText(this, "Keyword already exists!", Toast.LENGTH_SHORT).show()
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