//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#include "rules.h"

static int rank2A(Card card) {
  switch (card) {
  case k2c: return 0;  case k2s: return 0;  case k2h: return 0;  case k2d: return 0;
  case k3c: return 1;  case k3s: return 1;  case k3h: return 1;  case k3d: return 1;
  case k4c: return 2;  case k4s: return 2;  case k4h: return 2;  case k4d: return 2;
  case k5c: return 3;  case k5s: return 3;  case k5h: return 3;  case k5d: return 3;
  case k6c: return 4;  case k6s: return 4;  case k6h: return 4;  case k6d: return 4;
  case k7c: return 5;  case k7s: return 5;  case k7h: return 5;  case k7d: return 5;
  case k8c: return 6;  case k8s: return 6;  case k8h: return 6;  case k8d: return 6;
  case k9c: return 7;  case k9s: return 7;  case k9h: return 7;  case k9d: return 7;
  case kXc: return 8;  case kXs: return 8;  case kXh: return 8;  case kXd: return 8;
  case kjc: return 9;  case kjs: return 9;  case kjh: return 9;  case kjd: return 9;
  case kqc: return 10; case kqs: return 10; case kqh: return 10; case kqd: return 10;
  case kkc: return 11; case kks: return 11; case kkh: return 11; case kkd: return 11;
  case kac: return 12; case kas: return 12; case kah: return 12; case kad: return 12;
  }
  return -1;
}

static int rank32(Card card) {
  switch (card) {
  case k3c: return 0;  case k3s: return 0;  case k3h: return 0;  case k3d: return 0;
  case k4c: return 1;  case k4s: return 1;  case k4h: return 1;  case k4d: return 1;
  case k5c: return 2;  case k5s: return 2;  case k5h: return 2;  case k5d: return 2;
  case k6c: return 3;  case k6s: return 3;  case k6h: return 3;  case k6d: return 3;
  case k7c: return 4;  case k7s: return 4;  case k7h: return 4;  case k7d: return 4;
  case k8c: return 5;  case k8s: return 5;  case k8h: return 5;  case k8d: return 5;
  case k9c: return 6;  case k9s: return 6;  case k9h: return 6;  case k9d: return 6;
  case kXc: return 7;  case kXs: return 7;  case kXh: return 7;  case kXd: return 7;
  case kjc: return 8;  case kjs: return 8;  case kjh: return 8;  case kjd: return 8;
  case kqc: return 9;  case kqs: return 9;  case kqh: return 9;  case kqd: return 9;
  case kkc: return 10; case kks: return 10; case kkh: return 10; case kkd: return 10;
  case kac: return 11; case kas: return 11; case kah: return 11; case kad: return 11;
  case k2c: return 12; case k2s: return 12; case k2h: return 12; case k2d: return 12;
  }
  return -1;
}

struct RulesFree : public Rules {
  bool canPlay(const Deck &deck, const Hand &hand) const {
    bool result;
    if (hand.size() == 0) {
      result = false;
    } else if (!deck.discardSize()) {
      result = true;
    } else {
      result = hand.hasHigher(deck.lastPlay(), 1, rank2A);
    }
    return result;
  }

  bool canRevoke() const {
    return true;
  }

  bool clearDiscard(const Hand &) const {
    return false;
  }

  CardRank getRank() const {
    return rank2A;
  }

  int handSize(int) const {
    return 7;
  }

  bool faceDown() const {
    return false;
  }

  bool isValidPlay(const Deck &, const Hand &) const {
    return true;
  }

  bool isWinningPlay(const Deck &, const Hand &) const {
    return false;
  }

  const char *name() const {
    return "Rules Free";
  }

  const char *url() const {
    return "https://en.wikipedia.org/wiki/Card_game";
  }

  bool noPlayTakesDiscard() const {
    return false;
  }

  bool setNextTurn(const Hand &) const {
    return true;
  }
} rulesFree;

// https://www.pagat.com/climbing/president.html
struct Warlords : public Rules {
  bool canPlay(const Deck &deck, const Hand &hand) const {
    bool result;
    if (hand.size() == 0) {
      result = false;
    } else if (hand.hasEqual(k2c, rank32)) {
      // can play 2 anytime
      result = true;
    } else if (!deck.discardSize()) {
      // discard pile is empty
      result = true;
    } else if (rank32(deck.lastPlay()) == rank32(k2c)) {
      // restart the sequence
      result = true;
    } else {
      // can they play an equal or higher value card
      result = hand.hasHigher(deck.lastPlay(), deck.lastSize(), rank32);
    }
    return result;
  }

  bool clearDiscard(const Hand &hand) const {
    // playing 3s and 10s (or 4 the same) clears the deck
    return (hand.hasEqual(k3c, rank32) || hand.hasEqual(kXc, rank32) || hand.size() == 4);
  }

  bool canRevoke() const {
    return false;
  }

  CardRank getRank() const {
    return rank32;
  }

  int handSize(int players) const {
    return 52 / players;
  }

  bool faceDown() const {
    return false;
  }

  bool isValidPlay(const Deck &deck, const Hand &hand) const {
    bool result;
    if (hand.size() == 0) {
      // played hand was empty
      result = false;
    } else if (!hand.isEqual(hand.peek(), rank32)) {
      // must be 1 or more cards with the same value
      result = false;
    } else if (hand.hasEqual(k2c, rank32)) {
      // can play 2 anytime
      result = true;
    } else if (!deck.discardSize()) {
      // discard pile is empty
      result = true;
    } else if (rank32(deck.lastPlay()) == rank32(k2c)) {
      // restart the sequence
      result = true;
    } else {
      // can they play an higher value card with a hand the same size of the last
      result = hand.hasHigher(deck.lastPlay(), 1, rank32) && hand.size() == deck.lastSize();
    }
    return result;
  }

  bool isWinningPlay(const Deck &, const Hand &hand) const {
    return hand.size() == 0;
  }

  const char *name() const {
    return "Warlords and Scumbags";
  }

  const char *url() const {
    return "https://en.wikipedia.org/wiki/President_(card_game)";
  }

  bool noPlayTakesDiscard() const {
    return true;
  }

  bool setNextTurn(const Hand &hand) const {
    return !clearDiscard(hand);
  }
} warLords;

Rules *getRules(Game game) {
  Rules *result;
  switch (game) {
  case kWarlords:
    result = &warLords;
    break;
  default:
    result = &rulesFree;
    break;
  }
  return result;
}
