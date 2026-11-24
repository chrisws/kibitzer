package com.kibitzer.mobile.game

interface GameHandler {
    fun handlePutdown(controller: Controller, player: Player, hand: Hand): Message
}