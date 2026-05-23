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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val pkgName = intent.getStringExtra("PKG_NAME") ?: return
        val appName = intent.getStringExtra("APP_NAME") ?: "Logs"

        findViewById<TextView>(R.id.tvTitle).text = "$appName Logs"

        val rv = findViewById<RecyclerView>(R.id.rvDetails)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = DetailAdapter(emptyList())
        rv.adapter = adapter

        
        
        lifecycleScope.launch {
            AppDatabase.getDatabase(applicationContext)
                .notificationDao()
                .getLogsByPackage(pkgName)
                .collect { logs ->
                    adapter.updateData(logs)
                    swipeRefresh.isRefreshing = false 
                }
        }

        
        swipeRefresh.setOnRefreshListener {
            
            swipeRefresh.postDelayed({ swipeRefresh.isRefreshing = false }, 1000)
        }
    }
}
