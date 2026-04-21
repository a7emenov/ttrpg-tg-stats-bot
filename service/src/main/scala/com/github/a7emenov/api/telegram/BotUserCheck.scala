package com.github.a7emenov.api.telegram

import cats.data.NonEmptySet
import com.bot4s.telegram.api.declarative.{Action, Messages}
import com.bot4s.telegram.models.Message
import com.bot4s.telegram.models.User as BotUser
import com.github.a7emenov.domain.user.{User, UserPermission}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.github.a7emenov.process.user.UserProcess

trait BotUserCheck[F[_]] extends Messages[F] {

  protected def userProcess: UserProcess[F]

  def withUserId(botUserId: Long)(actionF: Action[F, User.Id]): F[Unit] =
    actionF(User.Id(botUserId.toString))

  def withMessageUserId(actionF: Action[F, User.Id])(implicit msg: Message): F[Unit] =
    using(_.from) { commandRunner => withUserId(commandRunner.id)(actionF) }

  def withUser(botUser: BotUser)(actionF: Action[F, User])(
      onNotFound: F[Unit],
      onError: UserProcess.Error.Get => F[Unit]
  ): F[Unit] =
    withUserId(botUser.id) { commandRunnerId =>
      for {
        userOpt <- userProcess.get(commandRunnerId)
        result  <- userOpt match {
          case Right(Some(user)) =>
            actionF(user)

          case Right(None) =>
            onNotFound

          case Left(e) =>
            onError(e)
        }
      } yield result
    }

  def withMessageUser(actionF: Action[F, User])(implicit msg: Message): F[Unit] =
    using(_.from) { commandRunner =>
      withUser(commandRunner)(actionF)(defaultUserNotFoundReply, defaultGetUserErrorReply)
    }

  def withPermissions(botUser: BotUser, head: UserPermission, tail: UserPermission*)(actionF: Action[F, User])(
      onNotFound: F[Unit],
      onError: UserProcess.Error.Get => F[Unit],
      onUnauthorized: F[Unit]
  ): F[Unit] =
    withPermissions(botUser, NonEmptySet.of(head, tail*))(actionF)(onNotFound, onError, onUnauthorized)

  def withPermissions(botUser: BotUser, permissions: NonEmptySet[UserPermission])(actionF: Action[F, User])(
      onNotFound: F[Unit],
      onError: UserProcess.Error.Get => F[Unit],
      onUnauthorized: F[Unit]
  ): F[Unit] =
    withUser(botUser)(checkPermissions(_, permissions)(actionF)(onUnauthorized))(onNotFound, onError)

  def withMessagePermissions(head: UserPermission, tail: UserPermission*)(actionF: Action[F, User])(implicit
      msg: Message): F[Unit] =
    withMessagePermissions(NonEmptySet.of(head, tail*))(actionF)

  def withMessagePermissions(permissions: NonEmptySet[UserPermission])(actionF: Action[F, User])(implicit
      msg: Message): F[Unit] =
    withMessageUser(checkPermissions(_, permissions)(actionF)(defaultUnauthorizedReply))

  private def defaultUserNotFoundReply(implicit msg: Message): F[Unit] =
    reply("user is not registered to use the bot").void

  private def defaultGetUserErrorReply(error: UserProcess.Error.Get)(implicit msg: Message): F[Unit] =
    reply("failed to retrieve user").void

  private def defaultUnauthorizedReply(implicit msg: Message): F[Unit] =
    reply("user is not authorized for this operation").void

  private def checkPermissions(
      user: User,
      permissions: NonEmptySet[UserPermission]
  )(actionF: Action[F, User])(onUnauthorized: F[Unit]): F[Unit] =
    if permissions.forall(user.permissions.contains) then
      actionF(user)
    else
      onUnauthorized
}
