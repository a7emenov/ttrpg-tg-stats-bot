package com.github.a7emenov.api.telegram

import cats.data.NonEmptySet
import com.bot4s.telegram.api.declarative.{Action, Messages}
import com.bot4s.telegram.models.Message
import com.github.a7emenov.domain.user.{User, UserPermission}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.github.a7emenov.process.user.UserProcess

trait BotUserCheck[F[_]] extends Messages[F] {

  protected def userProcess: UserProcess[F]

  def withCommandUserId(actionF: Action[F, User.Id])(implicit msg: Message): F[Unit] =
    using(_.from) { commandRunner =>
      actionF(User.Id(commandRunner.id.toString))
    }

  def withUser(actionF: Action[F, User])(implicit msg: Message): F[Unit] =
    withCommandUserId { commandRunnerId =>
      for {
        userOpt <- userProcess.get(commandRunnerId)
        result  <- userOpt match {
          case Right(Some(user)) =>
            actionF(user)

          case Right(None) =>
            reply("user is not registered to use the bot").void

          case Left(_) =>
            reply("failed to retrieve user").void
        }
      } yield result

    }

  def withPermissions(head: UserPermission, tail: UserPermission*)(actionF: Action[F, User])(implicit
      msg: Message): F[Unit] =
    withPermissions(NonEmptySet.of(head, tail*))(actionF)

  def withPermissions(permissions: NonEmptySet[UserPermission])(actionF: Action[F, User])(implicit
      msg: Message): F[Unit] =
    withUser { user =>
      if permissions.forall(user.permissions.contains) then
        actionF(user)
      else
        reply("user is not authorized for this operation").void
    }

}
