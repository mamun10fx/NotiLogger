package com.notilogger

import android.graphics.Bitmap
import android.util.LruCache

object IconCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getBitmap(packageName: String): Bitmap? {
        return memoryCache.get(packageName)
    }

    fun putBitmap(packageName: String, bitmap: Bitmap) {
        if (getBitmap(packageName) == null) {
            memoryCache.put(packageName, bitmap)
        }
    }
}
