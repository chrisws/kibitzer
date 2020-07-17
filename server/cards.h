//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#pragma once

#include <list>
#include <string>
#include <vector>
#include <memory>
#include <algorithm>

using namespace std;

enum Card {
  k2c, k2s, k2h, k2d,
  k3c, k3s, k3h, k3d,
  k4c, k4s, k4h, k4d,
  k5c, k5s, k5h, k5d,
  k6c, k6s, k6h, k6d,
  k7c, k7s, k7h, k7d,
  k8c, k8s, k8h, k8d,
  k9c, k9s, k9h, k9d,
  kXc, kXs, kXh, kXd,
  kjc, kjs, kjh, kjd,
  kqc, kqs, kqh, kqd,
  kkc, kks, kkh, kkd,
  kac, kas, kah, kad
};

typedef int (*CardRank)(Card card);

// a hand of cards
struct Hand {
  Hand() {}
  Hand(const Hand &hand) {
    _cards = hand._cards;
  }
  Hand(const list<unique_ptr<string>> &cards);

  Hand& operator=(const Hand &hand) {
    _cards = hand._cards;
    return *this;
  }

  virtual ~Hand() {}

  // add the card to the hand
  void add(const Card card) { _cards.push_back(card); }

  // add all the cards to the hand
  void addAll(const Hand &hand);

  // clear the hand
  void clear() { _cards.clear(); }

  // whether the hand has an equal value card
  bool hasEqual(Card card, CardRank rank) const;

  // whether the hand has the minimum number of an higher value card
  bool hasHigher(Card card, int minCount, CardRank rank) const;

  // whether the hand has all the cards of the given hand
  bool has(const Hand &hand) const;

  // whether the hand has only equal value cards
  bool isEqual(Card card, CardRank rank) const;

  // peek the last card from the hand
  Card peek() const { return _cards.back(); }

  // pop the last card from the hand
  Card pop();

  // remove the given hand from this hand
  void remove(const Hand &hand);

  // the number of cards in the hand
  int size() const { return _cards.size(); }

  // sort the cards
  void sort(CardRank rank);

  // swaps the cards in the given positions
  void swap(int i, int j);

  // returns the JSON string for the hand
  string toJson() const;

  // returns the hand as text
  string toString() const;

private:
  vector<Card> _cards;
};

// the deck of cards
struct Deck {
  Deck();
  virtual ~Deck() {}

  Deck& operator=(const Deck &deck) {
    _pack = deck._pack;
    _discard = deck._discard;
    return *this;
  }

  // clear the discard pile
  void clearDiscard() { _discard.clear(); }

  // deal out the suffled deck making n-hands of n-cards
  void deal(Hand &hand, int cards);

  // the size of the discard pile
  int discardSize() const { return _discard.size(); }

  // returns the json representation of the pack
  string getPack() const { return _pack.toJson(); }

  // returns the json representation of the dicard pile
  string getDiscard() const { return _discard.toJson(); }

  // the last card on the pile
  Card lastPlay() const { return static_cast<Card>(_last); }

  // the number of cards played in the last play
  int lastSize() const { return _lastSize; }

  // the size of the pack
  int packSize() const { return _pack.size(); }

  // pickup n-cards into the given hand from the pack
  Hand pickup(Hand &hand, int cards);

  // putdown the given hand into the discard pile
  void putdown(const Hand &hand);

  // shuffle the deck to begin the game
  void shuffle();

  // take all the card from the discard pile
  void takeDiscard(Hand &hand);

private:
  Hand _pack;
  Hand _discard;
  int _lastSize;
  int _last;
};
