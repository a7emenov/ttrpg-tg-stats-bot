package com.github.a7emenov.api.telegram.command

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.show.*
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup}
import com.github.a7emenov.api.telegram.callbackdata.{BotCallbackData, BotCallbackDataHandler}
import com.github.a7emenov.api.telegram.command.BotGameSessionCommandHandler.*
import com.github.a7emenov.api.telegram.dialoguestate.BotDialogueStateHandler
import com.github.a7emenov.api.telegram.user.{BotCallbackUserHandler, BotMessageUserHandler}
import com.github.a7emenov.domain.dialoguestate.DialogueState
import com.github.a7emenov.domain.gamesession.{GameSession, GameType, SessionParticipant}
import com.github.a7emenov.domain.user.UserPermission
import com.github.a7emenov.process.gamesession.GameSessionProcess

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

trait BotGameSessionCommandHandler[F[_]] extends Commands[F]
    with BotMessageUserHandler[F]
    with BotCallbackDataHandler[F]
    with BotCallbackUserHandler[F]
    with BotDialogueStateHandler[F]:

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

  onCommand(BotCommand.RecordGameSession.name) { implicit command =>
    withMessagePermissions(UserPermission.RecordGameSessions) { user =>
      onDialogueState(user.id) {
        case DialogueState.Empty =>
          reply("Input game date yyyy-mm-dd").as(DialogueState.RecordingNewSession.Timeframe)

        case state =>
          state.pure[F]
      }()
    }
  }

  onMessage { implicit message =>
    withMessageUserId { userId =>
      onDialogueState(userId) {
        case state @ DialogueState.RecordingNewSession.Timeframe =>
          message.text.flatMap(text => Try(LocalDate.parse(text, localDateFormatter)).toOption) match {
            case Some(date) =>
              reply("Input game name").as(
                DialogueState.RecordingNewSession.Name(
                  GameSession.Timeframe.DateOnly(date, duration = None)
                )
              )

            case None =>
              reply("Input game date yyyy-mm-dd").as(state)
          }

        case state: DialogueState.RecordingNewSession.Name =>
          message.text match {
            case Some(name) =>
              reply("Input additional hosts (comma-separated)").as(
                DialogueState.RecordingNewSession.AdditionalHosts(
                  state.timeframe,
                  GameSession.GameName(name)
                )
              )

            case None =>
              reply("Input game name").as(state)
          }

        case state: DialogueState.RecordingNewSession.AdditionalHosts =>
          val yesBtn = InlineKeyboardButton.callbackData(
            text = "Yes",
            cbd = makeCallbackData(CallbackTag.IsEventGame.value, CommandCallbackData.IsEventGame.Yes)
          )
          val noBtn = InlineKeyboardButton.callbackData(
            text = "No",
            cbd = makeCallbackData(CallbackTag.IsEventGame.value, CommandCallbackData.IsEventGame.No)
          )
          val markup = InlineKeyboardMarkup(Seq(Seq(yesBtn), Seq(noBtn)))
          message.text match {
            case Some(hosts) =>
              reply("Is event game", replyMarkup = markup.some).as(
                DialogueState.RecordingNewSession.GameType(
                  state.timeframe,
                  state.name,
                  hosts.split(",")
                    .toList
                    .map(SessionParticipant.Username(_))
                    .map(SessionParticipant.WithoutId(_))
                )
              )

            case None =>
              reply("Is event game?", replyMarkup = markup.some).as(
                DialogueState.RecordingNewSession.GameType(
                  state.timeframe,
                  state.name,
                  List.empty
                )
              )
          }
      }()
    }
  }

  onCallbackTag(CallbackTag.IsEventGame.value) { implicit query =>
    { (data: List[String]) =>
      withCallbackMessage { implicit message =>
        withCallbackPermissions(UserPermission.RecordGameSessions) { user =>
          onDialogueState(user.id) {
            case state: DialogueState.RecordingNewSession.GameType =>
              val choiceEither: Either[String, GameType] = data match {
                case List(CommandCallbackData.IsEventGame.Yes) =>
                  GameType.Event.asRight
                case List(CommandCallbackData.IsEventGame.No) =>
                  GameType.Standard.asRight
                case _ =>
                  "unknown game type".asLeft
              }

              choiceEither match {
                case Right(gameType) =>
                  for {
                    recordResult <- gameSessionProcess.record(
                      GameSession.newSession(
                        scribe = SessionParticipant.WithId(
                          user.id,
                          SessionParticipant.Username(query.from.username.getOrElse(query.from.firstName))
                        ),
                        name = state.name,
                        timeframe = state.timeframe,
                        gameType = gameType
                      )
                    )
                    result <- recordResult match {
                      case Left(e) =>
                        ackCallback(text = e.message.some).as(state)
                      case Right(game) =>
                        ackCallback(text = s"Created game with id ${game.id}".some) >>
                          reply(s"Created game with id ${game.id}").as(DialogueState.Empty)
                    }
                  } yield result

                case Left(e) =>
                  ackCallback(text = e.some).as(state)
              }
          }()
        }
      }
    }
  }

object BotGameSessionCommandHandler:

  private val localDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_LOCAL_DATE

  private enum CallbackTag:
    case IsEventGame extends CallbackTag

    val value: String =
      this.ordinal.toString

  private object CommandCallbackData:

    object IsEventGame:
      val Yes: String = "y"
      val No: String = "n"
