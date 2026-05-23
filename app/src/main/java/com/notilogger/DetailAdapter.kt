package com.notilogger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailAdapter(private var list: List<NotificationEntity>) :
    RecyclerView.Adapter<DetailAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvNotiTitle)
        val tvContent: TextView = view.findViewById(R.id.tvNotiContent)
        val tvTime: TextView = view.findViewById(R.id.tvNotiTime)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val timeStr = sdf.format(Date(item.timestamp))

        holder.tvTitle.text = item.title
        holder.tvContent.text = item.content
        holder.tvTime.text = timeStr

        
        holder.btnCopy.setOnClickListener {
            val context = holder.itemView.context
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val textToCopy = "App: ${item.packageName}\nTitle: ${item.title}\nMessage: ${item.content}\nTime: $timeStr"
            val clip = ClipData.newPlainText("Notification Log", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = list.size
    
    fun updateData(newList: List<NotificationEntity>) {
        list = newList
        notifyDataSetChanged()
    }
}
