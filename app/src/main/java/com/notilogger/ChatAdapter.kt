package com.notilogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private var list: List<TelegramChat>,
    private val onClick: (TelegramChat) -> Unit,
    private val onDelete: (TelegramChat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChatName: TextView = view.findViewById(R.id.tvChatName)
        val tvChatID: TextView = view.findViewById(R.id.tvChatID)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_telegram_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = list[position]
        holder.tvChatName.text = chat.name
        holder.tvChatID.text = "ID: ${chat.chatId}"
        
        holder.itemView.setOnClickListener { onClick(chat) }
        holder.btnDelete.setOnClickListener { onDelete(chat) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<TelegramChat>) {
        list = newList
        notifyDataSetChanged()
    }
}
