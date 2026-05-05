package com.github.a7emenov.api.telegram.user

import cats.data.NonEmptySet
import cats.syntax.functor.*
import cats.syntax.option.*
import com.bot4s.telegram.api.declarative.{Action, Callbacks}
import com.bot4s.telegram.models.CallbackQuery
import com.github.a7emenov.domain.user.{User, UserPermission}
import com.github.a7emenov.process.user.UserProcess

trait BotCallbackUserHandler[F[_]] extends BotUserHandlerCore[F] with Callbacks[F] {

  def withCallbackUserId(actionF: Action[F, User.Id])(implicit cbq: CallbackQuery): F[Unit] =
    withUserId(cbq.from.id)(actionF)

  def withCallbackUserUser(actionF: Action[F, User])(implicit cbq: CallbackQuery): F[Unit] =
    withUser(cbq.from.id)(actionF)(defaultUserNotFoundAck, defaultGetUserErrorAck)

  def withCallbackPermissions(head: UserPermission, tail: UserPermission*)(actionF: Action[F, User])(implicit
      cbq: CallbackQuery): F[Unit] =
    withCallbackPermissions(NonEmptySet.of(head, tail*))(actionF)

  def withCallbackPermissions(permissions: NonEmptySet[UserPermission])(actionF: Action[F, User])(implicit
      cbq: CallbackQuery): F[Unit] =
    withPermissions(cbq.from.id, permissions)(actionF)(
      onNotFound = defaultUserNotFoundAck,
      onError = defaultGetUserErrorAck,
      onUnauthorized = defaultUnauthorizedAck
    )

  private def defaultUserNotFoundAck(implicit cbq: CallbackQuery): F[Unit] =
    ackCallback(text = "user is not registered to use the bot".some).void

  private def defaultGetUserErrorAck(error: UserProcess.Error.Get)(implicit cbq: CallbackQuery): F[Unit] =
    ackCallback(text = "failed to retrieve user".some).void

  private def defaultUnauthorizedAck(implicit cbq: CallbackQuery): F[Unit] =
    ackCallback(text = "user is not authorized for this operation".some).void
}
