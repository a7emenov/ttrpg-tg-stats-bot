package com.github.a7emenov.api.telegram.user

import cats.data.NonEmptySet
import cats.syntax.functor.*
import com.bot4s.telegram.api.declarative.{Action, Messages}
import com.bot4s.telegram.models.Message
import com.github.a7emenov.domain.user.{User, UserPermission}
import com.github.a7emenov.process.user.UserProcess

trait BotMessageUserHandler[F[_]] extends Messages[F] with BotUserHandlerCore[F] {

  def withMessageUserId(actionF: Action[F, User.Id])(implicit msg: Message): F[Unit] =
    using(_.from) { messageSender => withUserId(messageSender.id)(actionF) }

  def withMessageUser(actionF: Action[F, User])(implicit msg: Message): F[Unit] =
    using(_.from) { messageSender =>
      withUser(messageSender.id)(actionF)(defaultUserNotFoundReply, defaultGetUserErrorReply)
    }

  def withMessagePermissions(head: UserPermission, tail: UserPermission*)(actionF: Action[F, User])(implicit
      msg: Message): F[Unit] =
    withMessagePermissions(NonEmptySet.of(head, tail*))(actionF)

  def withMessagePermissions(permissions: NonEmptySet[UserPermission])(actionF: Action[F, User])(implicit
      msg: Message): F[Unit] =
    using(_.from) { messageSender =>
      withPermissions(messageSender.id, permissions)(actionF)(
        onNotFound = defaultUserNotFoundReply,
        onError = defaultGetUserErrorReply,
        onUnauthorized = defaultUnauthorizedReply
      )
    }

  private def defaultUserNotFoundReply(implicit msg: Message): F[Unit] =
    reply("user is not registered to use the bot").void

  private def defaultGetUserErrorReply(error: UserProcess.Error.Get)(implicit msg: Message): F[Unit] =
    reply("failed to retrieve user").void

  private def defaultUnauthorizedReply(implicit msg: Message): F[Unit] =
    reply("user is not authorized for this operation").void
}
