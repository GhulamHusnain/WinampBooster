package com.gosash.winampbooster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.bottomNav).apply {
            inflateMenu(R.menu.bottom_nav)
            setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.nav_player -> show(PlayerFragment())
                    R.id.nav_booster -> show(TrueBoosterFragment())
                    R.id.nav_settings -> show(ColorSettingsFragment())
                }
                true
            }
            selectedItemId = R.id.nav_player
        }
    }

    override fun onResume() {
        super.onResume()
        ThemePrefs.applyToActivity(this)
    }

    private fun show(f: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commit()
    }
}
