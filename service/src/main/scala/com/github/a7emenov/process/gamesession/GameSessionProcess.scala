package com.github.a7emenov.process.gamesession

import cats.effect.{Resource, Sync}
import com.github.a7emenov.domain.gamesession.{GameSession, SessionParticipant}
import cats.syntax.option.*
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.service.gamesession.GameSessionService

trait GameSessionProcess[F[_]]:

  def record(session: GameSession): F[Either[GameSessionProcess.Error.Record, GameSession.WithId]]

  def getByScribe(userId: User.Id): fs2.Stream[F, Either[GameSessionProcess.Error.Get, List[GameSession.WithId]]]

  def getByHost(host: SessionParticipant): fs2.Stream[F, Either[GameSessionProcess.Error.Get, List[GameSession.WithId]]]

object GameSessionProcess:

  def make[F[_]: Sync](service: GameSessionService[F]): Resource[F, GameSessionProcess[F]] =
    GameSessionProcessDefault.make(service)

  sealed abstract class Error(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull)

  object Error:
    sealed abstract class Record(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Record:
      case class System(message: String, cause: Throwable) extends GameSessionProcess.Error.Record(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Get:
      case class System(message: String, cause: Throwable) extends GameSessionProcess.Error.Get(message, cause.some)
