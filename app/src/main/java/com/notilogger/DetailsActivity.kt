package com.notilogger

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class DetailsActivity : AppCompatActivity() {

    private lateinit var adapter: DetailAdapter
    private var allLogs = listOf<NotificationEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val pkgName = intent.getStringExtra("PKG_NAME") ?: return
        val appName = intent.getStringExtra("APP_NAME") ?: "Logs"

        findViewById<TextView>(R.id.tvTitle).text = "$appName Logs"

        val rv = findViewById<RecyclerView>(R.id.rvDetails)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        
        rv.layoutManager = LinearLayoutManager(this)
        adapter = DetailAdapter(emptyList())
        rv.adapter = adapter

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterLogs(newText ?: "")
                return true
            }
        })
        
        lifecycleScope.launch {
            AppDatabase.getDatabase(applicationContext)
                .notificationDao()
                .getLogsByPackage(pkgName)
                .collect { logs ->
                    allLogs = logs
                    filterLogs(searchView.query.toString())
                    swipeRefresh.isRefreshing = false 
                    
                    // Mark as seen
                    if (logs.any { !it.isSeen }) {
                        AppDatabase.getDatabase(applicationContext).notificationDao().markAsSeen(pkgName)
                    }
                }
        }

        
        swipeRefresh.setOnRefreshListener {
            
            swipeRefresh.postDelayed({ swipeRefresh.isRefreshing = false }, 1000)
        }
    }

    private fun filterLogs(query: String) {
        if (query.isEmpty()) {
            adapter.updateData(allLogs)
            return
        }
        val filtered = allLogs.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.content.contains(query, ignoreCase = true) 
        }
        adapter.updateData(filtered)
    }
}
