package com.github.a7emenov.service.gamesession

import cats.effect.{Resource, Sync}
import com.github.a7emenov.domain.gamesession.{GameSession, SessionParticipant}
import com.github.a7emenov.service.gamesession
import cats.syntax.option.*
import com.github.a7emenov.domain.user.User

trait GameSessionService[F[_]]:

  def record(session: GameSession): F[Either[GameSessionService.Error.Record, GameSession.WithId]]

  def getByScribe(
      userId: User.Id,
      chunkSize: Int
  ): fs2.Stream[F, Either[GameSessionService.Error.Get, List[GameSession.WithId]]]

  def getByHost(
      host: SessionParticipant,
      chunkSize: Int
  ): fs2.Stream[F, Either[GameSessionService.Error.Get, List[GameSession.WithId]]]

object GameSessionService:

  def make[F[_]: Sync]: Resource[F, GameSessionService[F]] =
    GameSessionServiceInMemory.make

  sealed abstract class Error(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull)

  object Error:
    sealed abstract class Record(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Record:
      case class System(message: String, cause: Throwable) extends GameSessionService.Error.Record(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Get:
      case class System(message: String, cause: Throwable) extends GameSessionService.Error.Get(message, cause.some)
