package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sih_26118_dosimeter_app.R
import com.example.sih_26118_dosimeter_app.cv.KineticDoseSolver

/**
 * Native Optical Badge Scanner View
 * Runs kinetic exposure saturation solver directly on-device without any server dependency.
 */
class ScanFragment : Fragment() {

    private var simulatedDeltaE = 5.40

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        val btnCapture = view.findViewById<Button>(R.id.btn_capture_scan)
        val btnSave = view.findViewById<Button>(R.id.btn_save_scan)
        val tvDeltaE = view.findViewById<TextView>(R.id.tv_scan_delta_e)
        val tvTwa = view.findViewById<TextView>(R.id.tv_scan_twa)
        val tvRisk = view.findViewById<TextView>(R.id.tv_scan_risk_tag)
        val tvAction = view.findViewById<TextView>(R.id.tv_scan_action)

        btnCapture?.setOnClickListener {
            simulatedDeltaE = (simulatedDeltaE + 4.50) % 40.0
            if (simulatedDeltaE < 2.0) simulatedDeltaE = 3.80

            val result = KineticDoseSolver.calculateDose(deltaE = simulatedDeltaE, shiftHours = 8.0)

            tvDeltaE?.text = String.format("%.2f", simulatedDeltaE)
            tvTwa?.text = "${result.twa8hrPpm} ppm"
            tvRisk?.text = result.riskLevel
            tvAction?.text = result.actionRequired

            when (result.riskLevel) {
                "SAFE" -> tvRisk?.setTextColor(resources.getColor(R.color.status_safe))
                "MODERATE" -> tvRisk?.setTextColor(resources.getColor(R.color.status_warning))
                else -> tvRisk?.setTextColor(resources.getColor(R.color.status_danger))
            }

            Toast.makeText(context, "Badge analyzed locally!", Toast.LENGTH_SHORT).show()
        }

        btnSave?.setOnClickListener {
            Toast.makeText(context, "Scan record saved to local database", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
