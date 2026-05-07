package com.github.a7emenov.process.gamesession

import cats.Functor
import cats.effect.{Ref, Resource, Sync}
import com.github.a7emenov.domain.gamesession.{GameSession, SessionParticipant}
import com.github.a7emenov.process.gamesession
import com.github.a7emenov.process.gamesession.GameSessionProcess
import com.github.a7emenov.service.gamesession.GameSessionService
import cats.syntax.either.*
import cats.syntax.functor.*
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.process.gamesession.GameSessionProcess.Error
import com.github.a7emenov.process.gamesession.GameSessionProcessDefault.*

class GameSessionProcessDefault[F[_]: Functor](
    gameSessionService: GameSessionService[F]
) extends GameSessionProcess[F]:

  override def record(session: GameSession): F[Either[GameSessionProcess.Error.Record, GameSession.WithId]] =
    gameSessionService.record(session)
      .map(_.leftMap(toRecordError))

  override def getByScribe(userId: User.Id)
      : fs2.Stream[F, Either[GameSessionProcess.Error.Get, List[GameSession.WithId]]] =
    gameSessionService.getByScribe(userId, chunkSize = 100)
      .map(_.leftMap(toGetError))

object GameSessionProcessDefault:

  def make[F[_]: Functor](gameSessionService: GameSessionService[F]): Resource[F, GameSessionProcess[F]] =
    Resource.pure(GameSessionProcessDefault(gameSessionService))

  private def toRecordError(error: GameSessionService.Error.Record): GameSessionProcess.Error.Record =
    error match {
      case cause: GameSessionService.Error.Record.System =>
        GameSessionProcess.Error.Record.System("Unknown game session service record error", cause)
    }

  private def toGetError(error: GameSessionService.Error.Get): GameSessionProcess.Error.Get =
    error match {
      case cause: GameSessionService.Error.Get.System =>
        GameSessionProcess.Error.Get.System("Unknown game session service get error", cause)
    }
