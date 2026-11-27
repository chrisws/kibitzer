package com.kibitzer.mobile.game

class OldMaidRules : Rules() {
  companion object {
    val OLD_MAID = Card.TWO_SPADES
    val DECK_CARDS: List<Card> =
      Card.entries.filter {
        it != Card.TWO_CLUBS && it != Card.TWO_DIAMONDS && it != Card.TWO_HEARTS
      }
  }

  override fun canPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size > 1

  override fun canRevoke(): Boolean = false

  override fun clearDiscard(hand: Hand): Boolean = false

  override fun faceDown(): Boolean = true

  override fun getRank(): CardRank = ::rank2A

  override fun handSize(players: Int): Int = 49 / players

  override fun isValidPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = true

  override fun isWinningPlay(
    deck: Deck,
    hand: Hand,
  ): Boolean = hand.size == 0

  override fun name(): String = "Old Maid"

  override fun url(): String = "https://en.wikipedia.org/wiki/Old_Maid_(card_game)"

  override fun noPlayTakesDiscard(): Boolean = false

  override fun setNextTurn(hand: Hand): Boolean = true
}
