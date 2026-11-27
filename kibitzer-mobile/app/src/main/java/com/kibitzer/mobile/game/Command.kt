package com.kibitzer.mobile.game

enum class Command(
  val prefix: String,
) {
  CHAT("chat:"),
  DEAL("deal:"),
  EXCHANGE("exch:"),
  INIT("init:"),
  JOIN("join:"),
  NICKNAME("nicn:"),
  PICKUP("picu:"),
  PUTDOWN("putd:"),
  ROOM("room:"),
  SHUFFLE("shuf:"),
  SKIP("skip:"),
  GAME("game:"),
  BID("bid:"),
  CHEAT("cheat:"),
  ASK("ask:"),
  ;

  companion object {
    fun fromPrefix(prefix: String): Command? = entries.find { it.prefix == prefix }
  }
}
