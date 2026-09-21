package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.sih_26118_dosimeter_app.MainActivity
import com.example.sih_26118_dosimeter_app.R

/**
 * Home Dashboard Fragment
 * Displays current shift hazard status, monitoring metrics, and recent scans.
 */
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val btnScan = view.findViewById<Button>(R.id.btn_action_scan)
        val btnLog = view.findViewById<Button>(R.id.btn_action_log)

        btnScan?.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_scan)
        }

        btnLog?.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_data_entry)
        }

        return view
    }
}
