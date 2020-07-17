//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#pragma once

#include <string>
#include "cards.h"

enum Game {
  kRulesFree,
  kWarlords
};

struct Rules {
  Rules() {}
  virtual ~Rules() {}

  // whether the player's hand contains a card to make a valid  next play
  virtual bool canPlay(const Deck &deck, const Hand &hand) const = 0;

  // whether a player can revoke the last players turn
  virtual bool canRevoke() const = 0;

  // whether the hand causes the discard pile to be discarded
  virtual bool clearDiscard(const Hand &hand) const = 0;

  // whether cards should be placed face down
  virtual bool faceDown() const = 0;

  // returns the game's ranking function
  virtual CardRank getRank() const = 0;

  // the number of cards to deal given the number of players
  virtual int handSize(int players) const = 0;

  // whether they played a valid hand
  virtual bool isValidPlay(const Deck &deck, const Hand &hand) const = 0;

  // whether the hand was a winning play
  virtual bool isWinningPlay(const Deck &deck, const Hand &hand) const = 0;

  // the card game name
  virtual const char *name() const = 0;

  // the card game name home page
  virtual const char *url() const = 0;

  // whether not being able to play means taking the discard pile
  virtual bool noPlayTakesDiscard() const = 0;

  // whether the hand allows selecting the next turn
  virtual bool setNextTurn(const Hand &hand) const = 0;
};

Rules *getRules(Game game);
