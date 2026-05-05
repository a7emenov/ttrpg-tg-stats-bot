package com.github.a7emenov.api.telegram.callback

import cats.syntax.applicative.*
import com.bot4s.telegram.api.declarative.{Action, Callbacks}
import com.bot4s.telegram.models.{CallbackQuery, Message}

trait BotCallbackHandler[F[_]] extends Callbacks[F]:

  val callbackHandlerTag: BotCommandCallbackData.HandlerTag

  /** Processes only callbacks for a given command tag (for callback data created through [[makeCallbackData]]).
   *  Callback query can be referenced in the anonymous function using `summon[CallbackQuery]`.
   */
  def onCommandCallback(commandTag: String)(actionT: CallbackQuery => Action[F, List[String]]): Unit =
    onCallbackQuery { implicit query =>
      query.data match {
        case Some(BotCommandCallbackData(`callbackHandlerTag`, `commandTag`, data)) =>
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

  def makeCallbackData(commandTag: String, data: String*): String =
    BotCommandCallbackData(
      handlerTag = callbackHandlerTag,
      commandTag = commandTag,
      data = data*
    )
