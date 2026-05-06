package com.github.a7emenov.api.telegram.command

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.show.*
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, ReplyKeyboardRemove}
import com.github.a7emenov.api.telegram.callbackdata.{BotCallbackData, BotCallbackDataHandler}
import com.github.a7emenov.api.telegram.command.BotGameSessionCommandHandler.*
import com.github.a7emenov.api.telegram.user.{BotCallbackUserHandler, BotMessageUserHandler}
import com.github.a7emenov.domain.gamesession.GameSession
import com.github.a7emenov.domain.user.UserPermission
import com.github.a7emenov.process.gamesession.GameSessionProcess

trait BotGameSessionCommandHandler[F[_]] extends Commands[F]
    with BotMessageUserHandler[F]
    with BotCallbackDataHandler[F]
    with BotCallbackUserHandler[F]:

  override val callbackHandlerTag: BotCallbackData.HandlerTag =
    BotCallbackData.HandlerTag.GameSession

  protected implicit val async: Async[F]

  protected def gameSessionProcess: GameSessionProcess[F]

  onCommand(BotCommand.RecordGameSession.name) { implicit command =>
    withMessagePermissions(UserPermission.RecordGameSessions) { user =>
      val yesBtn = InlineKeyboardButton.callbackData(
        text = "Yes",
        cbd = makeCallbackData(CallbackTag.IsUserHost.value, CommandCallbackData.IsHost.Yes)
      )
      val noBtn = InlineKeyboardButton.callbackData(
        text = "No",
        cbd = makeCallbackData(CallbackTag.IsUserHost.value, CommandCallbackData.IsHost.No)
      )
      val markup = InlineKeyboardMarkup(Seq(Seq(yesBtn), Seq(noBtn)))
      reply("Are you a host?", replyMarkup = markup.some).void
    }
  }

  onCommand(BotCommand.ListGames.name) { implicit command =>
    withMessagePermissions(UserPermission.ViewGameSessions) { user =>
      EitherT(gameSessionProcess.getByScribe(user.id)
        .rethrow
        .compile
        .toList
        .map(_.flatten)
        .redeem(
          e => show"game retrieval error: ${e.getMessage}".asLeft,
          _.mkString("(", "\n", ")").asRight
        ))
        .merge
        .flatMap(reply(_).void)
    }
  }

  onCallbackTag(CallbackTag.IsUserHost.value) { implicit query =>
    { (data: List[String]) =>
      withCallbackMessage { implicit message =>
        withCallbackPermissions(UserPermission.RecordGameSessions) { user =>
          (data match {
            case List(CommandCallbackData.IsHost.Yes) =>
              for {
                recordResult <- gameSessionProcess.record(
                  GameSession.newSession(scribeUserId = user.id)
                ).recover { case e =>
                  GameSessionProcess.Error.Record.System("Unknown game record error", e).asLeft
                }
                result <- recordResult match {
                  case Left(e) =>
                    ackCallback(text = e.message.some)
                  case Right(game) =>
                    ackCallback(text = s"Created game with id and yes host ${game.id}".some)
                      >> reply("Test reply yes").as(true)
                }
              } yield result
            case List(CommandCallbackData.IsHost.No) =>
              for {
                recordResult <- gameSessionProcess.record(
                  GameSession.newSession(scribeUserId = user.id)
                ).recover { case e =>
                  GameSessionProcess.Error.Record.System("Unknown game record error", e).asLeft
                }
                result <- recordResult match {
                  case Left(e) =>
                    ackCallback(text = e.message.some)
                  case Right(game) =>
                    ackCallback(text = s"Created game with id and no host ${game.id}".some)
                      >> reply("Test reply no").as(true)
                }
              } yield result
            case _ =>
              ackCallback(text = "Invalid callback data".some)
          }).void
        }
      }
    }
  }

object BotGameSessionCommandHandler:

  private enum CallbackTag:
    case IsUserHost extends CallbackTag

    val value: String =
      this.ordinal.toString

  private object CommandCallbackData:

    object IsHost:
      val Yes: String = "y"
      val No: String = "n"
