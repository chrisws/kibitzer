package com.kibitzer.mobile.game

class OldMaidHandler : GameHandler {

    override fun handlePutdown(controller: Controller, player: Player, hand: Hand): Message {
        // Old Maid: "play" means finding pairs in hand
        // Pairs are found automatically - scan hand for matching ranks
        val pairs = findPairs(player.hand)
        if (pairs.isEmpty()) {
            return Message("message", "${player.name()} has no pairs")
        }
        // Remove all pairs from hand
        for ((a, b) in pairs) {
            player.hand.remove(a)
            player.hand.remove(b)
        }
        // Check if player has the Old Maid
        if (player.hand.has(OldMaidRules.OLD_MAID)) {
            // Player loses - they have the Old Maid
            // Game continues until only one player has cards
        }
        // Draw one card from a random opponent
        val room = controller.let { null } // controller doesn't expose rooms directly for handler
        // Turn advancement is handled by Controller.setNextTurn
        return Message("cards", """{"message":"${player.name()} discarded ${pairs.size} pair(s)","hand":${player.hand.toJson()}}""")
    }

    private fun findPairs(hand: Hand): List<Pair<Card, Card>> {
        val pairs = mutableListOf<Pair<Card, Card>>()
        val seen = mutableMapOf<Int, Card>()
        for (card in hand.toList()) {
            val rank = rank2A(card)
            val prev = seen[rank]
            if (prev != null) {
                pairs.add(prev to card)
                seen.remove(rank)
            } else {
                seen[rank] = card
            }
        }
        return pairs
    }
}