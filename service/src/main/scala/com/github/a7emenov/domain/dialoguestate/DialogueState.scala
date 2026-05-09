package com.github.a7emenov.domain.dialoguestate

import com.github.a7emenov.domain.gamesession.{GameSession, GameType, SessionParticipant}

/** Represents the current state for the user's dialogue with the chatbot.
 *  Used when several pieces of data are required for an operation.
 */
sealed trait DialogueState

object DialogueState:

  case object Empty extends DialogueState

  sealed trait RecordingNewSession extends DialogueState

  object RecordingNewSession:

    case object Timeframe
        extends DialogueState.RecordingNewSession
    final case class Name(
        timeframe: GameSession.Timeframe
    ) extends DialogueState.RecordingNewSession
    final case class AdditionalHosts(
        timeframe: GameSession.Timeframe,
        name: GameSession.GameName,

    ) extends DialogueState.RecordingNewSession

    final case class GameType(
        timeframe: GameSession.Timeframe,
        name: GameSession.GameName,
        additionalHosts: List[SessionParticipant]
    ) extends DialogueState.RecordingNewSession
