package com.notilogger

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object KeywordManager {
    private const val PREFS_NAME = "keyword_prefs"
    private const val KEY_GLOBAL_KEYWORDS = "global_keywords"
    private const val PREFIX_APP_ENABLED = "custom_enabled_"
    private const val PREFIX_APP_KEYWORDS = "app_keywords_"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGlobalKeywords(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_GLOBAL_KEYWORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun setGlobalKeywords(context: Context, keywords: List<String>) {
        val json = gson.toJson(keywords)
        getPrefs(context).edit().putString(KEY_GLOBAL_KEYWORDS, json).apply()
    }

    fun isCustomFilterEnabled(context: Context, packageName: String): Boolean {
        return getPrefs(context).getBoolean(PREFIX_APP_ENABLED + packageName, false)
    }

    fun setCustomFilterEnabled(context: Context, packageName: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(PREFIX_APP_ENABLED + packageName, enabled).apply()
    }

    fun getAppKeywords(context: Context, packageName: String): List<String> {
        val json = getPrefs(context).getString(PREFIX_APP_KEYWORDS + packageName, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun setAppKeywords(context: Context, packageName: String, keywords: List<String>) {
        val json = gson.toJson(keywords)
        getPrefs(context).edit().putString(PREFIX_APP_KEYWORDS + packageName, json).apply()
    }
    
    // Core Engine Logic: Should we block this notification based on keywords?
    fun shouldBlockNotification(context: Context, packageName: String, title: String, content: String): Boolean {
        val textToSearch = "$title $content".lowercase()
        
        val keywords = if (isCustomFilterEnabled(context, packageName)) {
            getAppKeywords(context, packageName)
        } else {
            getGlobalKeywords(context)
        }
        
        for (keyword in keywords) {
            if (keyword.isNotEmpty() && textToSearch.contains(keyword.lowercase())) {
                return true // Match found, should block
            }
        }
        return false // No match, do not block
    }
}