package com.example.sih_26118_dosimeter_app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface DoseGuardApiService {

    @POST("api/dosimeter/sync")
    suspend fun syncShiftLogs(
        @Body request: SyncLogsRequest,
        @Header("Authorization") token: String? = null
    ): Response<SyncLogsResponse>

    @GET("api/dosimeter/logs")
    suspend fun getShiftLogs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("workerId") workerId: String? = null,
        @Query("riskLevel") riskLevel: String? = null,
        @Header("Authorization") token: String? = null
    ): Response<GetLogsResponse>

    @GET("api/dosimeter/dashboard")
    suspend fun getDashboardMetrics(
        @Header("Authorization") token: String? = null
    ): Response<DashboardMetricsResponse>

    @POST("api/auth/register")
    suspend fun registerWorker(
        @Body request: RegisterWorkerRequest
    ): Response<AuthResponse>
}
