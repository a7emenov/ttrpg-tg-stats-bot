package com.github.a7emenov.domain.user

import com.github.a7emenov.domain.game.SessionParticipant
import com.github.a7emenov.domain.user.User

/** Represents a user registered to use the bot.
 *  All game session participants are represented by [[SessionParticipant]] instead.
 *
 *  @param id - registered Telegram identifier (not username).
 */
case class User(
    id: User.Id
)

object User:

  opaque type Id = String

  object Id:
    def apply(value: String): User.Id =
      value

    extension (id: User.Id)
      def value: String = id
