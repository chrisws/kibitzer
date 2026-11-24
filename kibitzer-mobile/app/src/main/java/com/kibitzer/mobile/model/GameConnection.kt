package com.kibitzer.mobile.model

interface GameConnection {

    fun connect()
    fun disconnect()
    fun sendCommand(command: String)
    fun setOnStateUpdate(listener: (String) -> Unit)
    fun setOnChatUpdate(listener: (String) -> Unit)
    fun setOnHandshake(listener: (String) -> Unit)
    fun setOnConnectionState(listener: (Boolean) -> Unit)
    fun isConnected(): Boolean
    fun startScan()
    fun stopScan()
    fun setOnScanResult(listener: (String) -> Unit)
}