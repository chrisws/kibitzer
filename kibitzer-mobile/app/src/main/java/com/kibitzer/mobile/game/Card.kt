package com.kibitzer.mobile.game

enum class Card(val name: String) {
    TWO_CLUBS("2C"), TWO_DIAMONDS("2D"), TWO_HEARTS("2H"), TWO_SPADES("2S"),
    THREE_CLUBS("3C"), THREE_DIAMONDS("3D"), THREE_HEARTS("3H"), THREE_SPADES("3S"),
    FOUR_CLUBS("4C"), FOUR_DIAMONDS("4D"), FOUR_HEARTS("4H"), FOUR_SPADES("4S"),
    FIVE_CLUBS("5C"), FIVE_DIAMONDS("5D"), FIVE_HEARTS("5H"), FIVE_SPADES("5S"),
    SIX_CLUBS("6C"), SIX_DIAMONDS("6D"), SIX_HEARTS("6H"), SIX_SPADES("6S"),
    SEVEN_CLUBS("7C"), SEVEN_DIAMONDS("7D"), SEVEN_HEARTS("7H"), SEVEN_SPADES("7S"),
    EIGHT_CLUBS("8C"), EIGHT_DIAMONDS("8D"), EIGHT_HEARTS("8H"), EIGHT_SPADES("8S"),
    NINE_CLUBS("9C"), NINE_DIAMONDS("9D"), NINE_HEARTS("9H"), NINE_SPADES("9S"),
    TEN_CLUBS("XC"), TEN_DIAMONDS("XD"), TEN_HEARTS("XH"), TEN_SPADES("XS"),
    JACK_CLUBS("JC"), JACK_DIAMONDS("JD"), JACK_HEARTS("JH"), JACK_SPADES("JS"),
    QUEEN_CLUBS("QC"), QUEEN_DIAMONDS("QD"), QUEEN_HEARTS("QH"), QUEEN_SPADES("QS"),
    KING_CLUBS("KC"), KING_DIAMONDS("KD"), KING_HEARTS("KH"), KING_SPADES("KS"),
    ACE_CLUBS("AC"), ACE_DIAMONDS("AD"), ACE_HEARTS("AH"), ACE_SPADES("AS");

    val rank: Int
        get() = when (name[0]) {
            '2' -> 2
            '3' -> 3
            '4' -> 4
            '5' -> 5
            '6' -> 6
            '7' -> 7
            '8' -> 8
            '9' -> 9
            'X' -> 10
            'J' -> 11
            'Q' -> 12
            'K' -> 13
            'A' -> 14
            else -> 0
        }

    val suit: Char
        get() = name[1]

    companion object {
        fun fromName(name: String): Card? =
            entries.find { it.name == name }
    }
}

typealias CardRank = (Card) -> Int

fun rank2A(card: Card): Int = card.rank - 2  // 2=0, 3=1, ..., A=12

fun rank32(card: Card): Int =
    if (card.rank == 2) 12 else card.rank - 3  // 3=0, 4=1, ..., A=11, 2=12