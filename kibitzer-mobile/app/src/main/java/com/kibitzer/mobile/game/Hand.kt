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

class Hand {
  private val _cards = mutableListOf<Card>()

  constructor()
  constructor(other: Hand) {
    _cards.addAll(other._cards)
  }

  val size: Int get() = _cards.size

  fun add(card: Card) {
    _cards.add(card)
  }

  fun addAll(hand: Hand) {
    _cards.addAll(hand._cards)
  }

  fun clear() {
    _cards.clear()
  }

  fun peek(): Card = _cards.last()

  fun pop(): Card = _cards.removeAt(_cards.size - 1)

  fun has(card: Card): Boolean = card in _cards

  fun has(hand: Hand): Boolean = hand._cards.all { it in _cards }

  fun hasEqual(
    card: Card,
    rank: CardRank,
  ): Boolean {
    val value = rank(card)
    return _cards.any { rank(it) == value }
  }

  fun hasHigher(
    card: Card,
    minCount: Int,
    rank: CardRank,
  ): Boolean {
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

  fun isEqual(
    card: Card,
    rank: CardRank,
  ): Boolean {
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

  fun swap(
    i: Int,
    j: Int,
  ) {
    val temp = _cards[i]
    _cards[i] = _cards[j]
    _cards[j] = temp
  }

  fun get(index: Int): Card = _cards[index]

  fun toList(): List<Card> = _cards.toList()

  fun toJson(): String = _cards.joinToString(", ", prefix = "[", postfix = "]") { "\"${it.code}\"" }

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

  override fun toString(): String = if (_cards.isEmpty()) "empty!" else _cards.joinToString(" ") { it.code }
}
