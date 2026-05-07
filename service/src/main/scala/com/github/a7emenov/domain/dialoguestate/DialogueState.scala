package com.github.a7emenov.domain.dialoguestate

import com.github.a7emenov.domain.gamesession.{GameSession, GameType, SessionParticipant}

/** Represents the current state for the user's dialogue with the chatbot.
 *  Used when several pieces of data are required for an operation.
 */
sealed trait DialogueState

object DialogueState:

  case object Empty extends DialogueState

  case class RecordingNewSession(
      timeframe: Option[GameSession.Timeframe],
      name: GameSession.GameName,
      additionalHosts: Option[List[SessionParticipant]],
      gameType: GameType
  ) extends DialogueState
