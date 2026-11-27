package com.kibitzer.mobile.game

enum class PlayerState { LURK, JOINED, DEALT }

class Player(
  val sessionId: Int,
) {
  val hand = Hand()
  var nickname: String = "#$sessionId"
    set(value) {
      field = value.replace("\"", "")
    }
  var slot: Int = -1
  var points: Int = 0
  var room: Int = 0
  var state: PlayerState = PlayerState.LURK
  var isAi: Boolean = false

  fun name(): String = "($nickname)"

  fun toJson(): String {
    val active = state != PlayerState.LURK && slot != -1
    return """{"sessionId":$sessionId,"active":$active,"nic":"$nickname","ai":$isAi}"""
  }
}
