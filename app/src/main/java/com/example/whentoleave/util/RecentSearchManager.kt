package com.example.whentoleave.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RecentSearchManager {

    private const val PREFS_NAME = "recent_searches"
    private const val KEY = "searches"
    private const val MAX = 5

    data class RecentSearch(val startAddress: String, val endAddress: String)

    fun save(context: Context, start: String, end: String) {
        if (start.isBlank() || end.isBlank()) return
        val list = getAll(context).toMutableList()
        list.removeAll { it.startAddress == start && it.endAddress == end }
        list.add(0, RecentSearch(start, end))
        val arr = JSONArray()
        list.take(MAX).forEach { item ->
            arr.put(JSONObject().apply {
                put("s", item.startAddress)
                put("e", item.endAddress)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun getAll(context: Context): List<RecentSearch> {
        val str = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                RecentSearch(obj.getString("s"), obj.getString("e"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
