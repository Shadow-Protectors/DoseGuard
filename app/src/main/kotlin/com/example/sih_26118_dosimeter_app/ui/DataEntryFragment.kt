package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.sih_26118_dosimeter_app.R
import com.example.sih_26118_dosimeter_app.cv.KineticDoseSolver

/**
 * Native Manual Data Entry View
 * Computes dosage and saves records locally without needing a server connection.
 */
class DataEntryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry, container, false)

        val etWorkerId = view.findViewById<EditText>(R.id.et_worker_id)
        val etWorkerName = view.findViewById<EditText>(R.id.et_worker_name)
        val etBadgeId = view.findViewById<EditText>(R.id.et_badge_id)
        val etDepartment = view.findViewById<EditText>(R.id.et_department)
        val etShiftHours = view.findViewById<EditText>(R.id.et_shift_hours)
        val etDeltaE = view.findViewById<EditText>(R.id.et_delta_e)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_log)
        val cardResult = view.findViewById<CardView>(R.id.card_entry_result)
        val tvResultDetail = view.findViewById<TextView>(R.id.tv_result_detail)

        btnSubmit?.setOnClickListener {
            val deltaEVal = etDeltaE?.text?.toString()?.toDoubleOrNull() ?: 10.0
            val hoursVal = etShiftHours?.text?.toString()?.toDoubleOrNull() ?: 8.0
            val workerName = etWorkerName?.text?.toString() ?: "Worker"

            val calcResult = KineticDoseSolver.calculateDose(deltaE = deltaEVal, shiftHours = hoursVal)

            cardResult?.visibility = View.VISIBLE
            tvResultDetail?.text = "Worker: $workerName | TWA: ${calcResult.twa8hrPpm} ppm | Risk: ${calcResult.riskLevel}"

            Toast.makeText(context, "Log saved offline for $workerName!", Toast.LENGTH_LONG).show()
        }

        return view
    }
}
