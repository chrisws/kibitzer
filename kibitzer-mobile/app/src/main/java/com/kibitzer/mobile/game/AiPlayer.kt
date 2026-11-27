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

package com.kibitzer.mobile.game

import kotlin.random.Random

class AiPlayer(
  private val seed: Long = 0L,
  private val difficulty: Difficulty = Difficulty.MEDIUM,
) {
  enum class Difficulty { EASY, MEDIUM, HARD }

  enum class Personality(
    val description: String,
  ) {
    AGGRESSIVE("plays boldly, takes risks"),
    CAUTIOUS("plays safe, avoids risks"),
    CHATTY("talks a lot, gives hints"),
    QUIET("barely speaks, focused"),
    COMPETITIVE("wants to win at all costs"),
    RELAXED("takes it easy, enjoys the game"),
  }

  val name: String = NAMES.random()
  val personality: Personality = Personality.entries.random()

  private val rng = Random(seed)

  companion object {
    private val NAMES =
      listOf(
        "Ace",
        "Blaze",
        "Cleo",
        "Duke",
        "Echo",
        "Finn",
        "Gwen",
        "Hank",
        "Iris",
        "Jax",
        "Kira",
        "Luna",
        "Milo",
        "Nina",
        "Oscar",
        "Pip",
        "Quinn",
        "Rex",
        "Sage",
        "Tess",
        "Umbra",
        "Vex",
        "Wren",
        "Xena",
        "Yuri",
        "Zara",
      )
  }

  fun decide(
    controller: Controller,
    sessionId: Int,
  ): String {
    val player =
      controller.findSession(sessionId)
        ?: return "skip:"
    val room = controller.rooms[player.room]
    val rules = room.rules

    return when (rules) {
      is WarlordsRules -> warlordsMove(controller, player, room)
      is FreeRules -> freeMove(controller, player, room)
      is CrazyEightsRules -> crazyEightsMove(controller, player, room)
      is CheatRules -> cheatMove(controller, player, room)
      is OldMaidRules -> oldMaidMove(controller, player, room)
      is GoFishRules -> goFishMove(controller, player, room)
      else -> "skip:"
    }
  }

  private fun warlordsMove(
    controller: Controller,
    player: Player,
    room: Room,
  ): String {
    val hand = player.hand
    val deck = room.deck

    val clearPlay = findClearPlay(hand)
    if (clearPlay != null) {
      return "putd:${clearPlay.toJson()}"
    }

    val last = deck.lastPlay
    val lastRank = last?.let { rank32(it) }
    val neededSize = if (deck.discardSize == 0) 1 else deck.lastSize

    val byRank = hand.toList().groupBy { rank32(it) }

    val validPlays =
      byRank
        .filter { (rank, cards) ->
          cards.size >= neededSize && isValidWarlordsRank(rank, lastRank)
        }.map { (rank, cards) ->
          Hand().apply { cards.take(neededSize).forEach { add(it) } }
        }

    if (validPlays.isNotEmpty()) {
      val play = validPlays.minBy { rank32(it.toList().first()) }
      return "putd:${play.toJson()}"
    }

    return "picu:1"
  }

  private fun isValidWarlordsRank(
    rank: Int,
    lastRank: Int?,
  ): Boolean {
    if (rank == rank32(Card.TWO_CLUBS)) return true
    if (lastRank == null) return true
    if (lastRank == rank32(Card.TWO_CLUBS)) return true
    return rank > lastRank
  }

  private fun findClearPlay(hand: Hand): Hand? {
    val byRank = hand.toList().groupBy { rank32(it) }
    for ((rank, cards) in byRank) {
      if (rank == rank32(Card.TWO_CLUBS) && cards.size >= 1) {
        return Hand().apply { add(cards[0]) }
      }
    }
    return null
  }

  private fun freeMove(
    controller: Controller,
    player: Player,
    room: Room,
  ): String {
    val hand = player.hand
    val deck = room.deck

    if (hand.size == 0) return "skip:"

    val last = deck.lastPlay
    if (last == null) {
      val lowest = hand.toList().minBy { rank2A(it) }
      return "putd:[\"${lowest.code}\"]"
    }

    val validCards = hand.toList().filter { rank2A(it) > rank2A(last) }
    if (validCards.isNotEmpty()) {
      val card = validCards.minBy { rank2A(it) }
      return "putd:[\"${card.code}\"]"
    }

    return "picu:1"
  }

  private fun crazyEightsMove(
    controller: Controller,
    player: Player,
    room: Room,
  ): String {
    val hand = player.hand
    val deck = room.deck

    val eights = hand.toList().filter { it.name[0] == '8' }
    val last = deck.lastPlay

    if (last == null) {
      if (eights.isNotEmpty() && difficulty == Difficulty.HARD) {
        return "putd:[\"${eights[0].name}\"]"
      }
      val lowest = hand.toList().minBy { rank2A(it) }
      return "putd:[\"${lowest.name}\"]"
    }

    val matching = hand.toList().filter { rank2A(it) == rank2A(last) }
    if (matching.isNotEmpty()) {
      val card = matching.minBy { it.ordinal }
      return "putd:[\"${card.code}\"]"
    }

    if (eights.isNotEmpty()) {
      return "putd:[\"${eights[0].name}\"]"
    }

    return "picu:1"
  }

  private fun cheatMove(
    controller: Controller,
    player: Player,
    room: Room,
  ): String {
    val hand = player.hand
    val cheatState = room.cheatState

    when (cheatState.phase) {
      CheatState.Phase.BIDDING -> {
        val matching = hand.toList().count { rank2A(it) == cheatState.currentRank }
        val bid =
          if (difficulty == Difficulty.EASY && rng.nextBoolean()) {
            matching + 1
          } else {
            matching.coerceAtMost(hand.size)
          }
        return "bid:$bid"
      }
      CheatState.Phase.PLAYING -> {
        val toPlay =
          hand
            .toList()
            .filter { rank2A(it) == cheatState.currentRank }
            .take(cheatState.bid)
        val final =
          if (toPlay.size >= cheatState.bid) {
            toPlay
          } else {
            toPlay +
              hand
                .toList()
                .filter { rank2A(it) != cheatState.currentRank }
                .take(cheatState.bid - toPlay.size)
          }
        if (final.isEmpty()) return "skip:"
        val h = Hand().apply { final.forEach { add(it) } }
        return "putd:${h.toJson()}"
      }
      CheatState.Phase.RESOLVING -> "skip:"
    }
    return "skip:"
  }

  private fun oldMaidMove(
    controller: Controller,
    player: Player,
    room: Room,
  ): String = "picu:1"

  private fun goFishMove(
    controller: Controller,
    player: Player,
    room: Room,
  ): String {
    val hand = player.hand
    val room2 = controller.rooms[player.room]

    val myRanks = hand.toList().map { rank2A(it) }.toSet()
    if (myRanks.isEmpty()) return "picu:1"

    val targetRank = myRanks.toList().randomElement(rng)

    val opponents =
      room2.slots
        .mapIndexed { i, sid -> i to sid }
        .filter { (_, sid) -> sid != -1 && sid != player.sessionId }
        .map { (i, _) -> i }
        .shuffled(rng)

    if (opponents.isEmpty()) return "picu:1"

    val targetSlot = opponents.first()
    val rankChar = rankToChar(targetRank)
    return "ask:$targetSlot:$rankChar"
  }
}
