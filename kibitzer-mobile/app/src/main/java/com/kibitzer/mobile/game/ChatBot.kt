package com.kibitzer.mobile.game

import kotlin.random.Random

class ChatBot(
  private val rng: Random = Random.Default,
) {
  // --- State ---
  private val memory = ArrayDeque<Pair<String, String>>() // (text, emotion)
  private val moodLog = ArrayDeque<String>()
  private var exchangeCount = 0
  private var lastInput = ""
  private var repeatCount = 0
  private var lastResponseType = "question"

  companion object {
    private const val MAX_MEMORY = 10
    private const val MAX_MOOD = 20

    private val REFLECTIONS =
      mapOf(
        "i" to "you",
        "me" to "you",
        "my" to "your",
        "am" to "are",
        "was" to "were",
        "you" to "me",
        "your" to "my",
        "mine" to "yours",
        "myself" to "yourself",
        "yourself" to "myself",
        "are" to "am",
        "were" to "was",
        "we" to "you",
      )

    private val POSITIVE =
      listOf(
        "happy",
        "excited",
        "good",
        "love",
        "joy",
        "hope",
        "great",
        "awesome",
        "nice",
        "cool",
        "win",
        "won",
        "perfect",
        "amazing",
        "proud",
        "glad",
        "pleased",
        "satisfied",
        "delighted",
        "lucky",
      )

    private val NEGATIVE =
      listOf(
        "sad",
        "angry",
        "upset",
        "hate",
        "tired",
        "frustrated",
        "lose",
        "lost",
        "stupid",
        "awful",
        "terrible",
        "worst",
        "unlucky",
        "annoyed",
        "bored",
        "confused",
        "stressed",
        "disappointed",
        "hurt",
        "overwhelmed",
        "frustrating",
      )

    private val INTENSITY =
      listOf(
        "very",
        "extremely",
        "so",
        "really",
        "incredibly",
        "super",
        "absolutely",
        "totally",
      )

    private val GAME_TOPICS =
      mapOf(
        "strategy" to listOf("strategy", "plan", "tactic", "bluff", "trick", "move", "play", "best"),
        "luck" to listOf("luck", "lucky", "unlucky", "chance", "random", "shuffle", "deal", "odds"),
        "winning" to listOf("win", "won", "winner", "beat", "defeat", "champion", "score", "points"),
        "losing" to listOf("lose", "lost", "loser", "behind", "bad hand", "unlucky", "suck"),
        "cards" to listOf("card", "cards", "hand", "deck", "pile", "discard", "draw", "pick"),
        "opponent" to listOf("opponent", "ai", "computer", "you", "rival", "enemy", "foe"),
      )

    private val RESPONSES =
      listOf(
        "Tell me more about that.",
        "Interesting move. What were you thinking?",
        "That's a good observation.",
        "How do you feel about your hand right now?",
        "What's your strategy for this round?",
        "That's an interesting perspective on the game.",
        "Can you explain your reasoning there?",
        "What made you choose that play?",
        "How confident are you in that call?",
        "That's a bold strategy. Are you feeling lucky?",
      )

    private val EMPATHETIC =
      listOf(
        "That sounds frustrating. Let's see what we can do.",
        "I can tell that's bothering you.",
        "It's tough when the cards don't go your way.",
        "That must have been a disappointing hand.",
        "I'm sorry about that. What's your next move?",
      )

    private val SUPPORTIVE =
      listOf(
        "Nice play! You're on a roll.",
        "That was a smart move.",
        "You're playing well. Keep it up!",
        "Great thinking. That could really pay off.",
        "I like the confidence in that play.",
      )

    private val CURIOUS =
      listOf(
        "What made you go with that approach?",
        "I wonder what would happen if you tried something different?",
        "That's an interesting angle. How did you see it?",
        "What are you looking for in this hand?",
        "I'm curious about your reasoning there.",
      )

    private val INSIGHTS =
      listOf(
        "Sometimes the best move is the one you least expect.",
        "In cards, patience is often more valuable than aggression.",
        "Every hand teaches something, even the bad ones.",
        "The shuffle doesn't care about your plans. Adapt!",
        "A good player reads the game, not just their cards.",
        "Luck changes, but skill is constant.",
        "Sometimes folding is the strongest play.",
        "The pile tells a story if you know how to read it.",
      )

    private val QUESTIONS =
      listOf(
        "How do you feel about your current hand?",
        "What's your plan for the next few turns?",
        "Is there a card you're hoping to draw?",
        "How risky do you feel your current strategy is?",
        "What would change your approach right now?",
      )
  }

  fun greet(): String =
    "I'm your game companion! Ask me anything about the game, your strategy, or just chat. What's on your mind?"

  fun respond(input: String): String {
    val text = input.lowercase().trim()
    if (text.isEmpty()) return "Say something! I'm all ears."

    // Track repetition
    repeatCount = if (text == lastInput) repeatCount + 1 else 0
    lastInput = text

    // Update state
    updateMarkovNotUsed() // placeholder for future
    val emotion = detectEmotion(text)
    moodLog.addLast(emotion)
    if (moodLog.size > MAX_MOOD) moodLog.removeFirst()

    memory.addLast(text to emotion)
    if (memory.size > MAX_MEMORY) memory.removeFirst()
    exchangeCount++

    val mood = currentMood()
    val depth = depthLevel()
    val topics = detectTopics(text)

    // --- Pattern matching (adapted from ELIZA) ---
    val response =
      when {
        text == "bye" || text == "goodbye" || text == "exit" ->
          "Good game! Remember: the next hand is always a fresh start."

        text == "help" ->
          "Ask me about your strategy, your hand, luck, or just chat. I'm here to keep things fun!"

        text == "restart" -> {
          memory.clear()
          moodLog.clear()
          exchangeCount = 0
          "Fresh start! What are you feeling about this hand?"
        }

        text.contains("i feel") ->
          "Why do you feel ${reflect(text.substringAfter("i feel").trim())}?"

        text.contains("i am") ->
          "Being ${reflect(text.substringAfter("i am").trim())} - how long have you felt that way?"

        text.contains("i think") ->
          "What leads you to think ${reflect(text.substringAfter("i think").trim())}?"

        text.contains("i want") ->
          "What would having ${reflect(text.substringAfter("i want").trim())} mean for this game?"

        text.contains("i need") ->
          "How important is it to get ${reflect(text.substringAfter("i need").trim())} right now?"

        text.contains("because") ->
          if (rng.nextFloat() < 0.5) {
            "Is that the whole reason, or is there more to it?"
          } else {
            "How sure are you about that?"
          }

        text.contains("always") || text.contains("never") ->
          "Are there any exceptions to that pattern you've noticed?"

        text.contains("should") ->
          "Where does that expectation come from? The rules, or your gut?"

        text.contains("everyone") || text.contains("nobody") ->
          "Are you sure that applies without exception?"

        text.contains("hate") || text.contains("can't stand") ->
          "Strong feelings! What's underneath that reaction in this game?"

        text.contains("love") || text.contains("adore") ->
          "What makes that so appealing to you?"

        text.contains("confused") || text.contains("uncertain") || text.contains("unsure") ->
          "Confusion is where good strategy begins. What part feels unclear?"

        text.contains("remember") || text.contains("memory") ->
          "How does that earlier play affect your current strategy?"

        text.contains("afraid") || text.contains("fear") || text.contains("scared") ->
          "What are you most worried about losing here?"

        text.contains("regret") || text.contains("mistake") ->
          "We all make bad calls. How does it change your next move?"

        text.contains("happy") || text.contains("joy") || text.contains("wonderful") ->
          "That positive energy is great! What's going well for you?"

        text.contains("tired") || text.contains("exhausted") ->
          "Sounds draining. Is it the game or the stakes getting to you?"

        text.contains("angry") || text.contains("mad") || text.contains("upset") ->
          "Frustration is normal. What boundary did the game cross?"

        text.contains("why can't i") ->
          "What do you think is stopping you from ${reflect(text.substringAfter("why can't i").trim())}?"

        text.contains("i don't know") && text.length < 15 ->
          "That's okay. Sometimes the best move emerges when you stop overthinking."

        text.contains("sorry") ->
          "No apologies needed. We're just playing cards."

        text.contains("thank") ->
          "Anytime! I'm here to keep the game fun."

        text.contains("who are you") || text.contains("tell me about yourself") ->
          "I'm your AI game companion. I watch the game, chat with you, and try to keep things interesting. What do you want to talk about?"

        repeatCount > 1 ->
          "You've mentioned that a few times. It seems important. What's really on your mind?"

        text.length < 8 ->
          "Give me a bit more to work with! What are you thinking?"

        else ->
          adaptiveResponse(text, mood, topics, depth)
      }

    lastResponseType = if (response.endsWith("?")) "question" else "statement"
    return response
  }

  // --- Adaptive response selection ---
  private fun adaptiveResponse(
    text: String,
    mood: String,
    topics: List<String>,
    depth: String,
  ): String {
    val r = rng.nextFloat()

    val insightProb =
      when (depth) {
        "surface" -> 0.1f
        "moderate" -> 0.15f
        else -> 0.2f
      }
    val recallProb =
      when (depth) {
        "surface" -> 0.05f
        "moderate" -> 0.1f
        else -> 0.15f
      }

    return when {
      r < insightProb -> {
        if (topics.isNotEmpty()) {
          topicInsight(topics[rng.nextInt(topics.size)])
        } else {
          INSIGHTS[rng.nextInt(INSIGHTS.size)]
        }
      }
      r < insightProb + recallProb && memory.size > 2 -> {
        val old = memory.first().first
        "Earlier you said \"$old\" - does that still influence your play?"
      }
      mood == "negative" && r < 0.5f ->
        EMPATHETIC[rng.nextInt(EMPATHETIC.size)]
      mood == "positive" && r < 0.5f ->
        SUPPORTIVE[rng.nextInt(SUPPORTIVE.size)]
      topics.isNotEmpty() && r < 0.6f ->
        topicQuestion(topics[rng.nextInt(topics.size)])
      lastResponseType == "question" && r < 0.7f ->
        CURIOUS[rng.nextInt(CURIOUS.size)]
      else ->
        RESPONSES[rng.nextInt(RESPONSES.size)]
    }
  }

  // --- Emotion detection ---
  private fun detectEmotion(text: String): String {
    val intensity = if (INTENSITY.any { text.contains(it) }) 2 else 1
    if (POSITIVE.any { text.contains(it) }) return "positive$intensity"
    if (NEGATIVE.any { text.contains(it) }) return "negative$intensity"
    return "neutral"
  }

  // --- Current mood (weighted recent) ---
  private fun currentMood(): String {
    if (moodLog.size < 2) return "neutral"
    var pos = 0
    var neg = 0
    val recent = moodLog.takeLast(5)
    recent.reversed().forEachIndexed { i, e ->
      val w = 5 - i
      if (e.startsWith("positive")) pos += w + (if (e.endsWith("2")) 1 else 0)
      if (e.startsWith("negative")) neg += w + (if (e.endsWith("2")) 1 else 0)
    }
    return when {
      pos > neg + 2 -> "positive"
      neg > pos + 2 -> "negative"
      else -> "neutral"
    }
  }

  // --- Topic detection ---
  private fun detectTopics(text: String): List<String> {
    val found = mutableListOf<String>()
    for ((topic, keywords) in GAME_TOPICS) {
      if (keywords.any { text.contains(it) }) {
        found.add(topic)
      }
    }
    return found
  }

  // --- Conversation depth ---
  private fun depthLevel(): String =
    when {
      exchangeCount < 5 -> "surface"
      exchangeCount < 15 -> "moderate"
      else -> "deep"
    }

  // --- Pronoun reflection ---
  private fun reflect(text: String): String =
    text
      .split(" ")
      .joinToString(" ") { w -> REFLECTIONS[w] ?: w }
      .trim()

  // --- Topic-specific insight ---
  private fun topicInsight(topic: String): String =
    when (topic) {
      "strategy" -> "A good strategy adapts. What would you do differently with a fresh hand?"
      "luck" -> "Luck is the equalizer in cards. No strategy beats a truly random shuffle."
      "winning" -> "Winning feels great, but the best players enjoy the process too."
      "losing" -> "Every loss is data. What would you play differently next time?"
      "cards" -> "Your hand is what you're dealt. Your play is what you make of it."
      "opponent" -> "Reading your opponent is half the game. What patterns do you see?"
      else -> INSIGHTS[rng.nextInt(INSIGHTS.size)]
    }

  // --- Topic-specific question ---
  private fun topicQuestion(topic: String): String =
    when (topic) {
      "strategy" -> "What's your plan for the next few moves?"
      "luck" -> "Do you feel lucky or unlucky right now?"
      "winning" -> "What would a perfect hand look like for you right now?"
      "losing" -> "Is there a comeback play you're seeing?"
      "cards" -> "Is there a specific card you're hoping to draw?"
      "opponent" -> "How do you read my current strategy?"
      else -> QUESTIONS[rng.nextInt(QUESTIONS.size)]
    }

  private fun updateMarkovNotUsed() { /* reserved for future Markov chain */ }
}
