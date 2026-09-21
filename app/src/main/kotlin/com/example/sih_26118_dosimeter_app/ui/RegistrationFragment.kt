package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.sih_26118_dosimeter_app.MainActivity
import com.example.sih_26118_dosimeter_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegistrationFragment : Fragment() {

    private lateinit var viewModel: RegistrationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[RegistrationViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_registration, container, false)

        val etWorkerId = view.findViewById<TextInputEditText>(R.id.et_reg_worker_id)
        val etName = view.findViewById<TextInputEditText>(R.id.et_reg_name)
        val etDept = view.findViewById<TextInputEditText>(R.id.et_reg_department)
        val etRole = view.findViewById<TextInputEditText>(R.id.et_reg_role)
        val etBadgeId = view.findViewById<TextInputEditText>(R.id.et_reg_badge_id)

        val btnDemoWorker1 = view.findViewById<MaterialButton>(R.id.btn_qr_demo_worker1)
        val btnDemoWorker2 = view.findViewById<MaterialButton>(R.id.btn_qr_demo_worker2)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_reg_submit)

        // Demo QR Presets
        btnDemoWorker1.setOnClickListener {
            etWorkerId.setText("W-1001")
            etName.setText("Rajesh Kumar")
            etDept.setText("Refinery Sweetening Unit")
            etRole.setText("Plant Operator")
            etBadgeId.setText("BAND-9988")
            Toast.makeText(context, "Simulated QR Scan: W-1001", Toast.LENGTH_SHORT).show()
        }

        btnDemoWorker2.setOnClickListener {
            etWorkerId.setText("W-1002")
            etName.setText("Priya Sharma")
            etDept.setText("Hydrocracker Complex")
            etRole.setText("Safety Specialist")
            etBadgeId.setText("BAND-5521")
            Toast.makeText(context, "Simulated QR Scan: W-1002", Toast.LENGTH_SHORT).show()
        }

        btnSubmit.setOnClickListener {
            val workerId = etWorkerId.text.toString()
            val name = etName.text.toString()
            val dept = etDept.text.toString()
            val role = etRole.text.toString()
            val badge = etBadgeId.text.toString()
            val email = "${workerId.lowercase()}@refinery.com"

            viewModel.registerWorkerAndBadge(
                workerId = workerId,
                name = name,
                department = dept,
                role = role,
                email = email,
                assignedBandId = badge
            )
        }

        viewModel.registrationStatus.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegistrationViewModel.RegistrationState.Loading -> {
                    btnSubmit.isEnabled = false
                    btnSubmit.text = "Registering with Cloud..."
                }
                is RegistrationViewModel.RegistrationState.Success -> {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Register & Sync with Cloud DB"
                    val msg = if (state.isCloudSynced) {
                        "Registered & Synced to Cloud DB: ${state.worker.name}"
                    } else {
                        "Saved to Offline Database: ${state.worker.name}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    viewModel.resetStatus()
                    // Return to dashboard
                    (activity as? MainActivity)?.switchTab(R.id.nav_home)
                }
                is RegistrationViewModel.RegistrationState.Error -> {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Register & Sync with Cloud DB"
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatus()
                }
                else -> {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Register & Sync with Cloud DB"
                }
            }
        }

        return view
    }
}
