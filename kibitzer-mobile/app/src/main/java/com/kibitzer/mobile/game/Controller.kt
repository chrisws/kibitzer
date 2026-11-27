package com.kibitzer.mobile.game

class Controller(
  private val numRooms: Int = 1,
) {
  private val players = mutableListOf<Player>()
  val rooms: List<Room> = List(numRooms) { Room() }
  private var nextId = 1
  private var gameHandler: GameHandler? = null
  private var currentGame: Game = Game.WARLORDS

  // --- Public accessors ---

  fun findSession(sessionId: Int): Player? = players.find { it.sessionId == sessionId }

  fun getCurrentTurn(): Int = rooms[0].turn

  fun isGameOver(): Boolean {
    val room = rooms[0]
    return room.slots.any { sid ->
      sid != -1 && findSession(sid)?.hand?.size == 0
    }
  }

  fun setGame(game: Game) {
    currentGame = game
    rooms[0].rules =
      when (game) {
        Game.WARLORDS -> WarlordsRules()
        Game.FREE -> FreeRules()
        Game.CRAZY_EIGHTS -> CrazyEightsRules()
        Game.CHEAT -> CheatRules()
        Game.OLD_MAID -> OldMaidRules()
        Game.GO_FISH -> GoFishRules()
      }
    rooms[0].cheatState = CheatState()
    rooms[0].activeSuit = null
    gameHandler =
      when (game) {
        Game.OLD_MAID -> OldMaidHandler()
        Game.GO_FISH -> GoFishHandler()
        else -> null
      }
  }

  fun getCurrentGame(): Game = currentGame

  // --- Session management ---

  fun createSession(): Int {
    val id = nextId++
    players.add(Player(id))
    return id
  }

  fun destroySession(sessionId: Int): Message? {
    val player = findSession(sessionId) ?: return null
    val room = rooms[player.room]
    if (player.slot in room.slots.indices) {
      room.slots[player.slot] = -1
    }
    val name = player.name()
    val playersJson = playersJson(player.room)
    players.remove(player)
    val json = """{"id":$sessionId,"players":$playersJson,"name":"$name"}"""
    return Message("exit", json)
  }

  // --- Command handling ---

  fun handle(
    command: String,
    sessionId: Int,
  ): Message {
    val player =
      findSession(sessionId)
        ?: return Message("message", "unknown session")
    val prefix = command.take(5)
    val payload = command.drop(5)

    return when (Command.fromPrefix(prefix)) {
      Command.INIT -> init(player)
      Command.JOIN -> join(player, payload)
      Command.NICKNAME -> nic(player, payload)
      Command.GAME -> setGameCommand(player, payload)
      Command.SHUFFLE -> shuffle(player)
      Command.DEAL -> deal(player)
      Command.PICKUP -> pickup(player, payload)
      Command.PUTDOWN -> putdown(player, payload)
      Command.EXCHANGE -> exchange(player, payload)
      Command.BID -> bid(player, payload)
      Command.CHEAT -> cheat(player, payload)
      Command.ASK -> ask(player, payload)
      Command.SKIP -> skip(player)
      Command.CHAT -> chat(player, payload)
      Command.ROOM -> Message("message", "rooms not supported in v1")
      null -> Message("message", "unknown command: $prefix")
    }
  }

  // --- Redaction ---

  fun redact(
    sessionId: Int,
    message: Message,
  ): Message {
    if (message.id == "cards") {
      val player = findSession(sessionId) ?: return message
      val handJson = player.hand.toJson()
      val data = message.data as? String ?: return message
      // Replace the hand field in the JSON string
      val redacted = data.replace(Regex("\"hand\":\\[.*?\\]"), "\"hand\":$handJson")
      return Message("cards", redacted)
    }
    return message
  }

  // --- Internal command handlers ---

  private fun init(player: Player): Message {
    val json = """{"welcome":"Kibitzer Mobile","sessionId":${player.sessionId},"players":${playersJson(
      player.room,
    )}}"""
    return Message("init", json)
  }

  private fun join(
    player: Player,
    slotStr: String,
  ): Message {
    val slot = slotStr.trim().toIntOrNull() ?: return joinError(player)
    val room = rooms[player.room]

    if (player.state == PlayerState.LURK && slot in room.slots.indices && room.slots[slot] == -1) {
      room.slots[slot] = player.sessionId
      player.state = PlayerState.JOINED
      player.slot = slot

      val dealt = players.count { it.room == player.room && it.state == PlayerState.DEALT }
      val cards =
        players.count {
          it.room == player.room &&
            it.state == PlayerState.DEALT &&
            it.hand.size == room.rules.handSize(playing(player.room))
        }

      val dealId = if (dealt > 0 && dealt == cards) player.sessionId else -1
      if (dealId != -1) {
        room.turn = player.sessionId
      }

      val json = """{"message":"${player.name()} has joined the game in room ${player.room + 1}","players":${playersJson(
        player.room,
      )},"turn":${room.turn}}"""
      return Message("players", json)
    } else if (slot in room.slots.indices && room.slots[slot] != -1) {
      val other = findSession(room.slots[slot])
      if (other != null) {
        if (other.sessionId == player.sessionId) {
          if (player.state == PlayerState.DEALT && player.sessionId == room.turn) {
            room.turn = nextTurn(player.room)
            return cardsMessage(null, player.room, "${player.name()} skipped their turn")
          } else {
            return Message("message", "${player.name()} has ${player.hand.size} cards")
          }
        } else {
          return Message("message", "${player.name()} poked ${other.nickname} [${other.hand.size} cards]")
        }
      }
    }
    return joinError(player)
  }

  private fun joinError(player: Player): Message {
    val json = """{"message":"${player.name()} poked nobody","players":${playersJson(player.room)}}"""
    return Message("players", json)
  }

  private fun nic(
    player: Player,
    name: String,
  ): Message {
    if (player.state == PlayerState.LURK) {
      return Message("message", "lurkers can't set nic!")
    }
    val cleanName = name.replace("\"", "")
    val duplicate =
      players.find {
        it.sessionId != player.sessionId &&
          it.nickname.equals(cleanName, ignoreCase = true)
      }
    if (duplicate != null) {
      return Message("message", "nic $cleanName already exists")
    }
    val old = player.name()
    player.nickname = cleanName
    val json = """{"message":"$old changed nic to: ${player.nickname}","players":${playersJson(player.room)}}"""
    return Message("players", json)
  }

  private fun setGameCommand(
    player: Player,
    name: String,
  ): Message {
    val game =
      Game.fromName(name)
        ?: return Message("message", "unknown game: $name")
    setGame(game)
    return Message("players", """{"game":"${game.name}"}""")
  }

  private fun shuffle(player: Player): Message {
    if (player.state == PlayerState.LURK) {
      return Message("message", "select your avatar!")
    }
    val room = rooms[player.room]
    room.deck.shuffle()
    for (p in players) {
      if (p.room == player.room) {
        if (p.state == PlayerState.DEALT) {
          p.state = PlayerState.JOINED
        }
        p.hand.clear()
        room.turn = p.sessionId
      }
    }
    return Message("shuffle", "ready")
  }

  private fun deal(player: Player): Message {
    val room = rooms[player.room]
    val activePlayers =
      room.slots
        .mapNotNull { sid -> sid.takeIf { it != -1 } }
        .mapNotNull { sid -> findSession(sid) }
        .filter { it.state == PlayerState.JOINED }

    if (activePlayers.isEmpty()) {
      return Message("message", "no players to deal")
    }

    val numPlayers = activePlayers.size
    val cardsPerPlayer = room.rules.handSize(numPlayers)

    if (room.rules is OldMaidRules) {
      room.deck.shuffle()
      // Deal all 49 cards out
      var idx = 0
      while (room.deck.packSize > 0) {
        val p = activePlayers[idx % numPlayers]
        p.hand.add(room.deck.pack.pop())
        idx++
      }
      // Discard initial pairs
      activePlayers.forEach { p ->
        val pairs = findPairsInHand(p.hand)
        pairs.forEach { (a, b) ->
          val pairHand =
            Hand().apply {
              add(a)
              add(b)
            }
          p.hand.remove(pairHand)
        }
      }
    } else {
      room.deck.shuffle()
      activePlayers.forEach { p ->
        room.deck.deal(p.hand, cardsPerPlayer)
        p.hand.sort(room.rules.getRank())
      }
    }

    room.turn = activePlayers.first().sessionId
    activePlayers.forEach { it.state = PlayerState.DEALT }

    return cardsMessage(player, player.room, "cards dealt")
  }

  private fun pickup(
    player: Player,
    count: String,
  ): Message {
    if (!isTurn(player)) {
      return Message("message", "wait for your turn!")
    }
    val room = rooms[player.room]
    val rules = room.rules
    val deck = room.deck

    if (deck.packSize > 0) {
      val n = count.trim().toIntOrNull() ?: 1
      val message = StringBuilder("Picked: ${deck.pickup(player.hand, n)}")
      player.hand.sort(rules.getRank())
      room.saveState(player)
      setNextTurn(player.room, message)
      return cardsMessage(player, player.room, message.toString())
    } else if (deck.discardSize > 0) {
      val n = count.trim().toIntOrNull() ?: 1
      val available = minOf(n, deck.discardSize)
      val picked = deck.pickupFromDiscard(player.hand, available)
      val message = StringBuilder("Picked from discard: $picked")
      player.hand.sort(rules.getRank())
      room.saveState(player)
      setNextTurn(player.room, message)
      return cardsMessage(player, player.room, message.toString())
    }
    return Message("message", "nothing to pickup!")
  }

  private fun putdown(
    player: Player,
    handJson: String,
  ): Message {
    android.util.Log.d(
      "Play",
      "Controller.putdown: player=${player.name()}(id=${player.sessionId}), hand=$handJson, turn=${rooms[player.room].turn}",
    )
    if (!isTurn(player)) {
      android.util.Log.d(
        "Play",
        "Controller.putdown: REJECTED - not player's turn (turn=${rooms[player.room].turn}, player=${player.sessionId})",
      )
      return Message("message", "wait for your turn!")
    }
    val room = rooms[player.room]
    val rules = room.rules
    val deck = room.deck
    val hand = Hand.fromJson(handJson)
    android.util.Log.d(
      "Play",
      "Controller.putdown: hand parsed=$hand, deck.lastPlay=${deck.lastPlay}, deck.lastSize=${deck.lastSize}, deck.discardSize=${deck.discardSize}",
    )

    // Delegate to game handler if present
    gameHandler?.let { handler ->
      return handler.handlePutdown(this, player, hand)
    }

    val valid = rules.isValidPlay(deck, hand)
    android.util.Log.d("Play", "Controller.putdown: isValidPlay=$valid")
    if (valid) {
      room.saveState(player)
      player.hand.remove(hand)
      deck.putdown(hand)
      if (rules.clearDiscard(hand)) {
        deck.clearDiscard()
      }
      if (rules.isWinningPlay(deck, player.hand)) {
        return gameover(player)
      }
      val message = StringBuilder("${player.name()} played $hand")
      if (deck.discardSize < hand.size) {
        message.append(
          ", discard pile now has ${deck.discardSize} card${if (deck.discardSize != 1) "s" else ""}",
        )
      }
      if (rules.setNextTurn(hand)) {
        setNextTurn(player.room, message)
      }
      return cardsMessage(player, player.room, message.toString())
    }
    return Message("message", "${player.name()} cannot play")
  }

  private fun exchange(
    player: Player,
    data: String,
  ): Message {
    val parts = data.split(":")
    if (parts.size < 3) {
      return Message("message", "invalid exchange")
    }
    val command = parts[0]
    val toId = parts[1].toIntOrNull() ?: return Message("message", "invalid target")
    val handJson = parts.drop(2).joinToString(":")
    val toGive = Hand.fromJson(handJson)
    val fromPlayer =
      findSession(toId)
        ?: return Message("message", "other player has left")

    if (command == "Q") {
      val json = """{"fromId":${player.sessionId},"from":"${player.name()}","toId":$toId,"hand":"$handJson","message":"${player.name()} offering $toGive to ${fromPlayer.name()}"}"""
      return Message("exchange", json)
    } else if (fromPlayer.hand.has(toGive)) {
      val rules = rooms[player.room].rules
      fromPlayer.hand.remove(toGive)
      player.hand.addAll(toGive)
      player.hand.sort(rules.getRank())
      return cardsMessage(player, player.room, "${player.name()} took $toGive from ${fromPlayer.name()}")
    }
    return Message("message", "${fromPlayer.name()} no longer has $toGive to give")
  }

  private fun bid(
    player: Player,
    count: String,
  ): Message {
    val room = rooms[player.room]
    val n =
      count.trim().toIntOrNull()
        ?: return Message("message", "invalid bid")
    if (n < 0 || n > player.hand.size) {
      return Message("message", "invalid bid")
    }
    room.cheatState = room.cheatState.withBid(player.sessionId, n)
    return Message("message", "${player.name()} bid $n (rank ${room.cheatState.currentRank})")
  }

  private fun cheat(
    player: Player,
    targetId: String,
  ): Message {
    val room = rooms[player.room]
    if (!room.cheatState.called) {
      room.cheatState = room.cheatState.withCalled(player.sessionId)
      val bidder = findSession(room.cheatState.bidSessionId)
      val valid = room.cheatState.isBidValid(room.cheatState.playedCards)
      val loser = if (valid) player else (bidder ?: player)
      room.deck.pickup(loser.hand, room.deck.discardSize)
      room.cheatState = room.cheatState.nextTurn()
      setNextTurn(player.room, StringBuilder())
      return Message("message", "cheat resolved — ${loser.name()} picks up")
    }
    return Message("message", "already called")
  }

  private fun ask(
    player: Player,
    data: String,
  ): Message {
    val handler =
      gameHandler as? GoFishHandler
        ?: return Message("message", "ask not supported in this game")
    return handler.handleAsk(this, player, data)
  }

  private fun skip(player: Player): Message {
    val room = rooms[player.room]
    if (player.state == PlayerState.DEALT && player.sessionId == room.turn) {
      room.turn = nextTurn(player.room)
      val message = "${player.name()} skipped their turn"
      return cardsMessage(null, player.room, message)
    }
    return Message("message", "${player.name()} says hello")
  }

  private fun chat(
    player: Player,
    text: String,
  ): Message = Message("message", "${player.name()} $text")

  private fun gameover(player: Player): Message {
    val room = rooms[player.room]
    val message = StringBuilder("${player.name()} won the round! Score:")
    player.points++
    room.turn = -1

    for (p in players) {
      if (p.room == player.room) {
        if (p.state == PlayerState.DEALT) {
          p.state = PlayerState.JOINED
        }
        p.hand.clear()
        message
          .append("<br/>")
          .append(p.name())
          .append(": ")
          .append(p.points)
      }
    }
    return cardsMessage(player, player.room, message.toString())
  }

  // --- Helpers ---

  private fun nextTurn(room: Int): Int {
    val roomObj = rooms[room]
    val dealtPlayers = players.filter { it.room == room && it.state == PlayerState.DEALT }
    if (dealtPlayers.isEmpty()) return -1

    val currentIdx = dealtPlayers.indexOfFirst { it.sessionId == roomObj.turn }
    val nextIdx = if (currentIdx == -1) 0 else (currentIdx + 1) % dealtPlayers.size
    return dealtPlayers[nextIdx].sessionId
  }

  private fun playing(room: Int): Int {
    val roomObj = rooms[room]
    return roomObj.slots.count { sid ->
      sid != -1 && findSession(sid) != null
    }
  }

  private fun playersJson(room: Int): String {
    val roomObj = rooms[room]
    val parts =
      roomObj.slots.map { sid ->
        if (sid == -1) {
          """{"sessionId":-1,"active":false,"nic":"empty"}"""
        } else {
          findSession(sid)?.toJson()
            ?: run {
              roomObj.slots[roomObj.slots.indexOf(sid)] = -1
              """{"sessionId":-1,"active":false,"nic":"empty"}"""
            }
        }
      }
    return parts.joinToString(", ", prefix = "[", postfix = "]")
  }

  private fun cardsMessage(
    player: Player?,
    room: Int,
    message: String,
  ): Message {
    val roomObj = rooms[room]
    val sb = StringBuilder("{")
    sb.append("\"pile\":").append(roomObj.deck.getDiscardJson())
    sb.append(",\"message\":\"").append(message).append("\"")
    sb.append(",\"turn\":").append(roomObj.turn)
    sb.append(",\"faceDown\":").append(roomObj.rules.faceDown())
    sb.append(",\"game\":\"").append(roomObj.rules.name()).append("\"")
    if (player != null) {
      sb.append(",\"hand\":").append(player.hand.toJson())
      sb.append(",\"sessionId\":").append(player.sessionId)
    }
    sb.append("}")
    return Message("cards", sb.toString())
  }

  private fun setNextTurn(
    room: Int,
    message: StringBuilder,
  ) {
    val roomObj = rooms[room]
    roomObj.turn = nextTurn(room)
  }

  private fun isTurn(player: Player): Boolean =
    rooms[player.room].turn == -1 || player.sessionId == rooms[player.room].turn

  private fun findPairsInHand(hand: Hand): List<Pair<Card, Card>> {
    val pairs = mutableListOf<Pair<Card, Card>>()
    val seen = mutableMapOf<Int, Card>()
    for (card in hand.toList()) {
      val rank = rank2A(card)
      val prev = seen[rank]
      if (prev != null) {
        pairs.add(prev to card)
        seen.remove(rank)
      } else {
        seen[rank] = card
      }
    }
    return pairs
  }
}
