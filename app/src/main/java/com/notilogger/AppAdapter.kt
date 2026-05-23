package com.notilogger

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AppAdapter(
    private var list: List<AppGroup>, 
    private val context: Context,
    private val onLongClick: (String) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvLastTime: TextView = view.findViewById(R.id.tvLastTime)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val pm = context.packageManager

        
        var appName = item.packageName 
        try {
            val appInfo = pm.getApplicationInfo(item.packageName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            holder.imgIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (e: Exception) {
            holder.imgIcon.setImageResource(R.drawable.ic_app_logo)
        }

        holder.tvAppName.text = appName
        holder.tvAppName.setTextColor(android.graphics.Color.WHITE) 
        holder.tvCount.text = item.count.toString()
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.tvLastTime.text = sdf.format(Date(item.lastTime))

        
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailsActivity::class.java)
            intent.putExtra("PKG_NAME", item.packageName)
            intent.putExtra("APP_NAME", appName) 
            context.startActivity(intent)
        }

        
        holder.itemView.setOnLongClickListener {
            onLongClick(item.packageName)
            true
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<AppGroup>) {
        list = newList
        notifyDataSetChanged()
    }
}
