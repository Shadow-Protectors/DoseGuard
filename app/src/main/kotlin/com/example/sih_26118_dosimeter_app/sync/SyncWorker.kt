package com.example.sih_26118_dosimeter_app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sih_26118_dosimeter_app.repository.DoseGuardRepository

/**
 * Android WorkManager Background Synchronization Engine
 *
 * Listens for network connectivity and synchronizes pending local Room DB shift logs
 * to the central Node.js MongoDB backend using exponential backoff retry policies.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = DoseGuardRepository(applicationContext)
            val count = repository.syncPendingLogsToCloud()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
