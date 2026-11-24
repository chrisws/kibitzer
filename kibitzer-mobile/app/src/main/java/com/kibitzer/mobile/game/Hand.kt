package com.kibitzer.mobile.game

class Hand {
    private val _cards = mutableListOf<Card>()

    constructor()
    constructor(other: Hand) { _cards.addAll(other._cards) }

    val size: Int get() = _cards.size

    fun add(card: Card) { _cards.add(card) }

    fun addAll(hand: Hand) { _cards.addAll(hand._cards) }

    fun clear() { _cards.clear() }

    fun peek(): Card = _cards.last()

    fun pop(): Card = _cards.removeAt(_cards.size - 1)

    fun has(card: Card): Boolean = card in _cards

    fun has(hand: Hand): Boolean = hand._cards.all { it in _cards }

    fun hasEqual(card: Card, rank: CardRank): Boolean {
        val value = rank(card)
        return _cards.any { rank(it) == value }
    }

    fun hasHigher(card: Card, minCount: Int, rank: CardRank): Boolean {
        val value = rank(card)
        val counts = mutableMapOf<Int, Int>()
        for (c in _cards) {
            val r = rank(c)
            if (r > value) {
                counts[r] = (counts[r] ?: 0) + 1
            }
        }
        return counts.values.any { it >= minCount }
    }

    fun isEqual(card: Card, rank: CardRank): Boolean {
        val value = rank(card)
        return _cards.all { rank(it) == value }
    }

    fun remove(hand: Hand) {
        _cards.removeAll(hand._cards.toSet())
    }

    fun remove(card: Card) {
        _cards.remove(card)
    }

    fun sort(rank: CardRank) {
        _cards.sortWith(compareBy({ rank(it) }, { it.ordinal }))
    }

    fun swap(i: Int, j: Int) { _cards.swap(i, j) }

    fun get(index: Int): Card = _cards[index]

    fun toList(): List<Card> = _cards.toList()

    fun toJson(): String {
        return _cards.joinToString(", ", prefix = "[", postfix = "]") { "\"${it.name}\"" }
    }

    companion object {
        fun fromJson(json: String): Hand {
            val hand = Hand()
            val cleaned = json.trim().removePrefix("[").removeSuffix("]")
            if (cleaned.isBlank()) return hand
            for (token in cleaned.split(",")) {
                val name = token.trim().removeSurrounding("\"")
                Card.fromName(name)?.let { hand.add(it) }
            }
            return hand
        }
    }

    override fun toString(): String =
        if (_cards.isEmpty()) "empty!" else _cards.joinToString(" ") { it.name }
}