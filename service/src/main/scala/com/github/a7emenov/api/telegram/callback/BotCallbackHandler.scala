package com.github.a7emenov.api.telegram.callback

import cats.syntax.applicative.*
import com.bot4s.telegram.api.declarative.{Action, Callbacks}
import com.bot4s.telegram.models.{CallbackQuery, Message}

trait BotCallbackHandler[F[_]] extends Callbacks[F]:

  val callbackHandlerTag: BotCommandCallbackData.HandlerTag

  /** Processes only callbacks for a given callback tag (for callback data created through [[makeCallbackData]]).
   */
  def onCallbackTag(callbackTag: String)(actionT: CallbackQuery => Action[F, List[String]]): Unit =
    onCallbackQuery { implicit query =>
      query.data match {
        case Some(BotCommandCallbackData(`callbackHandlerTag`, `callbackTag`, data)) =>
          actionT(query)(data)

        case Some(_) | None =>
          ().pure[F]
      }
    }

  def withCallbackMessage(action: Action[F, Message])(implicit query: CallbackQuery): F[Unit] =
    query.message match {
      case Some(message) =>
        action(message)
      case None =>
        ().pure[F]
    }

  def makeCallbackData(callbackTag: String, data: String*): String =
    BotCommandCallbackData(
      handlerTag = callbackHandlerTag,
      callbackTag = callbackTag,
      data = data*
    )
