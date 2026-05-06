package com.github.a7emenov.api.telegram.command

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.show.*
import com.bot4s.telegram.api.declarative.Commands
import com.github.a7emenov.api.telegram.callbackdata.{BotCallbackData, BotCallbackDataHandler}
import com.github.a7emenov.api.telegram.user.{BotCallbackUserHandler, BotMessageUserHandler}
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

object BotGameSessionCommandHandler:

  private enum CallbackTag:
    case IsUserHost extends CallbackTag

    val value: String =
      this.ordinal.toString

  private object CommandCallbackData:

    object IsHost:
      val Yes: String = "y"
      val No: String = "n"
