package com.github.a7emenov.api.telegram

import cats.data.EitherT
import cats.effect.Async
import com.bot4s.telegram.api.declarative.Commands
import com.github.a7emenov.domain.user.{User, UserPermission, UserRole}
import com.github.a7emenov.process.user.UserProcess
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*

trait BotUserCommandHandler[F[_]] extends Commands[F] with BotUserCheck[F] {

  implicit val async: Async[F]

  onCommand(BotCommand.SetupAdmin.name) { implicit command =>
    withCommandUserId { commandUserId =>
      withArgs { args =>
        val resultF = for {
          setupToken <-
            EitherT.fromOption(args.headOption.map(UserProcess.SetupToken.fromString), "provide a setup token")
          result <- EitherT(userProcess.setupAdmin(commandUserId, setupToken))
            .leftMap {
              case UserProcess.Error.SetupAdmin.InvalidToken => "setup token is incorrect"
              case UserProcess.Error.SetupAdmin.AlreadyExists => "admin already exists"
              case UserProcess.Error.SetupAdmin.System(message, _) => show"unknown error: $message"
            }
        } yield result

        resultF
          .fold(identity, _ => "admin setup successfully")
          .flatMap(reply(_).void)
      }
    }
  }

  onCommand(BotCommand.CheckPermissions.name) { implicit command =>
    withUser { commandUser =>
      reply(commandUser.permissions.mkString(",")).void
    }
  }

  onCommand(BotCommand.Apply.name) { implicit command =>
    withCommandUserId { commandUserId =>
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
          result <- EitherT(userProcess.apply(commandUserId, role))
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
  }

  onCommand(BotCommand.ApproveUserApplication.name) { implicit command =>
    withPermissions(UserPermission.InternalAccess) { commandUser =>
      withArgs { args =>
        val resultF = for {
          userId <- EitherT.fromOption(args.headOption.map(User.Id(_)), "provide a user id")
          result <- EitherT(userProcess.approveApplication(userId))
            .leftMap {
              case _: UserProcess.Error.ApproveApplication.NotFound => "application not found"
              case UserProcess.Error.ApproveApplication.System(message, _) => show"unknown error: $message"
            }
        } yield result

        resultF
          .fold(identity, _ => "application approved successfully")
          .flatMap(reply(_).void)
      }
    }
  }

  onCommand(BotCommand.ListUserApplications.name) { implicit command =>
    withPermissions(UserPermission.InternalAccess) { commandUser =>
      withArgs { args =>
        val resultF = for {
          result <- EitherT(userProcess.getUserApplications
            .rethrow
            .compile
            .toList
            .map(_.flatten)
            .redeem(
              e => s"application retrieval error: ${e.getMessage}".asLeft,
              _.mkString("(", ",", ")").asRight
            ))
        } yield result

        resultF
          .merge
          .flatMap(reply(_).void)
      }
    }
  }
}
