package com.github.a7emenov.domain.user

import cats.{Order, Show}

/** Represents a user registered to use the bot.
 *  All game session participants are represented by [[SessionParticipant]] instead.
 *
 *  @param id - registered Telegram identifier (not username).
 */
case class User(
    id: User.Id,
    permissions: Set[UserPermission]
)

object User:

  def apply(id: User.Id, role: UserRole): User =
    User(
      id,
      role.defaultPermissions
    )

  opaque type Id = String

  object Id:

    implicit val show: Show[User.Id] =
      Show.show(_.value)

    implicit val order: Order[User.Id] =
      Order.fromOrdering

    def apply(value: String): User.Id =
      value

    extension (id: User.Id)
      def value: String = id
