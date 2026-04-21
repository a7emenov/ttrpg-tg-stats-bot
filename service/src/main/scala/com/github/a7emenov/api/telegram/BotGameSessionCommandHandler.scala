package com.github.a7emenov.api.telegram

import cats.syntax.functor.*
import cats.syntax.option.*
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import com.github.a7emenov.api.telegram.BotGameSessionCommandHandler.*
import com.github.a7emenov.domain.user.UserPermission
import com.github.a7emenov.process.gamesession.GameSessionProcess

trait BotGameSessionCommandHandler[F[_]] extends Commands[F] with Callbacks[F] with BotUserCheck[F]:

  protected def gameSessionProcess: GameSessionProcess[F]

  onCommand(BotCommand.RecordGameSession.name) { implicit command =>
    withMessagePermissions(UserPermission.RecordGameSessions) { user =>
      val yesBtn = InlineKeyboardButton.callbackData(
        text = "Yes",
        cbd = makeCallbackData(CommandTag.IsUserHost, CommandCallbackData.IsHost.Yes)
      )
      val noBtn = InlineKeyboardButton.callbackData(
        text = "No",
        cbd = makeCallbackData(CommandTag.IsUserHost, CommandCallbackData.IsHost.No)
      )
      val markup = InlineKeyboardMarkup(Seq(Seq(yesBtn), Seq(noBtn)))
      reply("Are you a host?", replyMarkup = markup.some).void
    }
  }

  onCallbackWithTag(CommandTag.IsUserHost.tag) { implicit cbq =>
    for {
      _ <- ackCallback(text = s"You pressed ${cbq.data}".some)
    } yield ()
  }

object BotGameSessionCommandHandler:

  private def makeCallbackData(commandTag: CommandTag, data: String): String =
    BotCommandCallbackData.of(
      wholeTag = commandTag.tag,
      data = data
    )

  private enum CommandTag(val name: String):
    case IsUserHost extends CommandTag("iuh")

    val tag: String =
      BotCommandCallbackData.tag(BotCommandCallbackData.HandlerTag.GameSession, name)

  private object CommandCallbackData:

    object IsHost:
      val Yes: String = "y"
      val No: String = "n"
