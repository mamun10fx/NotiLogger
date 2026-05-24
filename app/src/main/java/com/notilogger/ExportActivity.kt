package com.notilogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class ExportActivity : AppCompatActivity() {

    data class ExportAppInfo(
        val packageName: String,
        val appName: String,
        var isSelected: Boolean = true
    )

    private lateinit var rv: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ExportAdapter
    private var appsList = mutableListOf<ExportAppInfo>()

    private val createFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { performExport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        rv = findViewById(R.id.rvExportApps)
        tvCount = findViewById(R.id.tvSelectedCount)
        progressBar = findViewById(R.id.exportProgressBar)
        val btnExport = findViewById<MaterialButton>(R.id.btnExport)
        val btnSelectAll = findViewById<MaterialButton>(R.id.btnSelectAll)
        val btnUnselectAll = findViewById<MaterialButton>(R.id.btnUnselectAll)

        adapter = ExportAdapter(appsList) { updateSelectedCount() }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnSelectAll.setOnClickListener { setAllSelected(true) }
        btnUnselectAll.setOnClickListener { setAllSelected(false) }

        btnExport.setOnClickListener {
            val selected = appsList.filter { it.isSelected }.map { it.packageName }
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select at least one app", Toast.LENGTH_SHORT).show()
            } else {
                createFileLauncher.launch("NotiLogs_${System.currentTimeMillis()}.json")
            }
        }

        loadAppsWithLogs()
    }

    private fun loadAppsWithLogs() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val groups = db.notificationDao().getAppGroupsFlow() 
            // Since it's a flow we need to collect it or use a one-shot query. 
            // For export, a one-shot query is better. Let's add one to DAO if needed or just collect first item.
            // Actually, we already have getAppGroups() in previous version but we removed it. 
            // Let's just collect once.
        }
    }
    
    // Helper to load apps - refactored to be cleaner
    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
             val db = AppDatabase.getDatabase(applicationContext)
             // Fetch all logs and group by package locally for simplicity since we need app names too
             val allLogs = db.notificationDao().getAllLogsRaw()
             val pm = packageManager
             val grouped = allLogs.groupBy { it.packageName }.keys
             
             val list = grouped.map { pkg ->
                 val name = try {
                     val appInfo = pm.getApplicationInfo(pkg, 0)
                     pm.getApplicationLabel(appInfo).toString()
                 } catch (e: Exception) { pkg }
                 ExportAppInfo(pkg, name)
             }.sortedBy { it.appName.lowercase() }

             withContext(Dispatchers.Main) {
                 appsList.clear()
                 appsList.addAll(list)
                 adapter.notifyDataSetChanged()
                 progressBar.visibility = View.GONE
                 updateSelectedCount()
             }
        }
    }

    override fun onStart() {
        super.onStart()
        loadData()
    }

    private fun setAllSelected(selected: Boolean) {
        appsList.forEach { it.isSelected = selected }
        adapter.notifyDataSetChanged()
        updateSelectedCount()
    }

    private fun updateSelectedCount() {
        val count = appsList.count { it.isSelected }
        tvCount.text = "$count selected"
    }

    private fun performExport(uri: android.net.Uri) {
        val selectedPkgs = appsList.filter { it.isSelected }.map { it.packageName }
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val logs = db.notificationDao().getLogsByPackages(selectedPkgs)
            val json = Gson().toJson(logs)
            try {
                contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer -> writer.write(json) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExportActivity, "Export successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@ExportActivity, "Export failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    inner class ExportAdapter(private val list: List<ExportAppInfo>, val onToggle: () -> Unit) :
        RecyclerView.Adapter<ExportAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvAppName)
            val pkg: TextView = v.findViewById(R.id.tvLastTime) // Reusing layout
            val cb: CheckBox = v.findViewById(R.id.cbSelect)
            val img: ImageView = v.findViewById(R.id.imgIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_export_app, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.name.text = item.appName
            holder.pkg.text = item.packageName
            holder.cb.setOnCheckedChangeListener(null)
            holder.cb.isChecked = item.isSelected
            
            try {
                holder.img.setImageDrawable(packageManager.getApplicationIcon(item.packageName))
            } catch (e: Exception) {
                holder.img.setImageResource(R.drawable.ic_app_logo)
            }

            holder.cb.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onToggle()
            }
            holder.itemView.setOnClickListener { holder.cb.performClick() }
        }

        override fun getItemCount() = list.size
    }
}
