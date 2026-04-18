package com.github.a7emenov.api.telegram

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.github.a7emenov.domain.user.{User, UserPermission, UserRole}
import sttp.client4.Backend
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

  onCommand(BotCommand.Apply.name) { implicit command =>
    withArgs { args =>
      val resultF = for {
        role <- EitherT.fromEither(args.headOption.map(_.toLowerCase()).fold(
          "provide a role".asLeft
        ) {
          case "admin" => UserRole.Admin.asRight
          case "scribe" => UserRole.Scribe.asRight
          case "reader" => UserRole.Reader.asRight
          case _ => "invalid user role".asLeft
        })
        userId <-
          EitherT.fromOption(command.from.map(user => User.Id(user.id.toString)), "command must be run by a user")
        result <- EitherT(userProcess.apply(userId, role))
          .leftMap {
            case _: UserProcess.Error.Apply.UserAlreadyExists => "user already exists"
            case _: UserProcess.Error.Apply.ApplicationAlreadyExists => "application already exists"
            case UserProcess.Error.Apply.System(message, _) => show"unknown error: $message"
          }
      } yield result

      resultF
        .fold(identity, _ => "application created successfully")
        .flatMap(reply(_).void)
    }
  }

  onCommand(BotCommand.ApproveUserApplication.name) { implicit command =>
    withArgs { args =>
      val resultF = for {
        userId          <- EitherT.fromOption(args.headOption.map(User.Id(_)), "provide a user id")
        commandRunnerId <-
          EitherT.fromOption(command.from.map(user => User.Id(user.id.toString)), "command must be run by a user")
        commandRunnerUser <- EitherT(userProcess.get(commandRunnerId))
          .leftMap {
            case UserProcess.Error.Get.System(message, _) => show"unknown error: $message"
          }
          .subflatMap {
            case None => "user is not registered".asLeft
            case Some(user) => user.asRight
          }
        result <- if commandRunnerUser.permissions.contains(UserPermission.InternalAccess) then
          EitherT(userProcess.approveApplication(userId))
            .leftMap {
              case _: UserProcess.Error.ApproveApplication.NotFound => "application not found"
              case UserProcess.Error.ApproveApplication.System(message, _) => show"unknown error: $message"
            }
        else
          EitherT.leftT("user does not have permissions")
      } yield result

      resultF
        .fold(identity, _ => "application approved successfully")
        .flatMap(reply(_).void)
    }
  }

  onCommand(BotCommand.ListUserApplications.name) { implicit command =>
    withArgs { args =>
      val resultF = for {
        commandRunnerId <-
          EitherT.fromOption(command.from.map(user => User.Id(user.id.toString)), "command must be run by a user")
        commandRunnerUser <- EitherT(userProcess.get(commandRunnerId))
          .leftMap {
            case UserProcess.Error.Get.System(message, _) => show"unknown error: $message"
          }
          .subflatMap {
            case None => "user is not registered".asLeft
            case Some(user) => user.asRight
          }
        result <- if commandRunnerUser.permissions.contains(UserPermission.InternalAccess) then
          EitherT(userProcess.getUserApplications
            .rethrow
            .compile
            .toList
            .map(_.flatten)
            .redeem(
              e => s"application retrieval error: ${e.getMessage}".asLeft,
              _.mkString("(", ",", ")").asRight
            ))
        else
          EitherT.leftT("user does not have permissions")
      } yield result

      resultF
        .merge
        .flatMap(reply(_).void)
    }
  }
}
