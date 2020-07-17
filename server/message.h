//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#pragma once

#include <string>
#include <stdint.h>

using namespace std;

enum MessageType {
  kChat,
  kDeal,
  kExchange,
  kInit,
  kJoin,
  kNicname,
  kPickup,
  kPutDown,
  kRoom,
  kShuffle,
  kSkip,
  kZUnknown
};

struct Message {
  Message();
  Message(const Message &message) { build(message); }
  Message& operator=(const Message &message) { build(message); return *this; }
  virtual ~Message();

  const string broadcast() const { return *_broadcast; }
  void broadcast(const string &message);
  bool build(const string, const MessageType type);
  bool build(const Message &message);
  bool build(const unsigned char *data, size_t len);
  void create();
  void destroy();
  bool isBroadcast() const;

  string *_broadcast;
  unsigned char *_data;
  int _len;
  MessageType _type;
};
