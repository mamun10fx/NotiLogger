package com.notilogger

import android.content.Context

object FilterManager {
    private const val PREF_NAME = "filter_prefs"
    private const val KEY_MODE = "filter_mode"
    private const val KEY_APPS = "filter_apps"
    private const val KEY_SYSTEM = "show_system"

    const val MODE_BLACKLIST = 0
    const val MODE_WHITELIST = 1

    fun getSelectedApps(context: Context): HashSet<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        return prefs.getStringSet(KEY_APPS, null)?.toHashSet() ?: HashSet()
    }

    fun setMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_MODE, mode).apply()
    }

    fun getMode(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_MODE, MODE_BLACKLIST)
    }

    fun setShowSystem(context: Context, show: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SYSTEM, show).apply()
    }

    fun getShowSystem(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SYSTEM, false)
    }

    fun toggleApp(context: Context, pkg: String, isSelected: Boolean) {
        
        val apps = getSelectedApps(context)
        if (isSelected) {
            apps.add(pkg)
        } else {
            apps.remove(pkg)
        }
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_APPS, apps).apply()
    }

    fun isAppSelected(context: Context, pkg: String): Boolean {
        return getSelectedApps(context).contains(pkg)
    }

    fun shouldLog(context: Context, pkg: String): Boolean {
        val mode = getMode(context)
        val isSelected = isAppSelected(context, pkg)

        return if (mode == MODE_WHITELIST) {
            isSelected
        } else {
            
            !isSelected
        }
    }
}
