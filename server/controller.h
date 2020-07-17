//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#pragma once

#include <vector>
#include <memory>
#include "cards.h"
#include "message.h"
#include "rules.h"

using namespace std;

static const int numRooms = 10;

enum PlayerState {
  kLurk,
  kJoined,
  kDealt
};

struct Player {
  Player(int sessionId);
  virtual ~Player() {}

  const string toJson() const;
  const string name() const { return "(" + _nicname + ")"; }

  Hand _hand;
  string _nicname;
  int _slot;
  int _sessionId;
  int _points;
  int _room;
  PlayerState _state;
};

typedef const unique_ptr<Player> PlayerPtr;

struct SaveState {
  SaveState() : _sessionId(-1) {}
  Hand _hand;
  Deck _deck;
  int _sessionId;
};

struct Room {
  Room();
  virtual ~Room() {}

  // allows players to revoke the last play
  SaveState _saveState;

  // the card deck
  Deck _deck;

  // the free slots for active players
  vector<int> _slots;

  // the rules played in this room
  Rules *_rules;

  // session id of the current player turn
  int _turn;
};

struct Controller {
  Controller();
  virtual ~Controller() {}

  int createSession();
  const Message destroySession(int sessionId);
  bool handle(const unsigned char *data, size_t len, Message &response, int sessionId);
  bool handle(const string data, Message &response, int sessionId);
  bool isSameRoom(int session1, int session2);
  const Message redact(int sessionId, const Message &message);

private:
  // whether the next turn player can play
  bool canPlay(int sessionId, string &message);

  // the game after picking up or putting down
  const string cards(PlayerPtr &player, int room, const string &message);

  // find the users session
  vector<unique_ptr<Player>>::iterator findSession(int sessionId);

  // returns the Command enum for the given string
  MessageType getMessageType(const string &str) const;

  // returns the sessionId of the next players turn
  int nextTurn(int room) const;

  // returns the number of players
  int playing(int room);

  // returns the json for players in the give room
  const string players(int room);

  // save the current play state
  void saveState(PlayerPtr &player);

  // setup the next player
  void setNextTurn(int room, string &message);

  // the number of slots in the room
  int slots(int room) const { return (int)_rooms[room]._slots.size(); }

  // whether it is the players turn
  bool isTurn(PlayerPtr &player);

  // chat message to all players
  bool chat(Message &response, PlayerPtr &player, const string &str);

  // deal out the player's hand
  bool deal(Message &response, PlayerPtr &player);

  // the player won the round
  bool gameover(Message &response, PlayerPtr &player);

  // player giving cards to another player
  bool exchange(Message &response, PlayerPtr &player, const string &str);

  // web client init
  bool init(Message &response, PlayerPtr &player);

  // join error
  const string joinError(PlayerPtr &player);

  // join the game
  bool join(Message &response, PlayerPtr &player, const string &str);

  // set the players nicname
  bool nic(Message &response, PlayerPtr &player, const string &str);

  // the player takes cards from the pile
  bool pickup(Message &response, PlayerPtr &player, string count);

  // takes the given hand from the player and adds to the pile
  bool putdown(Message &response, PlayerPtr &player, const Hand &hand);

  // enter a different play roon
  bool room(Message &response, PlayerPtr &player, const string &str);

  // shuffle the deck
  bool shuffle(Message &response, PlayerPtr &player, const string &str);

  // skip turn
  bool skip(Message &response, PlayerPtr &player);

  // the game players including lurkers
  vector<unique_ptr<Player>> _players;

  // the play rooms
  Room _rooms[numRooms];
};
