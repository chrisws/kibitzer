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
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class BleService(
  private val context: Context,
) {
  companion object {
    private const val TAG = "BleService"
  }

  private val adapter: BluetoothAdapter?
    get() {
      val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      return manager?.adapter
    }

  fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

  fun hasBluetooth(): Boolean = adapter != null

  fun hasPermission(): Boolean =
    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
      PackageManager.PERMISSION_GRANTED &&
      context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) ==
      PackageManager.PERMISSION_GRANTED

  fun negotiateMtu(gatt: BluetoothGatt): Boolean {
    val result = gatt.requestMtu(GattConstants.TARGET_MTU)
    Log.d(TAG, "MTU negotiation requested: $result")
    return result
  }

  fun getDeviceAddress(device: BluetoothDevice): String = device.address

  fun getDeviceName(device: BluetoothDevice): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      device.name ?: "Unknown"
    } else {
      @Suppress("DEPRECATION")
      device.name ?: "Unknown"
    }

  fun getAdapterState(): Int = adapter?.state ?: BluetoothAdapter.STATE_OFF

  fun isAdapterOn(): Boolean = adapter?.state == BluetoothAdapter.STATE_ON
}
