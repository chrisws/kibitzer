package com.kibitzer.mobile.game

import kotlin.random.Random

class AiPlayer(
    private val seed: Long = 0L,
    private val difficulty: Difficulty = Difficulty.MEDIUM
) {
    enum class Difficulty { EASY, MEDIUM, HARD }

    private val rng = Random(seed)

    fun decide(controller: Controller, sessionId: Int): String {
        val player = controller.findSession(sessionId)
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

    private fun warlordsMove(controller: Controller, player: Player, room: Room): String {
        val hand = player.hand
        val deck = room.deck

        val clearPlay = findClearPlay(hand)
        if (clearPlay != null) {
            return "putd:${clearPlay.toJson()}"
        }

        val validPlays = findAllValidPlays(hand, deck)
        if (validPlays.isNotEmpty()) {
            val play = validPlays.minBy { it.sumOf { c -> rank32(c) } }
            return "putd:${play.toJson()}"
        }

        return "picu:1"
    }

    private fun findClearPlay(hand: Hand): Hand? {
        val byRank = hand.toList().groupBy { rank32(it) }
        for ((rank, cards) in byRank) {
            if (rank == rank32(Card.THREE_CLUBS) && cards.size >= 1) {
                return Hand().apply { add(cards[0]) }
            }
            if (rank == rank32(Card.TEN_CLUBS) && cards.size >= 1) {
                return Hand().apply { add(cards[0]) }
            }
            if (cards.size == 4) {
                return Hand().apply { cards.forEach { add(it) } }
            }
        }
        return null
    }

    private fun findAllValidPlays(hand: Hand, deck: Deck): List<Hand> {
        val last = deck.lastPlay ?: return emptyList()
        val lastRank = rank32(last)
        val validCards = hand.toList().filter { card ->
            val r = rank32(card)
            r == lastRank || r == lastRank + 1 || r == lastRank - 1
        }
        return validCards.map { Hand().apply { add(it) } }
    }

    private fun freeMove(controller: Controller, player: Player, room: Room): String {
        val hand = player.hand
        val deck = room.deck

        val last = deck.lastPlay
        if (last == null) {
            val lowest = hand.toList().minBy { rank2A(it) }
            return "putd:[\"${lowest.name}\"]"
        }

        val validCards = hand.toList().filter { rank2A(it) > rank2A(last) }
        if (validCards.isNotEmpty()) {
            val card = validCards.minBy { rank2A(it) }
            return "putd:[\"${card.name}\"]"
        }

        return "picu:1"
    }

    private fun crazyEightsMove(controller: Controller, player: Player, room: Room): String {
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
            return "putd:[\"${card.name}\"]"
        }

        if (eights.isNotEmpty()) {
            return "putd:[\"${eights[0].name}\"]"
        }

        return "picu:1"
    }

    private fun cheatMove(controller: Controller, player: Player, room: Room): String {
        val hand = player.hand
        val cheatState = room.cheatState

        when (cheatState.phase) {
            CheatState.Phase.BIDDING -> {
                val matching = hand.toList().count { rank2A(it) == cheatState.currentRank }
                val bid = if (difficulty == Difficulty.EASY && rng.nextBoolean())
                    matching + 1
                else
                    matching.coerceAtMost(hand.size)
                return "bid:$bid"
            }
            CheatState.Phase.PLAYING -> {
                val toPlay = hand.toList()
                    .filter { rank2A(it) == cheatState.currentRank }
                    .take(cheatState.bid)
                val final = if (toPlay.size >= cheatState.bid) toPlay
                else toPlay + hand.toList()
                    .filter { rank2A(it) != cheatState.currentRank }
                    .take(cheatState.bid - toPlay.size)
                if (final.isEmpty()) return "skip:"
                val h = Hand().apply { final.forEach { add(it) } }
                return "putd:${h.toJson()}"
            }
            CheatState.Phase.RESOLVING -> "skip:"
        }
        return "skip:"
    }

    private fun oldMaidMove(controller: Controller, player: Player, room: Room): String {
        return "picu:1"
    }

    private fun goFishMove(controller: Controller, player: Player, room: Room): String {
        val hand = player.hand
        val room2 = controller.rooms[player.room]

        val myRanks = hand.toList().map { rank2A(it) }.toSet()
        if (myRanks.isEmpty()) return "picu:1"

        val targetRank = myRanks.randomElement(rng)

        val opponents = room2.slots
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