package com.kibitzer.mobile.game

data class CheatState(
  val phase: Phase = Phase.BIDDING,
  val bid: Int = 0,
  val bidSessionId: Int = -1,
  val called: Boolean = false,
  val calledBy: Int = -1,
  val playedCards: List<Card> = emptyList(),
  val currentRank: Int = 1,
) {
  enum class Phase { BIDDING, PLAYING, RESOLVING }

  fun withBid(
    sessionId: Int,
    count: Int,
  ) = copy(phase = Phase.PLAYING, bid = count, bidSessionId = sessionId)

  fun withPlayedCards(cards: List<Card>) = copy(playedCards = cards)

  fun withCalled(sessionId: Int) = copy(called = true, calledBy = sessionId, phase = Phase.RESOLVING)

  fun nextTurn() =
    copy(
      phase = Phase.BIDDING,
      bid = 0,
      bidSessionId = -1,
      called = false,
      calledBy = -1,
      playedCards = emptyList(),
      currentRank = if (currentRank >= 13) 1 else currentRank + 1,
    )

  fun reset() = CheatState()

  fun isBidValid(played: List<Card>): Boolean {
    val matching = played.count { rank2A(it) == currentRank }
    return matching >= bid
  }
}

class CheatRules : Rules() {
  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size > 0

  override fun canRevoke(): Boolean = false

  override fun clearDiscard(hand: Hand): Boolean = false

  override fun faceDown(): Boolean = true

  override fun getRank(): CardRank = ::rank2A

  override fun handSize(players: Int): Int = (if (players <= 4) 52 else 104) / players

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size > 0

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size == 0

  override fun name(): String = "Cheat"

  override fun url(): String = "https://en.wikipedia.org/wiki/Cheat_(card_game)"

  override fun noPlayTakesDiscard(): Boolean = true

  override fun setNextTurn(hand: Hand): Boolean = true
}
