package com.github.a7emenov.domain.gamesession

import com.github.a7emenov.domain.user.User

/** Represents a participant in a given game session.
 *  Participants are recorded by their username which may change over time
 *  and is stored only for display purposes.
 */
sealed trait SessionParticipant:

  /** Participant's username which should be unique within a game session.
   *  @return
   */
  def username: SessionParticipant.Username

object SessionParticipant:

  final case class WithoutId(username: SessionParticipant.Username)
      extends SessionParticipant

  final case class WithId(id: User.Id, username: SessionParticipant.Username)
      extends SessionParticipant

  opaque type Username = String

  object Username:
    def apply(value: String): SessionParticipant.Username =
      value

    extension (username: SessionParticipant.Username)
      def value: String = username
