package com.gosash.winampbooster

import android.content.Context
import org.json.JSONArray

object FavStore {
    private const val PREF = "favs"
    private const val KEY = "urls"

    fun getAll(ctx: Context): List<String> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun add(ctx: Context, url: String) {
        val list = getAll(ctx).toMutableList()
        if (!list.contains(url)) list.add(0, url)
        save(ctx, list)
    }

    fun remove(ctx: Context, url: String) {
        val list = getAll(ctx).toMutableList()
        list.remove(url)
        save(ctx, list)
    }

    private fun save(ctx: Context, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
