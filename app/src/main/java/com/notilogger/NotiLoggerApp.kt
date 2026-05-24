package com.notilogger

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class NotiLoggerApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App comes to foreground
        if (SecurityManager.shouldLockOnForeground(this)) {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App goes to background
        if (SecurityManager.isAppLocked(this)) {
            SecurityManager.recordBackgroundTime(this)
        }
    }
}