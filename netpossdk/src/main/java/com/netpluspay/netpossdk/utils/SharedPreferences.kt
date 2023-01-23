package com.netpluspay.netpossdk.utils // ktlint-disable filename

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log

class AppSharedPreferences {
    private val keyTag = "key_tag"
    private val prefsName = "app_prefs_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("MyPref", MODE_PRIVATE)
    }

    fun putString(data: String, context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()?.putString(keyTag, data)?.apply()
    }

    fun getString(context: Context): String {
        val prefs = getPrefs(context)
        val savedKeyTag =  prefs.getString(keyTag, "").toString()
        Log.d("SAVED_KEY_TAG", savedKeyTag)
        return savedKeyTag
    }
}
