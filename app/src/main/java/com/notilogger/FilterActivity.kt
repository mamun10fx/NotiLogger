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

    private lateinit var adapter: FilterAdapter
    private lateinit var rv: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        rv = findViewById(R.id.rvApps)
        progressBar = findViewById(R.id.progressBar)
        val cbSystem = findViewById<CheckBox>(R.id.cbShowSystem)
        val rgMode = findViewById<RadioGroup>(R.id.rgMode)
        val rbBlacklist = findViewById<RadioButton>(R.id.rbBlacklist)
        val rbWhitelist = findViewById<RadioButton>(R.id.rbWhitelist)

        cbSystem.isChecked = FilterManager.getShowSystem(this)
        if (FilterManager.getMode(this) == FilterManager.MODE_WHITELIST) {
            rbWhitelist.isChecked = true
        } else {
            rbBlacklist.isChecked = true
        }

        adapter = FilterAdapter(packageManager)
        rv.setHasFixedSize(true)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        cbSystem.setOnCheckedChangeListener { _, isChecked ->
            FilterManager.setShowSystem(this, isChecked)
            loadApps(isChecked)
        }

        rgMode.setOnCheckedChangeListener { _, id ->
            val mode = if (id == R.id.rbWhitelist) FilterManager.MODE_WHITELIST else FilterManager.MODE_BLACKLIST
            FilterManager.setMode(this, mode)
        }

        loadApps(cbSystem.isChecked)
    }

    private fun loadApps(includeSystemApps: Boolean) {
        progressBar.visibility = View.VISIBLE
        rv.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val selectedApps = FilterManager.getSelectedApps(applicationContext)
            val result = mutableListOf<AppInfo>()

            try {
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                for (app in apps) {
                    if (!includeSystemApps && (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue
                    }

                    val name = try {
                        pm.getApplicationLabel(app)?.toString()
                    } catch (e: Exception) {
                        null 
                    }

                    val isSelected = selectedApps.contains(app.packageName)
                    result.add(
                        AppInfo(
                            packageName = app.packageName,
                            appName = name ?: app.packageName,
                            isSelected = isSelected
                        )
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FilterActivity, "Error: Permission denied.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                 withContext(Dispatchers.Main) {
                    Toast.makeText(this@FilterActivity, "Error: Out of memory.", Toast.LENGTH_SHORT).show()
                }
            }

            val sortedResult = result.sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                rv.visibility = View.VISIBLE
                adapter.submitList(sortedResult)
            }
        }
    }


    class FilterAdapter(private val pm: PackageManager) :
        ListAdapter<AppInfo, FilterAdapter.VH>(AppDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_filter_app, parent, false)
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
                            withContext(Dispatchers.Main) {
                                img.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            Log.e("FilterAdapter", "Error loading icon for ${item.packageName}", e)
                        }
                    }
                }

                sw.setOnCheckedChangeListener { _, isChecked ->
                    FilterManager.toggleApp(itemView.context, item.packageName, isChecked)
                    item.isSelected = isChecked
                }
            }

            fun unbind() {
                imageJob?.cancel()
            }

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
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.isSelected == newItem.isSelected && oldItem.appName == newItem.appName
        }
    }
}
