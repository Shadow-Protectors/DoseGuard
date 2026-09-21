package com.example.sih_26118_dosimeter_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sih_26118_dosimeter_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HistoryFragment : Fragment() {

    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: ShiftLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_shift_logs)
        val emptyLayout = view.findViewById<LinearLayout>(R.id.layout_empty_state)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_logs)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filters)
        val btnRefresh = view.findViewById<MaterialButton>(R.id.btn_refresh_logs)

        adapter = ShiftLogAdapter { clickedLog ->
            Toast.makeText(
                context,
                "Log: ${clickedLog.logId} • Dose: ${clickedLog.estimatedDosePpmHr} ppm·hr • Status: ${clickedLog.syncStatus}",
                Toast.LENGTH_SHORT
            ).show()
        }

        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        // Observe Logs
        viewModel.logs.observe(viewLifecycleOwner) { logList ->
            adapter.submitList(logList)
            if (logList.isNullOrEmpty()) {
                emptyLayout.visibility = View.VISIBLE
                recycler.visibility = View.GONE
            } else {
                emptyLayout.visibility = View.GONE
                recycler.visibility = View.VISIBLE
            }
        }

        // Swipe to Refresh
        swipeRefresh.setColorSchemeResources(R.color.primary_navy, R.color.primary_blue)
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshFromCloud()
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.syncCount.observe(viewLifecycleOwner) { pushed ->
            if (pushed != null && pushed > 0) {
                Toast.makeText(context, "Pushed $pushed pending offline logs to Cloud Database", Toast.LENGTH_SHORT).show()
            }
        }

        btnRefresh.setOnClickListener {
            viewModel.refreshFromCloud()
            Toast.makeText(context, "Synchronizing with Online Cloud Database...", Toast.LENGTH_SHORT).show()
        }

        // Filter Chips
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chip_filter_all -> viewModel.setFilter("ALL")
                R.id.chip_filter_safe -> viewModel.setFilter("SAFE")
                R.id.chip_filter_caution -> viewModel.setFilter("MODERATE")
                R.id.chip_filter_high -> viewModel.setFilter("HIGH")
                R.id.chip_filter_critical -> viewModel.setFilter("CRITICAL")
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFromCloud()
    }
}
