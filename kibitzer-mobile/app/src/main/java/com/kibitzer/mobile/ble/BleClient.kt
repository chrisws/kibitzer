/*
 * This file is part of kibitzer
 *
 * Copyright (C) 2026 Chris Warren-Smith. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kibitzer.mobile.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log

class BleClient(
  private val context: Context,
) : BluetoothGattCallback() {
  companion object {
    private const val TAG = "BleClient"
  }

  private var adapter: BluetoothAdapter? = null
  private var gatt: BluetoothGatt? = null
  private var scanCallback: ScanCallback? = null
  private val chunker = Chunker()
  private val pendingChunks = mutableListOf<ByteArray>()
  private var sessionId: Int = -1
  private var connectedDevice: BluetoothDevice? = null
  private var isConnected = false

  var onStateUpdate: ((String) -> Unit)? = null
  var onChatUpdate: ((String) -> Unit)? = null
  var onHandshake: ((String) -> Unit)? = null
  var onConnectionState: ((Boolean) -> Unit)? = null
  var onScanResult: ((BluetoothDevice) -> Unit)? = null

  fun start() {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    adapter = manager?.adapter
  }

  fun stop() {
    stopScan()
    disconnect()
    adapter = null
  }

  // --- Scanning ---

  fun startScan() {
    val adapter = adapter ?: return
    val scanner = adapter.bluetoothLeScanner ?: return

    scanCallback =
      object : ScanCallback() {
        override fun onScanResult(
          callbackType: Int,
          result: ScanResult,
        ) {
          val name = result.scanRecord?.deviceName
          if (name == "Kibitzer") {
            Log.d(TAG, "Found Kibitzer host: ${result.device.address}")
            onScanResult?.invoke(result.device)
          }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
          for (result in results) {
            val name = result.scanRecord?.deviceName
            if (name == "Kibitzer") {
              onScanResult?.invoke(result.device)
            }
          }
        }

        override fun onScanFailed(errorCode: Int) {
          Log.e(TAG, "Scan failed: $errorCode")
        }
      }

    scanner.startScan(scanCallback)
    Log.d(TAG, "BLE scan started")
  }

  fun stopScan() {
    val adapter = adapter ?: return
    val scanner = adapter.bluetoothLeScanner ?: return
    scanCallback?.let { scanner.stopScan(it) }
    scanCallback = null
  }

  // --- Connection ---

  fun connect(device: BluetoothDevice) {
    stopScan()
    gatt = device.connectGatt(context, false, this)
    Log.d(TAG, "Connecting to ${device.address}")
  }

  fun disconnect() {
    gatt?.disconnect()
    gatt?.close()
    gatt = null
    connectedDevice = null
    pendingChunks.clear()
    onConnectionState?.invoke(false)
  }

  // --- BluetoothGattCallback ---

  override fun onConnectionStateChange(
    gatt: BluetoothGatt,
    status: Int,
    newState: Int,
  ) {
    when (newState) {
      BluetoothGatt.STATE_CONNECTED -> {
        Log.d(TAG, "Connected, discovering services...")
        isConnected = true
        gatt.discoverServices()
      }
      BluetoothGatt.STATE_DISCONNECTED -> {
        Log.d(TAG, "Disconnected")
        isConnected = false
        connectedDevice = null
        pendingChunks.clear()
        onConnectionState?.invoke(false)
      }
    }
  }

  override fun onServicesDiscovered(
    gatt: BluetoothGatt,
    status: Int,
  ) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.e(TAG, "Service discovery failed: $status")
      return
    }
    val service = gatt.getService(GattConstants.SERVICE_UUID)
    if (service == null) {
      Log.e(TAG, "Kibitzer service not found")
      return
    }

    val stateChar = service.getCharacteristic(GattConstants.CHAR_STATE)
    val chatChar = service.getCharacteristic(GattConstants.CHAR_CHAT)
    val handshakeChar = service.getCharacteristic(GattConstants.CHAR_HANDSHAKE)
    val commandChar = service.getCharacteristic(GattConstants.CHAR_COMMAND)

    stateChar?.let { gatt.setCharacteristicNotification(it, true) }
    chatChar?.let { gatt.setCharacteristicNotification(it, true) }
    handshakeChar?.let { gatt.setCharacteristicNotification(it, true) }

    connectedDevice = gatt.device
    pendingChunks.clear()
    onConnectionState?.invoke(true)
    Log.d(TAG, "Services discovered, notifications enabled")
  }

  override fun onCharacteristicChanged(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
  ) {
    val value = characteristic.value ?: return
    when (characteristic.uuid) {
      GattConstants.CHAR_STATE -> handleStateChunk(value)
      GattConstants.CHAR_CHAT -> handleChatChunk(value)
      GattConstants.CHAR_HANDSHAKE -> handleHandshakeChunk(value)
    }
  }

  override fun onCharacteristicRead(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    status: Int,
  ) {
    // Handled via notifications
  }

  // --- Chunk handling ---

  private fun handleStateChunk(chunk: ByteArray) {
    pendingChunks.add(chunk)
    val total = (chunk[3].toInt() and 0xFF)
    if (pendingChunks.size >= total) {
      try {
        val message = chunker.reassemble(pendingChunks)
        pendingChunks.clear()
        onStateUpdate?.invoke(message)
      } catch (e: Exception) {
        Log.e(TAG, "State chunk reassembly failed", e)
        pendingChunks.clear()
      }
    }
  }

  private fun handleChatChunk(chunk: ByteArray) {
    pendingChunks.add(chunk)
    val total = (chunk[3].toInt() and 0xFF)
    if (pendingChunks.size >= total) {
      try {
        val message = chunker.reassemble(pendingChunks)
        pendingChunks.clear()
        onChatUpdate?.invoke(message)
      } catch (e: Exception) {
        Log.e(TAG, "Chat chunk reassembly failed", e)
        pendingChunks.clear()
      }
    }
  }

  private fun handleHandshakeChunk(chunk: ByteArray) {
    pendingChunks.add(chunk)
    val total = (chunk[3].toInt() and 0xFF)
    if (pendingChunks.size >= total) {
      try {
        val message = chunker.reassemble(pendingChunks)
        pendingChunks.clear()
        onHandshake?.invoke(message)
      } catch (e: Exception) {
        Log.e(TAG, "Handshake chunk reassembly failed", e)
        pendingChunks.clear()
      }
    }
  }

  // --- Sending ---

  fun sendCommand(command: String) {
    val gatt = gatt ?: return
    val service = gatt.getService(GattConstants.SERVICE_UUID) ?: return
    val char = service.getCharacteristic(GattConstants.CHAR_COMMAND) ?: return

    val chunks = chunker.split(command)
    for (chunk in chunks) {
      char.value = chunk
      gatt.writeCharacteristic(char)
    }
    Log.d(TAG, "Sent command: $command (${chunks.size} chunk(s))")
  }

  fun setSessionId(id: Int) {
    sessionId = id
  }

  fun getSessionId(): Int = sessionId

  fun isConnected(): Boolean = isConnected
}
