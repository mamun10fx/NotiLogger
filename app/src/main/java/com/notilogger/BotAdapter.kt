package com.notilogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BotAdapter(
    private var list: List<TelegramBot>,
    private val onClick: (TelegramBot) -> Unit,
    private val onDelete: (TelegramBot) -> Unit
) : RecyclerView.Adapter<BotAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBotName: TextView = view.findViewById(R.id.tvBotName)
        val tvBotToken: TextView = view.findViewById(R.id.tvBotToken)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteBot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_telegram_bot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bot = list[position]
        holder.tvBotName.text = bot.name
        holder.tvBotToken.text = "Token: ${bot.token.take(10)}..."
        
        holder.itemView.setOnClickListener { onClick(bot) }
        holder.btnDelete.setOnClickListener { onDelete(bot) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<TelegramBot>) {
        list = newList
        notifyDataSetChanged()
    }
}
