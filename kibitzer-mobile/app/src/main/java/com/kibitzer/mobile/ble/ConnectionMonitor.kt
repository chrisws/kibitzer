package com.kibitzer.mobile.ble

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ConnectionMonitor(
    private val scope: CoroutineScope,
    private val onHealthy: () -> Unit = {},
    private val onUnhealthy: () -> Unit = {},
    private val onReconnect: () -> Unit = {}
) {

    companion object {
        private const val TAG = "ConnectionMonitor"
    }

    private var monitorJob: Job? = null
    private var lastPongTime: Long = System.currentTimeMillis()
    private var reconnectAttempts: Int = 0
    private var isMonitoring: Boolean = false

    fun start() {
        if (isMonitoring) return
        isMonitoring = true
        reconnectAttempts = 0
        lastPongTime = System.currentTimeMillis()

        monitorJob = scope.launch {
            while (isActive) {
                delay(GattConstants.HEARTBEAT_INTERVAL_MS)
                checkHealth()
            }
        }
        Log.d(TAG, "Connection monitor started")
    }

    fun stop() {
        isMonitoring = false
        monitorJob?.cancel()
        monitorJob = null
        Log.d(TAG, "Connection monitor stopped")
    }

    fun recordPong() {
        lastPongTime = System.currentTimeMillis()
        reconnectAttempts = 0
    }

    fun isHealthy(): Boolean {
        val elapsed = System.currentTimeMillis() - lastPongTime
        return elapsed < GattConstants.PONG_TIMEOUT_MS
    }

    private fun checkHealth() {
        if (!isHealthy()) {
            Log.w(TAG, "Connection unhealthy, attempt $reconnectAttempts")
            onUnhealthy()
            if (reconnectAttempts < GattConstants.RECONNECT_MAX_ATTEMPTS) {
                reconnectAttempts++
                val delayMs = GattConstants.RECONNECT_BASE_MS * reconnectAttempts
                scope.launch {
                    delay(delayMs)
                    onReconnect()
                }
            } else {
                Log.e(TAG, "Max reconnect attempts reached")
            }
        } else {
            onHealthy()
        }
    }

    fun reset() {
        reconnectAttempts = 0
        lastPongTime = System.currentTimeMillis()
    }
}