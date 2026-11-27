package com.kibitzer.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.kibitzer.mobile.R
import com.kibitzer.mobile.ble.BleClient
import com.kibitzer.mobile.ble.BleHost
import com.kibitzer.mobile.ble.BleService
import com.kibitzer.mobile.game.Controller
import com.kibitzer.mobile.model.GameConnection

class GameService :
  Service(),
  GameConnection {
  companion object {
    private const val TAG = "GameService"
    private const val CHANNEL_ID = "kibitzer_service"
    private const val NOTIFICATION_ID = 1
  }

  private var bleClient: BleClient? = null
  private var bleHost: BleHost? = null
  private var bleService: BleService? = null
  private var controller: Controller? = null
  private var isHostMode: Boolean = false

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification("Kibitzer running"))

    bleService = BleService(this)
    controller = Controller()
    bleClient = BleClient(this)
    bleHost = BleHost(this, controller!!)

    Log.d(TAG, "GameService created")
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    intent?.let {
      when (it.action) {
        "com.kibitzer.mobile.START_HOST" -> startHost()
        "com.kibitzer.mobile.START_CLIENT" -> startClient()
        "com.kibitzer.mobile.STOP" -> stopService()
      }
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder? = null

  override fun onDestroy() {
    bleClient?.stop()
    bleHost?.stop()
    super.onDestroy()
    Log.d(TAG, "GameService destroyed")
  }

  // --- GameConnection implementation ---

  override fun connect() {
    bleClient?.startScan()
  }

  override fun disconnect() {
    bleClient?.disconnect()
  }

  override fun sendCommand(command: String) {
    bleClient?.sendCommand(command)
  }

  override fun setOnStateUpdate(listener: (String) -> Unit) {
    bleClient?.onStateUpdate = listener
  }

  override fun setOnChatUpdate(listener: (String) -> Unit) {
    bleClient?.onChatUpdate = listener
  }

  override fun setOnHandshake(listener: (String) -> Unit) {
    bleClient?.onHandshake = listener
  }

  override fun setOnConnectionState(listener: (Boolean) -> Unit) {
    bleClient?.onConnectionState = listener
  }

  override fun isConnected(): Boolean = bleClient?.isConnected() == true

  override fun startScan() {
    bleClient?.startScan()
  }

  override fun stopScan() {
    bleClient?.stopScan()
  }

  override fun setOnScanResult(listener: (String) -> Unit) {
    bleClient?.onScanResult = { device ->
      listener(device.address)
      bleClient?.connect(device)
    }
  }

  // --- Internal ---

  private fun startHost() {
    isHostMode = true
    bleHost?.start()
    bleHost?.startAdvertising()
    updateNotification("Kibitzer host — waiting for players")
    Log.d(TAG, "Host mode started")
  }

  private fun startClient() {
    isHostMode = false
    bleClient?.start()
    Log.d(TAG, "Client mode started")
  }

  private fun stopService() {
    bleClient?.stop()
    bleHost?.stop()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "Kibitzer Service",
          NotificationManager.IMPORTANCE_LOW,
        )
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(text: String): Notification {
    val builder =
      Notification
        .Builder(this)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Kibitzer")
        .setContentText(text)
        .setOngoing(true)
        .setChannelId(CHANNEL_ID)
    return builder.build()
  }

  private fun updateNotification(text: String) {
    val manager = getSystemService(NotificationManager::class.java)
    manager?.notify(NOTIFICATION_ID, buildNotification(text))
  }
}
