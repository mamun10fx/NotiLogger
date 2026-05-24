package com.notilogger

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object SecurityManager {
    private const val PREFS_NAME = "security_prefs"
    private const val KEY_IS_LOCKED = "is_locked"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_LOCK_TYPE = "lock_type" // 0 = Password, 1 = PIN
    private const val KEY_LOCK_TIMEOUT = "lock_timeout" // in milliseconds
    private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"

    const val TYPE_PASSWORD = 0
    const val TYPE_PIN = 1
    
    const val TIMEOUT_IMMEDIATE = 0L
    const val TIMEOUT_1_MIN = 60_000L
    const val TIMEOUT_2_MIN = 120_000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAppLocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOCKED, false)
    }

    fun setAppLocked(context: Context, isLocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOCKED, isLocked).apply()
    }

    fun setPassword(context: Context, password: String, type: Int) {
        val hash = hashPassword(password)
        getPrefs(context).edit()
            .putString(KEY_PASSWORD_HASH, hash)
            .putInt(KEY_LOCK_TYPE, type)
            .apply()
    }

    fun verifyPassword(context: Context, input: String): Boolean {
        val savedHash = getPrefs(context).getString(KEY_PASSWORD_HASH, null) ?: return false
        return hashPassword(input) == savedHash
    }

    fun getLockType(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOCK_TYPE, TYPE_PASSWORD)
    }

    fun setLockTimeout(context: Context, timeout: Long) {
        getPrefs(context).edit().putLong(KEY_LOCK_TIMEOUT, timeout).apply()
    }

    fun getLockTimeout(context: Context): Long {
        return getPrefs(context).getLong(KEY_LOCK_TIMEOUT, TIMEOUT_IMMEDIATE)
    }

    fun recordBackgroundTime(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
    }

    fun shouldLockOnForeground(context: Context): Boolean {
        if (!isAppLocked(context)) return false
        
        val lastBgTime = getPrefs(context).getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBgTime == 0L) return true // App just started fresh
        
        val timeout = getLockTimeout(context)
        val timePassed = System.currentTimeMillis() - lastBgTime
        
        return timePassed >= timeout
    }
    
    fun clearBackgroundTime(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKGROUND_TIME, 0L).apply()
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}