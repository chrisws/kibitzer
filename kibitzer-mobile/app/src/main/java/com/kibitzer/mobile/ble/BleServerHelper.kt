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
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.content.Context
import android.util.Log

object BleServerHelper {
  private const val TAG = "BleServerHelper"

  fun openGattServer(
    adapter: BluetoothAdapter,
    context: Context,
    callback: BluetoothGattServerCallback,
  ): BluetoothGattServer? =
    try {
      val method =
        BluetoothAdapter::class.java.getMethod(
          "openGattServer",
          Context::class.java,
          BluetoothGattServerCallback::class.java,
        )
      method.invoke(adapter, context, callback) as? BluetoothGattServer
    } catch (e: Exception) {
      Log.e(TAG, "Failed to open GATT server", e)
      null
    }

  fun setProperties(
    char: BluetoothGattCharacteristic,
    vararg properties: Int,
  ) {
    try {
      val method = BluetoothGattCharacteristic::class.java.getMethod("setProperties", IntArray::class.java)
      method.invoke(char, properties)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to set properties", e)
    }
  }
}
