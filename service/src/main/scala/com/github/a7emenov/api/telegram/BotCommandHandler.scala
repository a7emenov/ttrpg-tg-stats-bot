package com.github.a7emenov.api.telegram

import cats.effect.Async
import cats.syntax.functor.*
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.service.user.UserService
import sttp.client4.Backend
import cats.syntax.flatMap.*
import cats.syntax.monadError.*

class BotCommandHandler[F[_]: Async](
    token: String,
    backend: Backend[F],
    userService: UserService[F]
) extends TelegramBot[F](token, backend) with Polling[F] with Commands[F] {

  onCommand(BotCommand.Echo.name) { implicit command =>
    withArgs { args =>
      reply(args.mkString(" ")).void
    }
  }

  onCommand(BotCommand.GetUser.name) { implicit command =>
    withArgs { args =>
      args.headOption match {
        case Some(userId) =>
          for {
            userOpt <- userService.get(User.Id(userId)).rethrow
            response = userOpt.fold("user not found")(_ => "user exists")
            result <- reply(response).void
          } yield result
        case None =>
          reply("provide a user id").void
      }
    }
  }
}
