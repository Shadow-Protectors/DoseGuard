package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sih_26118_dosimeter_app.MainActivity
import com.example.sih_26118_dosimeter_app.R
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    private lateinit var viewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val cardAlert = view.findViewById<CardView>(R.id.card_hazard_alert)
        val tvAlertTitle = view.findViewById<TextView>(R.id.tv_alert_title)
        val tvAlertMessage = view.findViewById<TextView>(R.id.tv_alert_message)

        val tvWorkerName = view.findViewById<TextView>(R.id.tv_home_worker_name)
        val tvWorkerDetails = view.findViewById<TextView>(R.id.tv_home_worker_details)
        val tvBadgeAssigned = view.findViewById<TextView>(R.id.tv_home_badge_assigned)
        val tvShiftStatus = view.findViewById<TextView>(R.id.tv_home_shift_status)

        val tvDose = view.findViewById<TextView>(R.id.tv_metric_dose)
        val tvTwa = view.findViewById<TextView>(R.id.tv_metric_twa)
        val tvScans = view.findViewById<TextView>(R.id.tv_metric_scans)
        val tvCompliance = view.findViewById<TextView>(R.id.tv_metric_compliance)

        val btnScan = view.findViewById<MaterialButton>(R.id.btn_action_scan)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btn_action_register)
        val btnSync = view.findViewById<MaterialButton>(R.id.btn_action_sync_cloud)

        // Observe Latest Worker Profile
        viewModel.latestWorker.observe(viewLifecycleOwner) { worker ->
            if (worker != null) {
                tvWorkerName.text = worker.name
                tvWorkerDetails.text = "ID: ${worker.workerId} • ${worker.department}"
                tvBadgeAssigned.text = "Active Badge: ${worker.assignedBandId}"
            } else {
                tvWorkerName.text = "Rajesh Kumar"
                tvWorkerDetails.text = "ID: W-1001 • Refinery Sweetening Unit"
                tvBadgeAssigned.text = "Active Badge: BAND-9988"
            }
        }

        // Observe Latest Shift Scan & Hazard Threshold Alert
        viewModel.latestLog.observe(viewLifecycleOwner) { latestLog ->
            val context = context ?: return@observe
            if (latestLog != null) {
                tvShiftStatus.text = latestLog.riskLevel
                when (latestLog.riskLevel.uppercase()) {
                    "SAFE" -> {
                        tvShiftStatus.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                        cardAlert.visibility = View.GONE
                    }
                    "CAUTION", "MODERATE" -> {
                        tvShiftStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                        cardAlert.visibility = View.VISIBLE
                        cardAlert.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_warning_bg))
                        tvAlertTitle.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                        tvAlertTitle.text = "CAUTION: EXPOSURE ACTION LEVEL REACHED"
                        tvAlertMessage.text = "${latestLog.actionRequired} (TWA: ${latestLog.twa8hrPpm} ppm)"
                    }
                    "HIGH" -> {
                        tvShiftStatus.setTextColor(ContextCompat.getColor(context, R.color.status_high))
                        cardAlert.visibility = View.VISIBLE
                        cardAlert.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_high_bg))
                        tvAlertTitle.setTextColor(ContextCompat.getColor(context, R.color.status_high))
                        tvAlertTitle.text = "HIGH HAZARD: APPROACHING PEL THRESHOLD"
                        tvAlertMessage.text = "${latestLog.actionRequired} (TWA: ${latestLog.twa8hrPpm} ppm)"
                    }
                    else -> {
                        tvShiftStatus.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                        cardAlert.visibility = View.VISIBLE
                        cardAlert.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_danger_bg))
                        tvAlertTitle.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                        tvAlertTitle.text = "CRITICAL ALARM: PEL EXCEEDED (> 10 ppm)"
                        tvAlertMessage.text = "IMMEDIATE EVACUATION REQUIRED! ${latestLog.actionRequired}"
                    }
                }
            } else {
                cardAlert.visibility = View.GONE
            }
        }

        // Observe Dashboard Metrics
        viewModel.metrics.observe(viewLifecycleOwner) { m ->
            if (m != null) {
                tvDose.text = String.format("%.2f", m.totalDosePpmHr)
                tvTwa.text = String.format("%.2f", m.avgTwaPpm)
                tvScans.text = m.totalScans.toString()
                tvCompliance.text = "${String.format("%.0f", m.complianceRatePct)}%"
            }
        }

        viewModel.syncMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        btnScan.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_scan)
        }

        btnRegister.setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.nav_register)
        }

        btnSync.setOnClickListener {
            Toast.makeText(context, "Initiating cloud synchronization...", Toast.LENGTH_SHORT).show()
            viewModel.triggerCloudSync()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDashboard()
    }
}
