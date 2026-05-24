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
import androidx.lifecycle.lifecycleScope
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
        
        // Update Nav Header Version Dynamically
        val headerView = navView.getHeaderView(0)
        val tvHeaderVersion = headerView.findViewById<TextView>(R.id.navHeaderVersion)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvHeaderVersion.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            tvHeaderVersion.text = "v2.5.0"
        }

        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)

        adapter = AppAdapter(emptyList(), this) { pkg, name -> 
             showAppOptions(pkg, name)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        lifecycleScope.launch {
            AppDatabase.getDatabase(applicationContext).notificationDao().getAppGroupsFlow()
                .collect { groups ->
                    allGroups = groups
                    filterGroups(searchView.query.toString())
                }
        }

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
                
                R.id.nav_export -> startActivity(Intent(this, ExportActivity::class.java))
                R.id.nav_import -> importFileLauncher.launch(arrayOf("application/json"))
                
                R.id.nav_clear -> showClearConfirmation()
                R.id.nav_about -> showAboutDialog()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onResume() {
        super.onResume()
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
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).notificationDao().clearAll()
                }
                Toast.makeText(this, "Cleared!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAppOptions(packageName: String, appName: String) {
        val options = arrayOf("🗑️ Clear Logs", "🔑 Keyword Filters", "📤 Export Logs")
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
                    2 -> performSingleAppExport(packageName, appName)
                }
            }
            .show()
    }

    private fun performSingleAppExport(packageName: String, appName: String) {
        singleExportPkg = packageName
        createFileLauncher.launch("NotiLogs_${appName}_${System.currentTimeMillis()}.json")
    }

    private var singleExportPkg: String? = null

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val logs = if (singleExportPkg != null) {
                    db.notificationDao().getLogsByPackages(listOf(singleExportPkg!!))
                } else {
                    db.notificationDao().getAllLogsRaw()
                }

                val json = Gson().toJson(logs)
                try {
                    contentResolver.openOutputStream(it)?.use { os ->
                        OutputStreamWriter(os).use { writer -> writer.write(json) }
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Export successful!", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Failed to export", Toast.LENGTH_SHORT).show() }
                } finally {
                    singleExportPkg = null
                }
            }
        }
    }

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val json = contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
                    val type = object : com.google.gson.reflect.TypeToken<List<NotificationEntity>>() {}.type
                    val logs: List<NotificationEntity> = Gson().fromJson(json, type)

                    AppDatabase.getDatabase(applicationContext).notificationDao().insertBatch(logs)

                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Imported ${logs.size} logs!", Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Invalid JSON file", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
    private fun deleteLogsForPackage(pkg: String) {
         MaterialAlertDialogBuilder(this)
            .setTitle("Delete App Logs?")
            .setMessage("Delete all logs for this app?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.deleteAppLogs(pkg)
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
