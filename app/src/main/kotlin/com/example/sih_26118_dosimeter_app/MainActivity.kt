package com.example.sih_26118_dosimeter_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sih_26118_dosimeter_app.ui.DataEntryFragment
import com.example.sih_26118_dosimeter_app.ui.HomeFragment
import com.example.sih_26118_dosimeter_app.ui.ScanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main Android Host Activity
 * Runs 100% locally on device with native XML layouts & UI fragments.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // Load Home Dashboard by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_scan -> loadFragment(ScanFragment())
                R.id.nav_data_entry -> loadFragment(DataEntryFragment())
                else -> false
            }
        }
    }

    fun switchTab(menuId: Int) {
        bottomNav.selectedItemId = menuId
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }
}
