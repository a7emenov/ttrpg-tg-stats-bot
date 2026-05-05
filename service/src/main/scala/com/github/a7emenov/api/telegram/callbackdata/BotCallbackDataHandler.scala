package com.github.a7emenov.api.telegram.callbackdata

import scala.util.control.NonFatal

import cats.syntax.applicative.*
import com.bot4s.telegram.api.declarative.{Action, Callbacks}
import com.bot4s.telegram.models.{CallbackQuery, Message}
import cats.syntax.applicativeError.*
import cats.syntax.functor.*
import cats.syntax.option.*

trait BotCallbackDataHandler[F[_]] extends Callbacks[F]:

  val callbackHandlerTag: BotCallbackData.HandlerTag

  /** Processes only callbacks for a given callback tag (for callback data created through [[makeCallbackData]]).
   *  Also guarantees that a callback has been acknowledged even if `actionT` fails.
   *  Otherwise, `actionT` is responsible for calling [[ackCallback]].
   */
  def onCallbackTag(callbackTag: String)(actionT: CallbackQuery => Action[F, List[String]]): Unit =
    onCallbackQuery { implicit query =>
      query.data match {
        case Some(BotCallbackData(`callbackHandlerTag`, `callbackTag`, data)) =>
          actionT(query)(data).recover {
            case NonFatal(e) =>
              defaultUnknownErrorAck
          }

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
    BotCallbackData(
      handlerTag = callbackHandlerTag,
      callbackTag = callbackTag,
      data = data*
    )

  private def defaultUnknownErrorAck(implicit cbq: CallbackQuery): F[Unit] =
    ackCallback(text = "unknown error".some).void
