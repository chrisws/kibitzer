//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#include <map>
#include "cards.h"

static Card firstCard = k2c;
static Card lastCard = kad;

static const char *cardName(Card card) {
  switch (card) {
  case k2c: return "2C"; case k2d: return "2D"; case k2h: return "2H"; case k2s: return "2S";
  case k3c: return "3C"; case k3d: return "3D"; case k3h: return "3H"; case k3s: return "3S";
  case k4c: return "4C"; case k4d: return "4D"; case k4h: return "4H"; case k4s: return "4S";
  case k5c: return "5C"; case k5d: return "5D"; case k5h: return "5H"; case k5s: return "5S";
  case k6c: return "6C"; case k6d: return "6D"; case k6h: return "6H"; case k6s: return "6S";
  case k7c: return "7C"; case k7d: return "7D"; case k7h: return "7H"; case k7s: return "7S";
  case k8c: return "8C"; case k8d: return "8D"; case k8h: return "8H"; case k8s: return "8S";
  case k9c: return "9C"; case k9d: return "9D"; case k9h: return "9H"; case k9s: return "9S";
  case kXc: return "XC"; case kXd: return "XD"; case kXh: return "XH"; case kXs: return "XS";
  case kjc: return "JC"; case kjd: return "JD"; case kjh: return "JH"; case kjs: return "JS";
  case kqc: return "QC"; case kqd: return "QD"; case kqh: return "QH"; case kqs: return "QS";
  case kkc: return "KC"; case kkd: return "KD"; case kkh: return "KH"; case kks: return "KS";
  case kac: return "AC"; case kad: return "AD"; case kah: return "AH"; case kas: return "AS";
  }
  return "XX";
}

static const map<string, Card> cardNames = {
  {"2C", k2c}, {"2D", k2d}, {"2H", k2h}, {"2S", k2s},
  {"3C", k3c}, {"3D", k3d}, {"3H", k3h}, {"3S", k3s},
  {"4C", k4c}, {"4D", k4d}, {"4H", k4h}, {"4S", k4s},
  {"5C", k5c}, {"5D", k5d}, {"5H", k5h}, {"5S", k5s},
  {"6C", k6c}, {"6D", k6d}, {"6H", k6h}, {"6S", k6s},
  {"7C", k7c}, {"7D", k7d}, {"7H", k7h}, {"7S", k7s},
  {"8C", k8c}, {"8D", k8d}, {"8H", k8h}, {"8S", k8s},
  {"9C", k9c}, {"9D", k9d}, {"9H", k9h}, {"9S", k9s},
  {"XC", kXc}, {"XD", kXd}, {"XH", kXh}, {"XS", kXs},
  {"JC", kjc}, {"JD", kjd}, {"JH", kjh}, {"JS", kjs},
  {"QC", kqc}, {"QD", kqd}, {"QH", kqh}, {"QS", kqs},
  {"KC", kkc}, {"KD", kkd}, {"KH", kkh}, {"KS", kks},
  {"AC", kac}, {"AD", kad}, {"AH", kah}, {"AS", kas}
};

bool compare(const Card &c1, const Card &c2, CardRank rank) {
  int r1 = rank(c1);
  int r2 = rank(c2);
  bool result;
  if (r1 != r2) {
    result = r1 < r2;
  } else {
    result = c1 < c2;
  }
  return result;
}

Hand::Hand(const list<unique_ptr<string>> &cards) {
  for (auto &&next : cards) {
    if (cardNames.find(*next) != cardNames.end()) {
      _cards.push_back(cardNames.at(*next));
    }
  }
}

void Hand::addAll(const Hand &hand) {
  for (Card next : hand._cards) {
    _cards.push_back(next);
  }
}

bool Hand::hasEqual(Card card, CardRank rank) const {
  int value = rank(card);
  bool result = false;
  for (Card next : _cards) {
    if (rank(next) == value) {
      result = true;
      break;
    }
  }
  return result;
}

bool Hand::hasHigher(Card card, int minCount, CardRank rank) const {
  auto comparator = [&](const Card &c1, const Card &c2) {
    return compare(c1, c2, rank);
  };

  map<Card, int, decltype(comparator)> counts(comparator);
  int value = rank(card);

  for (Card next : _cards) {
    if (rank(next) > value) {
      counts[next]++;
    }
  }

  bool found = false;
  for (auto it = counts.cbegin(); it != counts.cend(); ++it) {
    if ((*it).second >= minCount) {
      found = true;
    }
  }
  return found;
}

bool Hand::has(const Hand &hand) const {
  bool result = true;
  for (Card next : hand._cards) {
    if (find(_cards.begin(), _cards.end(), next) == _cards.end()) {
      result = false;
      break;
    }
  }
  return result;
}

bool Hand::isEqual(Card card, CardRank rank) const {
  int value = rank(card);
  bool result = true;
  for (Card next : _cards) {
    if (rank(next) != value) {
      result = false;
      break;
    }
  }
  return result;
}

Card Hand::pop() {
  Card card = _cards.back();
  _cards.pop_back();
  return card;
}

void Hand::remove(const Hand &hand) {
  vector<Card> newCards;
  for (auto card: _cards) {
    bool found = false;
    for (auto inner : hand._cards) {
      if (inner == card) {
        found = true;
        break;
      }
    }
    if (!found) {
      newCards.push_back(card);
    }
  }
  _cards = newCards;
}

void Hand::sort(CardRank rank) {
  ::sort( _cards.begin(), _cards.end(), [&](const Card &c1, const Card &c2) {
    return compare(c1, c2, rank);
  });
}

void Hand::swap(int i, int j) {
  Card card = _cards[j];
  _cards[j] = _cards[i];
  _cards[i] = card;
}

string Hand::toJson() const {
  string json = "[";
  bool next = false;
  for (Card card : _cards) {
    if (next) {
      json.push_back(',');
    } else {
      next = true;
    }
    json.push_back('"');
    json.append(cardName(card));
    json.push_back('"');
  }
  json.push_back(']');
  return json;
}

string Hand::toString() const {
  string result;
  vector<Card> newCards;
  bool next = false;
  for (auto card: _cards) {
    if (next) {
      result.append(" ");
    } else {
      next = true;
    }
    result.append(cardName(card));
  }
  if (result.length() == 0) {
    result.append("empty!");
  }
  return result;
}

Deck::Deck() :
  _lastSize(0),
  _last(-1) {
}

void Deck::deal(Hand &hand, int cards)  {
  hand.clear();
  for (int i = 0; i < cards; i++) {
    if (_pack.size()) {
      hand.add(_pack.pop());
    } else {
      break;
    }
  }
  if (_pack.size() == 1) {
    // deal last remaining card
    hand.add(_pack.pop());
  }
}

Hand Deck::pickup(Hand &hand, int cards) {
  Hand result;
  for (int i = 0; i < cards; i++) {
    if (_pack.size()) {
      Card card = _pack.pop();
      result.add(card);
      hand.add(card);
    }
  }
  return result;
}

void Deck::putdown(const Hand &hand) {
  _discard.addAll(hand);
  _last = _discard.peek();
  _lastSize = hand.size();
}

void Deck::shuffle() {
  _discard.clear();
  _last = -1;
  _lastSize = 0;
  _pack.clear();

  for (int i = firstCard; i <= lastCard; i++) {
    _pack.add(static_cast<Card>(i));
  }

  srand(time(nullptr));
  for (int i = _pack.size() - 1; i > 0; i--) {
    _pack.swap(rand() % i, i);
  }
}

void Deck::takeDiscard(Hand &hand) {
  hand.addAll(_discard);
  _discard.clear();
  _last = -1;
  _lastSize = 0;
}

