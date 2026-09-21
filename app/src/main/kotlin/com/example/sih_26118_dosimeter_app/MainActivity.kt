package com.example.sih_26118_dosimeter_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sih_26118_dosimeter_app.ui.HistoryFragment
import com.example.sih_26118_dosimeter_app.ui.HomeFragment
import com.example.sih_26118_dosimeter_app.ui.RegistrationFragment
import com.example.sih_26118_dosimeter_app.ui.ScanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * DoseGuard Main Activity
 * Manages core navigation across Dashboard, Scanner, Registration, and History screens.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_scan -> loadFragment(ScanFragment())
                R.id.nav_register -> loadFragment(RegistrationFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
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
