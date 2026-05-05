package com.github.a7emenov.api.telegram.user

import cats.MonadThrow
import cats.data.NonEmptySet
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.bot4s.telegram.api.declarative.Action
import com.github.a7emenov.domain.user.{User, UserPermission}
import com.github.a7emenov.process.user.UserProcess

trait BotUserHandlerCore[F[_]]:

  implicit val monad: MonadThrow[F]

  protected def userProcess: UserProcess[F]

  def withUserId(botUserId: Long)(actionF: Action[F, User.Id]): F[Unit] =
    actionF(User.Id(botUserId.toString))

  def withUser(botUserId: Long)(actionF: Action[F, User])(
      onNotFound: F[Unit],
      onError: UserProcess.Error.Get => F[Unit]
  ): F[Unit] =
    withUserId(botUserId) { commandRunnerId =>
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

  def withPermissions(botUserId: Long, head: UserPermission, tail: UserPermission*)(actionF: Action[F, User])(
      onNotFound: F[Unit],
      onError: UserProcess.Error.Get => F[Unit],
      onUnauthorized: F[Unit]
  ): F[Unit] =
    withPermissions(botUserId, NonEmptySet.of(head, tail*))(actionF)(onNotFound, onError, onUnauthorized)

  def withPermissions(botUserId: Long, permissions: NonEmptySet[UserPermission])(actionF: Action[F, User])(
      onNotFound: F[Unit],
      onError: UserProcess.Error.Get => F[Unit],
      onUnauthorized: F[Unit]
  ): F[Unit] =
    withUser(botUserId)(checkPermissions(_, permissions)(actionF)(onUnauthorized))(onNotFound, onError)

  private def checkPermissions(
      user: User,
      permissions: NonEmptySet[UserPermission]
  )(actionF: Action[F, User])(onUnauthorized: F[Unit]): F[Unit] =
    if permissions.forall(user.permissions.contains) then
      actionF(user)
    else
      onUnauthorized
