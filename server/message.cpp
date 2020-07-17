//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#include <libwebsockets.h>
#include "message.h"

Message::Message() :
  _broadcast(nullptr),
  _data(nullptr),
  _len(0),
  _type(kZUnknown) {
}

Message::~Message() {
  destroy();
}

void Message::broadcast(const string &message) {
  if (_broadcast == nullptr) {
    _broadcast = new string();
  }
  _broadcast->clear();
  _broadcast->append(message);
}

void Message::create() {
  _broadcast = new string();
  _data = nullptr;
  _len = 0;
}

void Message::destroy() {
  if (_broadcast != nullptr) {
    delete _broadcast;
    _broadcast = nullptr;
  }
  free(_data);
  _data = nullptr;
  _len = 0;
}

bool Message::build(const std::string data, const MessageType type) {
  _type = type;
  return this->build((const unsigned char *)data.c_str(), data.length());
}

bool Message::build(const Message &message) {
  bool result;
  if (_data == message._data && _len == message._len) {
    result = true;
  } else {
    free(_data);
    _type = message._type;
    _len = message._len;
    _data = (unsigned char *)malloc(LWS_PRE + _len);
    result = (_data != NULL);
    if (result) {
      memcpy((char *)_data, message._data, LWS_PRE + _len);
    }
  }
  return result;
}

bool Message::build(const unsigned char *data, size_t len) {
  free(_data);

  // over-allocate by LWS_PRE
  _data = (unsigned char *)malloc(LWS_PRE + len);
  _len = len;

  bool result = (_data != NULL);
  if (result) {
    memset((char *)_data, '\0', LWS_PRE);
    memcpy((char *)_data + LWS_PRE, data, len);
  }
  return result;
}

bool Message::isBroadcast() const {
  bool result;
  switch (_type) {
  case kChat:
  case kExchange:
  case kJoin:
  case kNicname:
  case kPickup:
  case kPutDown:
  case kRoom:
  case kShuffle:
  case kSkip:
    result = true;
    break;
  default:
    result = false;
  }
  return result;
}
