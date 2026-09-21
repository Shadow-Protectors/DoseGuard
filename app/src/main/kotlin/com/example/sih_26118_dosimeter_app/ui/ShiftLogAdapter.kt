package com.example.sih_26118_dosimeter_app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.sih_26118_dosimeter_app.R
import com.example.sih_26118_dosimeter_app.database.ShiftLogEntity

class ShiftLogAdapter(
    private var items: List<ShiftLogEntity> = emptyList(),
    private val onItemClick: ((ShiftLogEntity) -> Unit)? = null
) : RecyclerView.Adapter<ShiftLogAdapter.LogViewHolder>() {

    fun submitList(newItems: List<ShiftLogEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shift_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWorkerName: TextView = itemView.findViewById(R.id.tv_item_worker_name)
        private val tvMeta: TextView = itemView.findViewById(R.id.tv_item_meta)
        private val chipRisk: TextView = itemView.findViewById(R.id.chip_risk)
        private val tvTwa: TextView = itemView.findViewById(R.id.tv_item_twa)
        private val tvDose: TextView = itemView.findViewById(R.id.tv_item_dose)
        private val tvDeltaE: TextView = itemView.findViewById(R.id.tv_item_delta_e)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_item_timestamp)
        private val tvSyncStatus: TextView = itemView.findViewById(R.id.tv_item_sync_status)

        fun bind(item: ShiftLogEntity, onItemClick: ((ShiftLogEntity) -> Unit)?) {
            tvWorkerName.text = "${item.workerName} (${item.workerId})"
            tvMeta.text = "Badge: ${item.bandId} • ${item.department}"
            tvTwa.text = "${item.twa8hrPpm} ppm"
            tvDose.text = "${item.estimatedDosePpmHr} ppm·hr"
            tvDeltaE.text = "ΔE ${String.format("%.1f", item.rawDeltaE)}"
            tvTimestamp.text = "${item.shiftDate} • ${item.scanTime}"

            // Risk status color coding
            chipRisk.text = item.riskLevel
            val context = itemView.context
            when (item.riskLevel.uppercase()) {
                "SAFE" -> {
                    chipRisk.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                }
                "CAUTION", "MODERATE" -> {
                    chipRisk.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                }
                "HIGH" -> {
                    chipRisk.setTextColor(ContextCompat.getColor(context, R.color.status_high))
                }
                else -> {
                    chipRisk.setTextColor(ContextCompat.getColor(context, R.color.status_danger))
                }
            }

            // Sync status
            if (item.syncStatus == "SYNCED") {
                tvSyncStatus.text = "• CLOUD SYNCED"
                tvSyncStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_blue))
            } else {
                tvSyncStatus.text = "• OFFLINE PENDING"
                tvSyncStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
