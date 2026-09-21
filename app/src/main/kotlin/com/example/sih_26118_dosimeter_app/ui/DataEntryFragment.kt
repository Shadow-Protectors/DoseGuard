package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sih_26118_dosimeter_app.R
import com.example.sih_26118_dosimeter_app.cv.KineticDoseSolver
import com.example.sih_26118_dosimeter_app.database.ShiftLogEntity
import com.example.sih_26118_dosimeter_app.repository.DoseGuardRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DataEntryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry, container, false)

        val etWorkerId = view.findViewById<TextInputEditText>(R.id.et_worker_id)
        val etWorkerName = view.findViewById<TextInputEditText>(R.id.et_worker_name)
        val etBadgeId = view.findViewById<TextInputEditText>(R.id.et_badge_id)
        val etDepartment = view.findViewById<TextInputEditText>(R.id.et_department)
        val etShiftHours = view.findViewById<TextInputEditText>(R.id.et_shift_hours)
        val etDeltaE = view.findViewById<TextInputEditText>(R.id.et_delta_e)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_submit_log)
        val cardResult = view.findViewById<CardView>(R.id.card_entry_result)
        val tvResultDetail = view.findViewById<TextView>(R.id.tv_result_detail)

        val repository = DoseGuardRepository(requireContext())

        btnSubmit?.setOnClickListener {
            val deltaEVal = etDeltaE?.text?.toString()?.toDoubleOrNull() ?: 10.0
            val hoursVal = etShiftHours?.text?.toString()?.toDoubleOrNull() ?: 8.0
            val workerName = etWorkerName?.text?.toString()?.ifEmpty { "Worker" } ?: "Worker"
            val workerId = etWorkerId?.text?.toString()?.ifEmpty { "W-4029" } ?: "W-4029"
            val badgeId = etBadgeId?.text?.toString()?.ifEmpty { "BDG-9901" } ?: "BDG-9901"
            val department = etDepartment?.text?.toString()?.ifEmpty { "Refining" } ?: "Refining"

            val calcResult = KineticDoseSolver.calculateDose(deltaE = deltaEVal, shiftHours = hoursVal)

            cardResult?.visibility = View.VISIBLE
            tvResultDetail?.text = "Worker: $workerName ($workerId)\nTWA: ${calcResult.twa8hrPpm} ppm | Risk: ${calcResult.riskLevel}\nDose: ${calcResult.dosePpmHr} ppm·hr\nAction: ${calcResult.actionRequired}"

            val now = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val logId = "LOG-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"

            val md = MessageDigest.getInstance("SHA-256")
            val hashInput = "$logId-$workerId-$badgeId-${now.time}"
            val imageHash = md.digest(hashInput.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }

            val log = ShiftLogEntity(
                logId = logId,
                workerId = workerId,
                workerName = workerName,
                department = department,
                bandId = badgeId,
                shiftDate = dateFormat.format(now),
                scanTime = timeFormat.format(now),
                durationHours = hoursVal,
                expiryStatus = "VALID",
                rawDeltaE = deltaEVal,
                estimatedDosePpmHr = calcResult.dosePpmHr,
                uncertaintyPpmHr = calcResult.uncertaintyPpmHr,
                twa8hrPpm = calcResult.twa8hrPpm,
                riskLevel = calcResult.riskLevel,
                actionRequired = calcResult.actionRequired,
                imageHash = imageHash,
                syncStatus = "PENDING"
            )

            lifecycleScope.launch {
                val synced = repository.saveScan(log)
                val statusMsg = if (synced) "Saved and Synced to Cloud DB!" else "Saved to Local Room DB"
                Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
