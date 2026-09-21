package com.example.sih_26118_dosimeter_app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sih_26118_dosimeter_app.MainActivity
import com.example.sih_26118_dosimeter_app.R
import com.google.android.material.button.MaterialButton

class ScanFragment : Fragment() {

    private lateinit var viewModel: ScanViewModel
    private var simulatedDeltaE: Double = 8.50

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        val tvSubstratePreview = view.findViewById<TextView>(R.id.tv_sensor_substrate_preview)
        val btnChipSafe = view.findViewById<MaterialButton>(R.id.btn_chip_safe)
        val btnChipCaution = view.findViewById<MaterialButton>(R.id.btn_chip_caution)
        val btnChipCritical = view.findViewById<MaterialButton>(R.id.btn_chip_critical)
        val btnCapture = view.findViewById<MaterialButton>(R.id.btn_capture_scan)

        val tvRisk = view.findViewById<TextView>(R.id.tv_scan_risk_tag)
        val tvDeltaE = view.findViewById<TextView>(R.id.tv_scan_delta_e)
        val tvDose = view.findViewById<TextView>(R.id.tv_scan_dose)
        val tvTwa = view.findViewById<TextView>(R.id.tv_scan_twa)
        val tvAction = view.findViewById<TextView>(R.id.tv_scan_action)
        val tvHash = view.findViewById<TextView>(R.id.tv_scan_hash)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_scan)

        fun updateSubstrateColor(deltaE: Double) {
            // Simulate colorimetric reaction darkening from pristine cream to bismuth sulfide dark gray
            val factor = (1.0 - (deltaE / 75.0).coerceIn(0.0, 0.85))
            val r = (239 * factor).toInt()
            val g = (235 * factor).toInt()
            val b = (233 * factor).toInt()
            tvSubstratePreview.setBackgroundColor(Color.rgb(r, g, b))
            tvSubstratePreview.setTextColor(if (factor < 0.5) Color.WHITE else Color.BLACK)
        }

        // Preset Chips
        btnChipSafe.setOnClickListener {
            simulatedDeltaE = 4.20
            updateSubstrateColor(simulatedDeltaE)
            viewModel.analyzeReading(simulatedDeltaE)
        }

        btnChipCaution.setOnClickListener {
            simulatedDeltaE = 18.50
            updateSubstrateColor(simulatedDeltaE)
            viewModel.analyzeReading(simulatedDeltaE)
        }

        btnChipCritical.setOnClickListener {
            simulatedDeltaE = 48.00
            updateSubstrateColor(simulatedDeltaE)
            viewModel.analyzeReading(simulatedDeltaE)
        }

        btnCapture.setOnClickListener {
            // Increment or randomize simulated scan reading
            simulatedDeltaE = (simulatedDeltaE + 6.25) % 65.0
            if (simulatedDeltaE < 3.0) simulatedDeltaE = 5.80
            updateSubstrateColor(simulatedDeltaE)
            viewModel.analyzeReading(simulatedDeltaE)
            Toast.makeText(context, "Optical frame processed with ArUco homography!", Toast.LENGTH_SHORT).show()
        }

        // Observe Dose Result
        viewModel.doseResult.observe(viewLifecycleOwner) { result ->
            val context = context ?: return@observe

            tvDeltaE.text = "ΔE ${String.format("%.2f", viewModel.currentDeltaE.value ?: 8.50)}"
            tvDose.text = "${result.dosePpmHr} ppm·hr"
            tvTwa.text = "${result.twa8hrPpm} ppm"
            tvRisk.text = result.riskLevel
            tvAction.text = result.actionRequired

            when (result.riskLevel.uppercase()) {
                "SAFE" -> {
                    tvRisk.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                    tvAction.setBackgroundColor(ContextCompat.getColor(context, R.color.status_safe_bg))
                }
                "CAUTION", "MODERATE" -> {
                    tvRisk.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    tvAction.setBackgroundColor(ContextCompat.getColor(context, R.color.status_warning_bg))
                }
                "HIGH" -> {
                    tvRisk.setTextColor(ContextCompat.getColor(context, R.color.status_high))
                    tvAction.setBackgroundColor(ContextCompat.getColor(context, R.color.status_high_bg))
                }
                else -> {
                    tvRisk.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                    tvAction.setBackgroundColor(ContextCompat.getColor(context, R.color.status_danger_bg))
                }
            }
        }

        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "Syncing to Online Database..."
            viewModel.saveCurrentScan(
                workerId = "W-1001",
                workerName = "Rajesh Kumar",
                department = "Refinery Sweetening Unit",
                bandId = "BAND-9988",
                durationHours = 8.0
            )
        }

        viewModel.saveStatus.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ScanViewModel.SaveState.Saving -> {
                    btnSave.isEnabled = false
                    btnSave.text = "Syncing to Online Database..."
                }
                is ScanViewModel.SaveState.Success -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Save & Sync to Online Database"
                    val msg = if (state.isOnlineSynced) {
                        "Scan record synced to Online Cloud Database! (ID: ${state.logId})"
                    } else {
                        "Saved to Offline Room Database. Will sync when online."
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    viewModel.resetSaveState()
                    (activity as? MainActivity)?.switchTab(R.id.nav_history)
                }
                is ScanViewModel.SaveState.Error -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Save & Sync to Online Database"
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveState()
                }
                else -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Save & Sync to Online Database"
                }
            }
        }

        return view
    }
}
