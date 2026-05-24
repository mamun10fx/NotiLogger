package com.notilogger

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterActivity : AppCompatActivity() {

    data class AppInfo(
        val packageName: String,
        val appName:String,
        var isSelected: Boolean
    )

    companion object {
        const val LEVEL_GLOBAL = 0
        const val LEVEL_BOT = 1
        const val LEVEL_CHAT = 2
    }

    private var level: Int = LEVEL_GLOBAL
    private var targetId: Long = -1

    private lateinit var adapter: FilterAdapter
    private lateinit var rv: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var allApps = listOf<AppInfo>()
    
    private val selectedAppsSet = mutableSetOf<String>()
    private val gson = com.google.gson.Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        level = intent.getIntExtra("LEVEL", LEVEL_GLOBAL)
        targetId = intent.getLongExtra("TARGET_ID", -1)

        rv = findViewById(R.id.rvApps)
        progressBar = findViewById(R.id.progressBar)
        val cbSystem = findViewById<CheckBox>(R.id.cbShowSystem)
        val rgMode = findViewById<RadioGroup>(R.id.rgMode)
        val rbBlacklist = findViewById<RadioButton>(R.id.rbBlacklist)
        val rbWhitelist = findViewById<RadioButton>(R.id.rbWhitelist)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)

        // Title update for better UX
        findViewById<TextView>(R.id.tvFilterTitle)?.text = when(level) {
            LEVEL_BOT -> "Bot App Filter"
            LEVEL_CHAT -> "Chat App Filter"
            else -> "Global App Filter"
        }

        setupInitialState(cbSystem, rbBlacklist, rbWhitelist)

        adapter = FilterAdapter(packageManager) { pkg, isSelected ->
            toggleAppSelection(pkg, isSelected)
        }
        rv.setHasFixedSize(true)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        cbSystem.setOnCheckedChangeListener { _, isChecked ->
            FilterManager.setShowSystem(this, isChecked)
            loadApps(isChecked)
        }

        rgMode.setOnCheckedChangeListener { _, id ->
            val mode = if (id == R.id.rbWhitelist) FilterManager.MODE_WHITELIST else FilterManager.MODE_BLACKLIST
            updateFilterMode(mode)
        }

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText ?: "")
                return true
            }
        })

        loadApps(cbSystem.isChecked)
    }

    private fun setupInitialState(cbSystem: CheckBox, rbBlacklist: RadioButton, rbWhitelist: RadioButton) {
        cbSystem.isChecked = FilterManager.getShowSystem(this)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val mode = when (level) {
                LEVEL_BOT -> AppDatabase.getDatabase(applicationContext).notificationDao().getBotById(targetId)?.filterMode ?: FilterManager.MODE_BLACKLIST
                LEVEL_CHAT -> AppDatabase.getDatabase(applicationContext).notificationDao().getChatById(targetId)?.filterMode ?: FilterManager.MODE_BLACKLIST
                else -> FilterManager.getMode(this@FilterActivity)
            }
            
            withContext(Dispatchers.Main) {
                if (mode == FilterManager.MODE_WHITELIST) rbWhitelist.isChecked = true else rbBlacklist.isChecked = true
            }
        }
    }

    private fun updateFilterMode(mode: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            when (level) {
                LEVEL_GLOBAL -> FilterManager.setMode(this@FilterActivity, mode)
                LEVEL_BOT -> {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.getBotById(targetId)?.let { dao.updateBot(it.copy(filterMode = mode)) }
                }
                LEVEL_CHAT -> {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.getChatById(targetId)?.let { dao.updateChat(it.copy(filterMode = mode)) }
                }
            }
        }
    }

    private fun toggleAppSelection(pkg: String, isSelected: Boolean) {
        if (isSelected) selectedAppsSet.add(pkg) else selectedAppsSet.remove(pkg)
        
        lifecycleScope.launch(Dispatchers.IO) {
            when (level) {
                LEVEL_GLOBAL -> FilterManager.toggleApp(this@FilterActivity, pkg, isSelected)
                LEVEL_BOT -> {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.getBotById(targetId)?.let { dao.updateBot(it.copy(selectedAppsJson = gson.toJson(selectedAppsSet.toList()))) }
                }
                LEVEL_CHAT -> {
                    val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                    dao.getChatById(targetId)?.let { dao.updateChat(it.copy(selectedAppsJson = gson.toJson(selectedAppsSet.toList()))) }
                }
            }
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { 
                it.appName.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true) 
            }
        }
        adapter.submitList(filtered)
    }

    private fun loadApps(includeSystemApps: Boolean) {
        progressBar.visibility = View.VISIBLE
        rv.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            
            // Load selected apps based on level
            val selected = when (level) {
                LEVEL_BOT -> {
                    val json = AppDatabase.getDatabase(applicationContext).notificationDao().getBotById(targetId)?.selectedAppsJson ?: "[]"
                    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, type).toSet()
                }
                LEVEL_CHAT -> {
                    val json = AppDatabase.getDatabase(applicationContext).notificationDao().getChatById(targetId)?.selectedAppsJson ?: "[]"
                    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, type).toSet()
                }
                else -> FilterManager.getSelectedApps(applicationContext)
            }
            
            selectedAppsSet.clear()
            selectedAppsSet.addAll(selected)

            val result = mutableListOf<AppInfo>()
            try {
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in apps) {
                    if (!includeSystemApps && (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                    val name = try { pm.getApplicationLabel(app)?.toString() } catch (e: Exception) { null }
                    result.add(AppInfo(packageName = app.packageName, appName = name ?: app.packageName, isSelected = selectedAppsSet.contains(app.packageName)))
                }
            } catch (e: Exception) { e.printStackTrace() }

            val sortedResult = result.sortedBy { it.appName.lowercase() }
            withContext(Dispatchers.Main) {
                allApps = sortedResult
                progressBar.visibility = View.GONE
                rv.visibility = View.VISIBLE
                filterApps(findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView).query.toString())
            }
        }
    }


    class FilterAdapter(private val pm: PackageManager, private val onToggle: (String, Boolean) -> Unit) :
        ListAdapter<AppInfo, FilterAdapter.VH>(AppDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_filter_app, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.bind(item)
        }

        override fun onViewRecycled(holder: VH) {
            super.onViewRecycled(holder)
            holder.unbind()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val img: ImageView = v.findViewById(R.id.imgIcon)
            private val name: TextView = v.findViewById(R.id.tvName)
            private val pkg: TextView = v.findViewById(R.id.tvPkg)
            private val sw: SwitchMaterial = v.findViewById(R.id.switchFilter)
            private var imageJob: Job? = null

            fun bind(item: AppInfo) {
                name.text = item.appName
                pkg.text = item.packageName
                sw.setOnCheckedChangeListener(null)
                sw.isChecked = item.isSelected

                img.setImageResource(android.R.drawable.sym_def_app_icon)
                imageJob?.cancel()

                val cachedBitmap = IconCache.getBitmap(item.packageName)
                if (cachedBitmap != null) {
                    img.setImageBitmap(cachedBitmap)
                } else {
                    imageJob = CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val drawable = pm.getApplicationIcon(item.packageName)
                            val bitmap = drawableToBitmap(drawable)
                            IconCache.putBitmap(item.packageName, bitmap)
                            withContext(Dispatchers.Main) { img.setImageBitmap(bitmap) }
                        } catch (e: Exception) { }
                    }
                }

                sw.setOnCheckedChangeListener { _, isChecked ->
                    onToggle(item.packageName, isChecked)
                    item.isSelected = isChecked
                }
            }

            fun unbind() { imageJob?.cancel() }

            private fun drawableToBitmap(drawable: Drawable): Bitmap {
                if (drawable is BitmapDrawable) return drawable.bitmap
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                return bitmap
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean = oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean = oldItem.isSelected == newItem.isSelected && oldItem.appName == newItem.appName
    }
}
