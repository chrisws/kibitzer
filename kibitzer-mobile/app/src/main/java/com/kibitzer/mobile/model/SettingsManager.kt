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

package com.kibitzer.mobile.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SettingsManager {
  private const val PREFS_NAME = "kibitzer_settings"
  private const val KEY_COMPACT_CARDS = "compact_cards"
  private const val KEY_NICKNAME = "nickname"

  private var appContext: Context? = null

  var compactCards: Boolean by mutableStateOf(true)
    private set

  var nickname: String by mutableStateOf("")
    private set

  fun init(context: Context) {
    appContext = context.applicationContext
    val prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    compactCards = prefs.getBoolean(KEY_COMPACT_CARDS, true)
    nickname = prefs.getString(KEY_NICKNAME, "") ?: ""
    android.util.Log.d("SettingsMgr", "init: nickname from prefs='$nickname'")
    if (nickname.isEmpty()) {
      val deviceName = android.provider.Settings.Global.getString(
        context.contentResolver,
        "device_name",
      )
      nickname = deviceName ?: android.os.Build.MODEL
      android.util.Log.d("SettingsMgr", "init: primed nickname='$nickname' (deviceName=$deviceName, MODEL=${android.os.Build.MODEL})")
    }
  }

  fun updateCompactCards(value: Boolean) {
    compactCards = value
    appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      ?.edit()
      ?.putBoolean(KEY_COMPACT_CARDS, value)
      ?.apply()
  }

  fun updateNickname(value: String) {
    nickname = value
    appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      ?.edit()
      ?.putString(KEY_NICKNAME, value)
      ?.apply()
  }
}