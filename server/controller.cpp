//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#include <algorithm>
#include <sstream>
#include <iostream>
#include <map>
#include <limits.h>
#include "controller.h"
#include "utils.h"
#include "config.h"

static int nextId = 1;
static size_t cmdSize = 5;
static int maxPlayers = 6;

static const map<string, MessageType> messageTypes = {
  {"chat:", kChat},
  {"deal:", kDeal},
  {"exch:", kExchange},
  {"init:", kInit},
  {"join:", kJoin},
  {"nicn:", kNicname},
  {"picu:", kPickup},
  {"putd:", kPutDown},
  {"room:", kRoom},
  {"shuf:", kShuffle},
  {"skip:", kSkip}
};

static list<unique_ptr<string>> toArray(const string &str) {
  list<unique_ptr<string>> result;
  int brackets = 0;
  int start = -1;
  bool quotes = false;
  bool escape = false;
  bool error = false;

  for (size_t i = 0; i < str.size() && !error; i++) {
    switch (str[i]) {
    case '[':
      if (!escape) {
        brackets++;
      }
      break;
    case ']':
      if (!escape) {
        brackets--;
      }
      break;
    case '\\':
      escape = !escape;
      break;
    case ',':
      escape = false;
      quotes = false;
      start = -1;
      break;
    case ' ':
      break;
    case '"':
      if (!escape) {
        quotes = !quotes;
        if (quotes) {
          start = i + 1;
        } else if (brackets < 1 || start < 0) {
          error = true;
        } else {
          string s = str.substr(start, i - start);
          result.push_back(make_unique<string>(s));
        }
      }
      escape = false;
      break;
    default:
      if (!quotes) {
        error = true;
      }
      break;
    }
  }
  if (error) {
    log("error in list %s\n", str.c_str());
  }
  return result;
}

int toInt(const string &str) {
  int result = 0;
  int index = 0;
  while (str[index] == ' ') {
    index++;
  }
  while (isdigit(str[index])) {
    result = (result * 10) + (str[index] - '0');
    index++;
  }
  return result;
}

const string fromInt(int value) {
  char num[12];
  sprintf(num, "%d", value);
  return num;
}

const string field(const string &name, const string &value, bool next = false) {
  string result;
  if (next) {
    result.push_back(',');
  }
  result.push_back('\"');
  result.append(name);
  if (value[0] == '[' || value[0] == '{' || value == "true" || value == "false") {
    result.append("\":");
    result.append(value);
  } else {
    result.append("\":\"");
    result.append(value);
    result.push_back('\"');
  }
  return result;
}

const string field(const string &name, int value, bool next = false) {
  string result;
  if (next) {
    result.push_back(',');
  }
  result.push_back('\"');
  result.append(name);
  result.append("\":");
  result.append(fromInt(value));
  return result;
}

const string envelope(const string &id, const string &data) {
  string result;
  result.push_back('{');
  result.append(field("id", id, false));
  result.append(field("data", data, true));
  result.push_back('}');
  return result;
}

const string escape(const string &str) {
  string result;
  for (char c : str) {
    if (c == '"') {
      result.push_back('\\');
    }
    result.push_back(c);
  }
  return result;
}

const string message(const string &str) {
  return envelope("message", str);
}

const string player(int sessionId, bool active, const string &nicname) {
  string result;
  result.push_back('{');
  result.append(field("sessionId", sessionId), false);
  result.append(field("active", active, true));
  result.append(field("nic", nicname, true));
  result.push_back('}');
  return result;
}

const string plural(const string term, int size) {
  string result;
  if (size == 1) {
    result = fromInt(size) + " " + term;
  } else {
    result = fromInt(size) + " " + term + "s";
  }
  return result;
}

string replace(string subject, const string& search, const string& replace) {
  size_t pos = 0;
  while ((pos = subject.find(search, pos)) != string::npos) {
    subject.replace(pos, search.length(), replace);
    pos += replace.length();
  }
  return subject;
}

Player::Player(int sessionId) :
  _slot(-1),
  _sessionId(sessionId),
  _points(0),
  _room(0),
  _state(kLurk) {
  _nicname = "#" + fromInt(sessionId);
}

const string Player::toJson() const {
  return ::player(_sessionId, (_state != kLurk && _slot != -1), _nicname);
}

Room::Room() :
  _rules(nullptr),
  _turn(-1) {
  for (int i = 0; i < maxPlayers; i++) {
    _slots.push_back(-1);
  }
}

Controller::Controller() {
  // configure the rooms
  for (int i = 0; i < numRooms - 1; i++) {
    _rooms[i]._rules = getRules(kWarlords);
  }
  _rooms[numRooms - 1]._rules = getRules(kRulesFree);
}

int Controller::createSession() {
  int sessionId = nextId++;
  log("create session: [%d]\n", sessionId);
  _players.push_back(make_unique<Player>(sessionId));
  return sessionId;
}

const Message Controller::destroySession(int sessionId) {
  Message result;
  auto player = findSession(sessionId);
  if (player != _players.end()) {
    int room = (*player)->_room;
    int slot = (*player)->_slot;
    if (slot > -1 && slot < slots(room)) {
      // free the game slot
      _rooms[room]._slots[slot] = -1;
    }

    string name = (*player)->name();
    string playersJson = players((*player)->_room);
    _players.erase(remove(_players.begin(), _players.end(), *player));

    string json;
    json.push_back('{');
    json.append(field("id", sessionId, false));
    json.append(field("players", playersJson, true));
    json.append(field("name", name, true));
    json.push_back('}');
    result.build(envelope("exit", json), kChat);

    log("destroy session: [%d]\n", sessionId);
  } else {
    log("session not found: [%d]\n", sessionId);
  }
  return result;
}

bool Controller::handle(const string data, Message &response, int sessionId) {
  return handle((const unsigned char *)data.c_str(), data.length(), response, sessionId);
}

bool Controller::handle(const unsigned char *data, size_t len, Message &response, int sessionId) {
  auto player = findSession(sessionId);
  bool result = true;
  if (player != _players.end() && len >= cmdSize) {
    string *message = new string((const char *)data, len);
    response.broadcast("");
    MessageType type = getMessageType(message->substr(0, cmdSize));
    switch (type) {
    case kChat:
      result = chat(response, *player, message->substr(cmdSize));
      break;
    case kDeal:
      result = deal(response, *player);
      break;
    case kExchange:
      result = exchange(response, *player, message->substr(cmdSize));
      break;
    case kInit:
      result = init(response, *player);
      break;
    case kJoin:
      result = join(response, *player, message->substr(cmdSize));
      break;
    case kNicname:
      result = nic(response, *player, message->substr(cmdSize));
      break;
    case kPickup:
      result = pickup(response, *player, message->substr(cmdSize));
      break;
    case kPutDown:
      result = putdown(response, *player, Hand(toArray(message->substr(cmdSize))));
      break;
    case kRoom:
      result = room(response, *player, message->substr(cmdSize));
      break;
    case kShuffle:
      result = shuffle(response, *player, message->substr(cmdSize));
      break;
    case kSkip:
      result = skip(response, *player);
      break;
    default:
      log("invalid message: %s", message->c_str());
      break;
    }
    delete message;
  } else if (len < 4) {
    log("invalid message\n");
    result = false;
  } else {
    log("session not found: [%d]\n", sessionId);
    result = false;
  }
  return result;
}

bool Controller::isSameRoom(int session1, int session2) {
  auto player1 = findSession(session1);
  auto player2 = findSession(session2);
  return player1 != _players.end() && player2 != _players.end() && (*player1)->_room == (*player2)->_room;
}

const Message Controller::redact(int sessionId, const Message &message) {
  Message result;
  auto player = findSession(sessionId);

  switch (message._type) {
  case kDeal:
  case kExchange:
  case kPickup:
  case kPutDown:
  case kSkip:
    if (player != _players.end()) {
      result.build(cards(*player, (*player)->_room, message.broadcast()), message._type);
    } else {
      result.build(cards(nullptr, 0, message.broadcast()), message._type);
    }
    break;
  default:
    result.build(message);
    break;
  }
  return result;
}

bool Controller::canPlay(int sessionId, string &message) {
  auto player = find_if(_players.begin(), _players.end(), [&](PlayerPtr &next) {
    return next->_sessionId == sessionId;
  });
  bool result;
  if (player != _players.end()) {
    int room = (*player)->_room;
    Rules *rules = _rooms[room]._rules;
    Deck &deck = _rooms[room]._deck;

    result = rules->canPlay(deck, (*player)->_hand);
    if (!result && rules->noPlayTakesDiscard()) {
      deck.takeDiscard((*player)->_hand);
      (*player)->_hand.sort(rules->getRank());
      result = rules->canPlay(deck, (*player)->_hand);
      message.append(" " + (*player)->name() + " picked up the deck.");
    }
  } else {
    result = false;
  }
  return result;
}

const string Controller::cards(PlayerPtr &player, int room, const string &message) {
  string json;
  json.push_back('{');
  json.append(field("pile", _rooms[room]._deck.getDiscard(), false));
  json.append(field("message", message, true));
  json.append(field("turn", _rooms[room]._turn, true));
  json.append(field("faceDown", _rooms[room]._rules->faceDown(), true));
  json.append(field("game", _rooms[room]._rules->name(), true));
  if (player != nullptr) {
    json.append(field("hand", player->_hand.toJson(), true));
    json.append(field("sessionId", player->_sessionId, true));
  }
  json.push_back('}');
  return envelope("cards", json);
}

vector<unique_ptr<Player>>::iterator Controller::findSession(int sessionId) {
  return find_if(_players.begin(), _players.end(), [&](PlayerPtr &next) {
    return next->_sessionId == sessionId;
  });
}

MessageType Controller::getMessageType(const string &str) const {
  MessageType result;
  if (messageTypes.find(str) == messageTypes.end()) {
    result = kZUnknown;
  } else {
    result = messageTypes.at(str);
  }
  return result;
}

int Controller::nextTurn(int room) const {
  int size = 0;
  int current = 0;
  int result = -1;
  for (auto &&player : _players) {
    if (player->_room == room && player->_state == kDealt) {
      if (player->_sessionId == _rooms[room]._turn) {
        current = size;
      } else if (_rooms[room]._turn == -1) {
        result = player->_sessionId;
      }
      size++;
    }
  }

  if (size) {
    int turn = (current + 1) % size;
    int count = 0;
    for (auto &&player : _players) {
      if (player->_room == room && player->_state == kDealt) {
        if (turn == count) {
          result = player->_sessionId;
          break;
        }
        count++;
      }
    }
  }
  return result;
}

int Controller::playing(int room) {
  int result = 0;
  for (int i = 0; i < slots(room); i++) {
    int slot = _rooms[room]._slots[i];
    if (slot != -1) {
      auto player = findSession(slot);
      if (player != _players.end()) {
        result++;
      }
    }
  }
  return result;
}

const string Controller::players(int room) {
  string result;
  bool comma = false;

  result.push_back('[');
  for (int i = 0; i < slots(room); i++) {
    if (comma) {
      result.push_back(',');
    } else {
      comma = true;
    }
    int sessionId = _rooms[room]._slots[i];
    if (sessionId == -1) {
      // empty slot
      result.append(::player(-1, false, "empty " + fromInt(i + 1)));
    } else {
      auto session = findSession(sessionId);
      if (session != _players.end()) {
        result.append((*session)->toJson());
      } else {
        // slot was invalid
        _rooms[room]._slots[i] = -1;
        result.append(::player(-1, false, "empty " + fromInt(i + 1)));
      }
    }
  }
  result.push_back(']');
  return result;
}

void Controller::saveState(PlayerPtr &player) {
  int room = player->_room;
  _rooms[room]._saveState._deck = _rooms[room]._deck;
  _rooms[room]._saveState._hand = player->_hand;
  _rooms[room]._saveState._sessionId = player->_sessionId;
}

void Controller::setNextTurn(int room, string &message) {
  int current = _rooms[room]._turn;
  int next = nextTurn(room);
  for (size_t count = 0; next != current && count < _players.size(); count++) {
    if (canPlay(next, message)) {
      _rooms[room]._turn = next;
      break;
    } else {
      next = nextTurn(room);
    }
  }
}

bool Controller::isTurn(PlayerPtr &player) {
  int turn = _rooms[player->_room]._turn;
  return turn == -1 || player->_sessionId == turn;
}

bool Controller::chat(Message &response, PlayerPtr &player, const string &str) {
  return response.build(message(player->name() + " " + str), kChat);
}

bool Controller::deal(Message &response, PlayerPtr &player) {
  int room = player->_room;
  Rules *rules = _rooms[room]._rules;

  if (player->_state == kJoined) {
    _rooms[room]._deck.deal(player->_hand, rules->handSize(playing(room)));
    player->_hand.sort(rules->getRank());
    player->_state = kDealt;
  }

  response.broadcast(player->name() + " received cards");
  string ready = string("Ready to play: <a target=new href='") + rules->url() + "'>" + string(rules->name()) + "</a>";
  return response.build(cards(player, room, ready), kDeal);
}

bool Controller::gameover(Message &response, PlayerPtr &player) {
  string message = player->name() + " won the round, you can't beat skill!<br/><br/>Score:";
  player->_points++;

  for (auto &&next : _players) {
    if (next->_room == player->_room) {
      if (next->_state == kDealt) {
        next->_state = kJoined;
      }
      next->_hand.clear();
      message.append("<br/>")
        .append(next->name())
        .append(": ")
        .append(fromInt(next->_points));
    }
  }
  response.broadcast(message);
  return response.build(cards(player, player->_room, message), kPutDown);
}

bool Controller::exchange(Message &response, PlayerPtr &player, const string &str) {
  istringstream stream(str);
  string out, toId, hand;
  char command = '\0';

  for (int i = 0; i < 3 && getline(stream, out, ':'); i++) {
    switch (i) {
    case 0:
      command = out[0];
      break;
    case 1:
      toId = out;
      break;
    case 2:
      hand = out;
      break;
    }
  }

  string result;
  MessageType messageType = kChat;
  Hand toGive(toArray(hand));
  auto fromPlayer = findSession(toInt(toId));

  if (fromPlayer == _players.end()) {
    result = message("other player has left");
  } else if (command == 'Q') {
    string json;
    json.push_back('{');
    json.append(field("fromId", player->_sessionId, false));
    json.append(field("from", player->name(), true));
    json.append(field("toId", toId, true));
    json.append(field("hand", hand, true));
    json.append(field("message", player->name() + " offering " + toGive.toString() + " to " + (*fromPlayer)->name(), true));
    json.push_back('}');
    result = envelope("exchange", json);
  } else if ((*fromPlayer)->_hand.has(toGive)) {
    Rules *rules = _rooms[player->_room]._rules;
    (*fromPlayer)->_hand.remove(toGive);
    player->_hand.addAll(toGive);
    player->_hand.sort(rules->getRank());
    result = cards(player, player->_room, player->name() + " took " + toGive.toString() + " from " + (*fromPlayer)->name());
    messageType = kExchange;
  } else {
    result = message((*fromPlayer)->name() + " no longer has " + toGive.toString() + " to give");
  }
  return response.build(result, messageType);
}

bool Controller::init(Message &response, PlayerPtr &player) {
  const string welcome = "<p>" PACKAGE_STRING;
  string json;
  json.push_back('{');
  json.append(field("welcome", welcome, false));
  json.append(field("sessionId", player->_sessionId, true));
  json.append(field("players", players(player->_room), true));
  json.push_back('}');

  return response.build(envelope("init", json), kInit);
}

const string Controller::joinError(PlayerPtr &player) {
  // refresh players
  string json;
  json.push_back('{');
  json.append(field("message", player->name() + " poked nobody", false));
  json.append(field("players", players(player->_room), true));
  json.push_back('}');
  return envelope("players", json);
}

bool Controller::join(Message &response, PlayerPtr &player, const string &str) {
  string result;
  int slot = toInt(str);
  int room = player->_room;
  Rules *rules = _rooms[room]._rules;

  if (player->_state == kLurk && slot >= 0 && slot < slots(room) && _rooms[room]._slots[slot] == -1) {
    _rooms[room]._slots[slot] = player->_sessionId;
    player->_state = kJoined;
    player->_slot = slot;

    string json;
    json.push_back('{');
    json.append(field("message", player->name() + " has joined the game in room " + fromInt(room + 1), false));
    json.append(field("players", players(room), true));

    int dealt = 0;
    int cards = 0;
    for (auto &&next : _players) {
      if (next->_room == player->_room && next->_state == kDealt) {
        dealt++;
        if (next->_hand.size() == rules->handSize(playing(room))) {
          cards++;
        }
      }
    }
    if (dealt > 0 && dealt == cards) {
      // already shuffled but game not started
      json.append(field("dealId", player->_sessionId, true));
      _rooms[room]._turn = player->_sessionId;
    }
    json.append(field("turn", _rooms[room]._turn, true));
    json.push_back('}');
    result = envelope("players", json);
  } else if (slot >= 0 && slot < slots(room) && _rooms[room]._slots[slot] != -1) {
    auto other = find_if(_players.begin(), _players.end(), [&](PlayerPtr &next) {
      return next->_room == room && next->_sessionId == _rooms[room]._slots[slot];
    });
    if (other != _players.end()) {
      if (rules->canRevoke() && _rooms[room]._saveState._sessionId != -1 &&
          (*other)->_sessionId == player->_sessionId && (*other)->_sessionId == _rooms[room]._saveState._sessionId) {
        // revoke the last players turn
        _rooms[room]._saveState._sessionId = -1;
        _rooms[room]._deck = _rooms[room]._saveState._deck;
        (*other)->_hand = _rooms[room]._saveState._hand;
        _rooms[room]._turn = nextTurn(room);
        result = cards((*other), room, (*other)->name() + " play revoked by " + player->_nicname);
      } else {
        if ((*other)->_sessionId == player->_sessionId) {
          // poked self
          if (player->_state == kDealt && player->_sessionId == _rooms[room]._turn) {
            _rooms[room]._turn = nextTurn(room);
            result = cards(nullptr, room, player->name() + " skipped their turn");
          } else {
            result = message(player->name() + " has " + fromInt(player->_hand.size()) + " cards");
          }
        } else {
          // poke other player
          result = message(player->name() + " poked " + (*other)->_nicname + " [" +
                           fromInt((*other)->_hand.size()) + " cards]");
        }
      }
    } else {
      result = joinError(player);
    }
  } else {
    result = joinError(player);
  }
  return response.build(result, kJoin);
}

bool Controller::nic(Message &response, PlayerPtr &player, const string &str) {
  string result;
  if (player->_state != kLurk) {
    auto other = find_if(_players.begin(), _players.end(), [&](PlayerPtr &next) {
      return next->_sessionId != player->_sessionId &&
        equal(next->_nicname.begin(), next->_nicname.end(), str.begin(), [](const char &c1, const char &c2) {
            return (c1 == c2 || toupper(c1) == toupper(c2));
          });
    });
    if (other == _players.end()) {
      string old = player->name();
      player->_nicname = replace(str, "\"", "");

      string json;
      json.push_back('{');
      json.append(field("message", old + " changed nic to: " + player->_nicname, false));
      json.append(field("players", players(player->_room), true));
      json.push_back('}');

      result = envelope("players", json);
    } else {
      result = message("nic " + str + " already exists");
    }
  } else {
    result = message("lurkers can't set nic!");
  }
  return response.build(result, kNicname);
}

bool Controller::pickup(Message &response, PlayerPtr &player, string count) {
  bool result;
  if (isTurn(player)) {
    int room = player->_room;
    Rules *rules = _rooms[room]._rules;
    Deck &deck = _rooms[room]._deck;

    if (deck.packSize()) {
      string message;
      int n;
      if (count.length()) {
        n = toInt(count);
      } else {
        n = 1;
      }
      message = "Picked: " + deck.pickup(player->_hand, n).toString();
      player->_hand.sort(rules->getRank());
      saveState(player);
      setNextTurn(room, message);
      response.broadcast(player->name() + " took " + fromInt(n) + " card from the deck");
      result = response.build(cards(player, player->_room, message), kPickup);
    } else {
      result = chat(response, player, "nothing to pickup!");
    }
  } else {
    result = chat(response, player, "wait for your turn!");
  }
  return result;
}

bool Controller::putdown(Message &response, PlayerPtr &player, const Hand &hand) {
  bool result;
  if (isTurn(player)) {
    int room = player->_room;
    Rules *rules = _rooms[room]._rules;
    Deck &deck = _rooms[room]._deck;
    int discardSize = deck.discardSize();
    if (rules->isValidPlay(deck, hand)) {
      saveState(player);
      player->_hand.remove(hand);
      deck.putdown(hand);
      if (rules->clearDiscard(hand)) {
        deck.clearDiscard();
      }
      if (rules->isWinningPlay(deck, player->_hand)) {
        result = gameover(response, player);
      } else {
        string message = player->name() + " played " + hand.toString();
        if (deck.discardSize() < discardSize) {
          message += ", discard pile now has "+ plural("card", deck.discardSize());
        }
        if (rules->setNextTurn(hand)) {
          setNextTurn(room, message);
        }
        response.broadcast(message);
        result = response.build(cards(player, player->_room, response.broadcast()), kPutDown);
      }
    } else {
      result = chat(response, player, "invalid play!");
    }
  } else {
    result = chat(response, player, "wait for your turn!");
  }

  return result;
}

bool Controller::room(Message &response, PlayerPtr &player, const string &str) {
  string result;
  int newRoom = toInt(str) - 1;
  int room = player->_room;
  Rules *rules = _rooms[room]._rules;

  if (str.length() == 0) {
    result = message(string("In room ") + fromInt(room + 1) + " playing " + rules->name());
  } else if (newRoom == room) {
    result = message("already in room " + fromInt(room + 1));
  } else if (newRoom >= 0 && newRoom < numRooms) {
    if (player->_slot > -1 && player->_slot < slots(room)) {
      _rooms[room]._slots[player->_slot] = -1;
    }
    player->_hand.clear();
    player->_room = newRoom;
    player->_slot = -1;
    player->_state = kLurk;
    player->_points = 0;
    string json;
    string message = player->name() + " entered room " + fromInt(newRoom + 1) +  ", " + _rooms[newRoom]._rules->name();
    json.push_back('{');
    json.append(field("message", message), false);
    json.append(field("players", players(newRoom), true));
    json.append(field("turn", _rooms[newRoom]._turn, true));
    json.append(field("clearHandId", player->_sessionId, true));
    json.append(field("game", _rooms[newRoom]._rules->name(), true));
    json.push_back('}');
    result = envelope("players", json);
  } else {
    result = message("invalid room ");
  }
  return response.build(result, kRoom);
}

bool Controller::shuffle(Message &response, PlayerPtr &player, const string &str) {
  string result;
  if (str.find("help") != string::npos) {
    result = message("deal");
  } else if (player->_state != kLurk) {
    int room = player->_room;
    _rooms[room]._deck.shuffle();
    for (auto &&next : _players) {
      if (next->_room == player->_room) {
        if (next->_state == kDealt) {
          next->_state = kJoined;
        }
        next->_hand.clear();
        _rooms[room]._turn = next->_sessionId;
      }
    }
    result = envelope("shuffle", "ready");
  } else {
    result = message("select your avatar!");
  }
  return response.build(result, kShuffle);
}

bool Controller::skip(Message &response, PlayerPtr &player) {
  string result;
  int room = player->_room;
  if (player->_state == kDealt && player->_sessionId == _rooms[room]._turn) {
    _rooms[room]._turn = nextTurn(room);
    string message = player->name() + " skipped their turn";
    response.broadcast(message);
    result = cards(nullptr, room, message);
  } else {
    result = message(player->name() + " says hello");
  }
  return response.build(result, kSkip);
}

#if defined(_TEST)
#include <libwebsockets.h>
#include <stdio.h>
void print(Message &message) {
  if (message._len) {
    for (int i = 0; i < message._len; i++) {
      printf("%c", message._data[i + LWS_PRE]);
    }
    printf("\n");
  }
}
int main() {
  Controller controller;
  Message message;
  int session1 = controller.createSession();
  int session2 = controller.createSession();
  int session3 = controller.createSession();

  string deal = "deal:";
  string join1 = "join:0";
  string join2 = "join:1";
  string join3 = "join:2";
  string chat = "chat:hello!";
  string nicn = "nicn:foo";
  string picu = "picu:15";
  string putd = "putd:[\"QH\"]";
  string shuf = "shuf:";
  string init = "init:";

  controller.handle(init, message, session1); print(message);
  controller.handle(join1, message, session1); print(message);
  controller.handle(join2, message, session2); print(message);
  controller.handle(join3, message, session3); print(message);
  controller.handle(nicn, message, session1); print(message);
  controller.handle(nicn, message, session2); print(message);
  controller.handle(nicn, message, session3); print(message);
  controller.handle(shuf, message, session1); print(message);
  controller.handle(deal, message, session1); print(message);
  controller.handle(deal, message, session2); print(message);
  controller.handle(deal, message, session3); print(message);
  controller.handle(chat, message, session1); print(message);
  controller.handle(picu, message, session1); print(message);
  controller.handle(putd, message, session2); print(message);
  controller.handle(picu, message, session1); print(message);
  controller.handle(putd, message, session2); print(message);

  // cleanup
  controller.destroySession(session1);
  controller.destroySession(session1);
  controller.destroySession(session1);

  Deck deck;
  Hand aces;
  aces.add(kac);
  aces.add(kas);
  deck.putdown(aces);

  Hand ace;
  Hand two;
  Hand ten;

  ace.add(kac);
  two.add(k2c);
  ten.add(kXc);

  Rules *rules = getRules(kWarlords);

  if (rules->canPlay(deck, ten)) {
    fprintf(stderr, "test failed: can play ten on ace\n");
  }
  if (rules->canPlay(deck, ace)) {
    fprintf(stderr, "test failed: can play ace on ace\n");
  }
  if (!rules->canPlay(deck, two)) {
    fprintf(stderr, "test failed: can't play two on ace\n");
  }

  aces.add(k2s);
  if (rules->canPlay(deck, ten)) {
    fprintf(stderr, "test failed: can play ten on two\n");
  }
  if (rules->canPlay(deck, ace)) {
    fprintf(stderr, "test failed: can play ace on two\n");
  }
  if (!rules->canPlay(deck, two)) {
    fprintf(stderr, "test failed: can't play two on two\n");
  }

  return 0;
}
#endif

