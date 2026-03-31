package com.github.a7emenov.domain.game

import com.github.a7emenov.domain.user.User

/** Represents a participant in a given game session.
 *  Participants are recorded by their username which may change over time
 *  and is stored only for display purposes. Username
 *
 *  @param username - participant's username which should be unique within a game session.
 *  @param id - Telegram identifier corresponding to the username.
 */
case class SessionParticipant(
    username: SessionParticipant.Username,
    id: Option[User.Id]
)

object SessionParticipant:

  opaque type Username = String

  object Username:
    def apply(value: String): SessionParticipant.Username =
      value

    extension (username: SessionParticipant.Username)
      def value: String = username
