package com.github.a7emenov.api.telegram

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.functor.*
import cats.syntax.show.*
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.github.a7emenov.domain.user.User
import sttp.client4.Backend
import cats.syntax.flatMap.*
import com.github.a7emenov.process.user.UserProcess

class BotCommandHandler[F[_]: Async](
    token: String,
    backend: Backend[F],
    userProcess: UserProcess[F]
) extends TelegramBot[F](token, backend) with Polling[F] with Commands[F] {

  onCommand(BotCommand.Echo.name) { implicit command =>
    withArgs { args =>
      reply(args.mkString(" ")).void
    }
  }

  onCommand(BotCommand.SetupAdmin.name) { implicit command =>
    withArgs { args =>
      val resultF = for {
        setupToken <- EitherT.fromOption(args.headOption.map(UserProcess.SetupToken(_)), "provide a setup token")
        userId     <-
          EitherT.fromOption(command.from.map(user => User.Id(user.id.toString)), "command must be run by a user")
        result <- EitherT(userProcess.setupAdmin(userId, setupToken))
          .leftMap {
            case UserProcess.Error.SetupAdmin.WrongToken => "setup token is incorrect"
            case UserProcess.Error.SetupAdmin.AlreadyExists => "admin already exists"
            case UserProcess.Error.SetupAdmin.System(message, _) => show"unknown error: $message"
          }
      } yield result

      resultF
        .fold(identity, _ => "admin setup successfully")
        .flatMap(reply(_).void)
    }
  }

  onCommand(BotCommand.CheckPermissions.name) { implicit command =>
    val resultF = for {
      userId <-
        EitherT.fromOption(command.from.map(user => User.Id(user.id.toString)), "command must be run by a user")
      userOpt <- EitherT(userProcess.get(userId))
        .leftMap {
          case UserProcess.Error.Get.System(message, _) => show"unknown error: $message"
        }
      permissions <- EitherT.fromOption(userOpt.map(_.permissions), "user is not registered")
    } yield permissions

    resultF
      .fold(identity, _.mkString(","))
      .flatMap(reply(_).void)
  }
}
