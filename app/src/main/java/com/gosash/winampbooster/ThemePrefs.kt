package com.gosash.winampbooster

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView

object ThemePrefs {
    private const val PREF = "theme"
    private const val BG = "bg"
    private const val AC = "ac"
    private const val TX = "tx"

    fun bg(ctx: Context) = ctx.sp().getInt(BG, Color.rgb(15, 20, 18))
    fun accent(ctx: Context) = ctx.sp().getInt(AC, Color.rgb(0, 255, 140))
    fun text(ctx: Context) = ctx.sp().getInt(TX, Color.rgb(230, 255, 240))

    fun save(ctx: Context, bg: Int, ac: Int, tx: Int) {
        ctx.sp().edit().putInt(BG, bg).putInt(AC, ac).putInt(TX, tx).apply()
    }

    fun applyToActivity(a: Activity) {
        val root = a.findViewById<View>(android.R.id.content)
        applyToView(root)
    }

    fun applyToView(v: View) {
        val ctx = v.context
        val bgC = bg(ctx)
        val acC = accent(ctx)
        val txC = text(ctx)

        fun walk(view: View) {
            if (view is ViewGroup) view.setBackgroundColor(bgC)
            if (view is TextView) view.setTextColor(txC)

            if (view is Button) {
                view.setBackgroundColor(acC)
                view.setTextColor(Color.BLACK)
            }

            if (view is BottomNavigationView) {
                view.setBackgroundColor(bgC)
                view.itemIconTintList = ColorStateList.valueOf(acC)
                view.itemTextColor = ColorStateList.valueOf(txC)
            }

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(v)
    }

    private fun Context.sp() = getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
