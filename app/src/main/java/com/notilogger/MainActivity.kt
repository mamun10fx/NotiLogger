package com.notilogger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var tvEmpty: TextView
    private var allGroups = listOf<AppGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        tvEmpty = findViewById(R.id.tvEmpty)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)

        adapter = AppAdapter(emptyList(), this) { pkg, name -> 
             showAppOptions(pkg, name)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterGroups(newText ?: "")
                return true
            }
        })

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_permission -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                R.id.nav_battery -> ignoreBatteryOptimization()
                
                R.id.nav_filter -> startActivity(Intent(this, FilterActivity::class.java).apply { putExtra("LEVEL", FilterActivity.LEVEL_GLOBAL) })
                R.id.nav_security -> startActivity(Intent(this, SecurityActivity::class.java))
                R.id.nav_global_keywords -> startActivity(Intent(this, KeywordFilterActivity::class.java).apply { putExtra("LEVEL", KeywordFilterActivity.LEVEL_GLOBAL) })
                R.id.nav_telegram -> startActivity(Intent(this, TelegramBotsActivity::class.java))
                
                R.id.nav_save -> createFileLauncher.launch("NotiLogs_${System.currentTimeMillis()}.json")
                R.id.nav_clear -> showClearConfirmation()
                R.id.nav_about -> showAboutDialog()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    private fun loadLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            val groups = AppDatabase.getDatabase(applicationContext).notificationDao().getAppGroups()
            withContext(Dispatchers.Main) {
                allGroups = groups
                filterGroups(findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView).query.toString())
            }
        }
    }

    private fun filterGroups(query: String) {
        if (query.isEmpty()) {
            adapter.updateData(allGroups)
            tvEmpty.visibility = if (allGroups.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        val pm = packageManager
        val filtered = allGroups.filter { group ->
            val appName = try {
                val appInfo = pm.getApplicationInfo(group.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                group.packageName
            }
            appName.contains(query, ignoreCase = true) || group.packageName.contains(query, ignoreCase = true)
        }
        
        adapter.updateData(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showClearConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear All Logs?")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getDatabase(applicationContext).notificationDao().clearAll()
                    loadLogs()
                }
                Toast.makeText(this, "Cleared!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAppOptions(packageName: String, appName: String) {
        val options = arrayOf("🗑️ Clear Logs", "🔑 Keyword Filters")
        MaterialAlertDialogBuilder(this)
            .setTitle(appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteLogsForPackage(packageName)
                    1 -> {
                        val intent = Intent(this, KeywordFilterActivity::class.java)
                        intent.putExtra("LEVEL", KeywordFilterActivity.LEVEL_APP)
                        intent.putExtra("PKG_NAME", packageName)
                        intent.putExtra("APP_NAME", appName)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun deleteLogsForPackage(pkg: String) {
         MaterialAlertDialogBuilder(this)
            .setTitle("Delete App Logs?")
            .setMessage("Delete all logs for this app?")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.deleteAppLogs(pkg)
                    loadLogs()
                }
                Toast.makeText(this, "Logs deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun ignoreBatteryOptimization() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try { startActivity(intent) } catch (e: Exception) {
            Toast.makeText(this, "Search 'Battery Optimization' in Settings", Toast.LENGTH_LONG).show()
        }
    }

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val allLogs = AppDatabase.getDatabase(applicationContext).notificationDao().getAllLogsRaw() 
                val json = Gson().toJson(allLogs)
                try {
                    contentResolver.openOutputStream(it)?.use { os ->
                        OutputStreamWriter(os).use { writer -> writer.write(json) }
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Saved successfully!", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Failed to save", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnEmail).setOnClickListener { openLink("mailto:mamun10fx@gmail.com") }
        dialogView.findViewById<View>(R.id.btnFB).setOnClickListener { openLink("https://www.facebook.com/profile.php?id=61583220766712") }
        dialogView.findViewById<View>(R.id.btnWA).setOnClickListener { openLink("https://wa.me/+8801788784668") }
        dialogView.findViewById<View>(R.id.btnTG).setOnClickListener { openLink("https://t.me/mamun10sc") }
        dialogView.findViewById<View>(R.id.btnInsta).setOnClickListener { openLink("https://www.instagram.com/mamun10xc") }
        dialogView.findViewById<View>(R.id.btnGit).setOnClickListener { openLink("https://github.com/mamun10fx") }
        dialogView.findViewById<View>(R.id.btnYT).setOnClickListener { openLink("https://m.youtube.com/@mamun10fx") }
        
        dialog.show()
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}
