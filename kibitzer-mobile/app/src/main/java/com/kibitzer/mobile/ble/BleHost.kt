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
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.kibitzer.mobile.game.Controller
import com.kibitzer.mobile.game.Message

class BleHost(
  private val context: Context,
  private val controller: Controller,
) : BluetoothGattServerCallback() {
  companion object {
    private const val TAG = "BleHost"
    private const val ADVERT_NAME = "Kibitzer"
  }

  private var gattServer: BluetoothGattServer? = null
  private var adapter: BluetoothAdapter? = null
  private val chunker = Chunker()
  private val connectedDevices = mutableMapOf<BluetoothDevice, Int>()
  private val pendingChunks = mutableMapOf<BluetoothDevice, MutableList<ByteArray>>()
  private var nextSessionId = 1
  private var isAdvertising = false

  val onMessage: ((String) -> Unit)? = null

  fun start() {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    adapter = manager?.adapter
    adapter?.let {
      gattServer = BleServerHelper.openGattServer(it, context, this)
      Log.d(TAG, "GATT server started")
    }
  }

  fun stop() {
    gattServer?.close()
    gattServer = null
    isAdvertising = false
    Log.d(TAG, "GATT server stopped")
  }

  fun startAdvertising() {
    val server = gattServer ?: return
    val service = server.getService(GattConstants.SERVICE_UUID) ?: return
    val commandChar = service.getCharacteristic(GattConstants.CHAR_COMMAND)
    val stateChar = service.getCharacteristic(GattConstants.CHAR_STATE)
    val chatChar = service.getCharacteristic(GattConstants.CHAR_CHAT)
    val handshakeChar = service.getCharacteristic(GattConstants.CHAR_HANDSHAKE)
    val playersChar = service.getCharacteristic(GattConstants.CHAR_PLAYERS)

    if (commandChar == null ||
      stateChar == null ||
      chatChar == null ||
      handshakeChar == null ||
      playersChar == null
    ) {
      Log.e(TAG, "Missing GATT characteristics")
      return
    }

    BleServerHelper.setProperties(commandChar, BluetoothGattCharacteristic.PROPERTY_WRITE)
    BleServerHelper.setProperties(
      stateChar,
      BluetoothGattCharacteristic.PROPERTY_READ,
      BluetoothGattCharacteristic.PROPERTY_NOTIFY,
    )
    BleServerHelper.setProperties(
      chatChar,
      BluetoothGattCharacteristic.PROPERTY_READ,
      BluetoothGattCharacteristic.PROPERTY_NOTIFY,
    )
    BleServerHelper.setProperties(
      handshakeChar,
      BluetoothGattCharacteristic.PROPERTY_READ,
      BluetoothGattCharacteristic.PROPERTY_NOTIFY,
    )
    BleServerHelper.setProperties(
      playersChar,
      BluetoothGattCharacteristic.PROPERTY_READ,
      BluetoothGattCharacteristic.PROPERTY_NOTIFY,
    )

    isAdvertising = true
    Log.d(TAG, "Advertising as $ADVERT_NAME")
  }

  // --- BluetoothGattServerCallback ---

  override fun onConnectionStateChange(
    device: BluetoothDevice,
    status: Int,
    newState: Int,
  ) {
    when (newState) {
      BluetoothProfile.STATE_CONNECTED -> {
        Log.d(TAG, "Device connected: ${device.address}")
        val sessionId = controller.createSession()
        connectedDevices[device] = sessionId
        pendingChunks[device] = mutableListOf()
        sendHandshake(device, sessionId)
      }
      BluetoothProfile.STATE_DISCONNECTED -> {
        Log.d(TAG, "Device disconnected: ${device.address}")
        val sessionId = connectedDevices.remove(device)
        pendingChunks.remove(device)
        if (sessionId != null) {
          controller.destroySession(sessionId)
        }
      }
    }
  }

  override fun onCharacteristicReadRequest(
    device: BluetoothDevice,
    requestId: Int,
    offset: Int,
    characteristic: BluetoothGattCharacteristic,
  ) {
    val server = gattServer ?: return
    val sessionId = connectedDevices[device] ?: return
    val data =
      when (characteristic.uuid) {
        GattConstants.CHAR_STATE -> getStateJson(sessionId)
        GattConstants.CHAR_PLAYERS -> getPlayersJson()
        GattConstants.CHAR_HANDSHAKE -> getHandshakeJson(sessionId)
        else -> ""
      }
    val dataBytes = data.toByteArray()
    server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, dataBytes.size, dataBytes)
  }

  override fun onCharacteristicWriteRequest(
    device: BluetoothDevice,
    requestId: Int,
    characteristic: BluetoothGattCharacteristic,
    permitted: Boolean,
    prepared: Boolean,
    offset: Int,
    value: ByteArray,
  ) {
    val server = gattServer ?: return
    val sessionId = connectedDevices[device] ?: return

    if (characteristic.uuid == GattConstants.CHAR_COMMAND) {
      handleCommandChunk(device, value, sessionId)
    }
    server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
  }

  override fun onExecuteWrite(
    device: BluetoothDevice,
    requestId: Int,
    prepared: Boolean,
  ) {
    // No-op for now
  }

  override fun onNotificationSent(
    device: BluetoothDevice,
    status: Int,
  ) {
    Log.d(TAG, "Notification sent to ${device.address}, status=$status")
  }

  // --- Command handling ---

  private fun handleCommandChunk(
    device: BluetoothDevice,
    chunk: ByteArray,
    sessionId: Int,
  ) {
    val buffer = pendingChunks.getOrPut(device) { mutableListOf() }
    buffer.add(chunk)

    val total = (chunk[3].toInt() and 0xFF)
    if (buffer.size >= total) {
      try {
        val message = chunker.reassemble(buffer)
        buffer.clear()
        processCommand(message, sessionId)
      } catch (e: Exception) {
        Log.e(TAG, "Chunk reassembly failed", e)
        buffer.clear()
      }
    }
  }

  private fun processCommand(
    raw: String,
    sessionId: Int,
  ) {
    Log.d(TAG, "Command from $sessionId: $raw")
    val response = controller.handle(raw, sessionId)
    broadcastState(response)
  }

  // --- Broadcasting ---

  fun broadcastState(message: Message) {
    val json = message.toJson()
    val chunks = chunker.split(json)
    for ((device, _) in connectedDevices) {
      for (chunk in chunks) {
        gattServer?.let { server ->
          val service = server.getService(GattConstants.SERVICE_UUID) ?: return
          val char = service.getCharacteristic(GattConstants.CHAR_STATE) ?: return
          server.notifyCharacteristicChanged(device, char, false, chunk)
        }
      }
    }
  }

  fun broadcastChat(message: Message) {
    val json = message.toJson()
    val chunks = chunker.split(json)
    for ((device, _) in connectedDevices) {
      for (chunk in chunks) {
        gattServer?.let { server ->
          val service = server.getService(GattConstants.SERVICE_UUID) ?: return
          val char = service.getCharacteristic(GattConstants.CHAR_CHAT) ?: return
          server.notifyCharacteristicChanged(device, char, false, chunk)
        }
      }
    }
  }

  // --- JSON helpers ---

  private fun getStateJson(sessionId: Int): String {
    val player = controller.findSession(sessionId)
    if (player == null) return "{}"
    val room = controller.rooms[0]
    val handJson = player.hand.toJson()
    val discardJson = room.deck.getDiscardJson()
    return """{"hand":$handJson,"pile":$discardJson,"turn":${room.turn},"game":"${room.rules.name()}"}"""
  }

  private fun getPlayersJson(): String {
    val room = controller.rooms[0]
    val parts =
      room.slots.map { sid ->
        if (sid == -1) {
          """{"sessionId":-1,"active":false,"nic":"empty"}"""
        } else {
          controller.findSession(sid)?.toJson()
            ?: """{"sessionId":-1,"active":false,"nic":"empty"}"""
        }
      }
    return parts.joinToString(", ", prefix = "[", postfix = "]")
  }

  private fun getHandshakeJson(sessionId: Int): String =
    """{"sessionId":$sessionId,"game":"${controller.getCurrentGame().name}","maxPlayers":${GattConstants.MAX_PLAYERS}}"""

  private fun sendHandshake(
    device: BluetoothDevice,
    sessionId: Int,
  ) {
    val json = getHandshakeJson(sessionId)
    val chunks = chunker.split(json)
    for (chunk in chunks) {
      gattServer?.let { server ->
        val service = server.getService(GattConstants.SERVICE_UUID) ?: return
        val char = service.getCharacteristic(GattConstants.CHAR_HANDSHAKE) ?: return
        server.notifyCharacteristicChanged(device, char, false, chunk)
      }
    }
  }

  fun getConnectedCount(): Int = connectedDevices.size

  fun isRunning(): Boolean = gattServer != null && isAdvertising
}
