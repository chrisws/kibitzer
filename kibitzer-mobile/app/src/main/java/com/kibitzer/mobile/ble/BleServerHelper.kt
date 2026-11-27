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
