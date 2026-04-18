package com.github.a7emenov.process.gamesession

import cats.effect.{Resource, Sync}
import com.github.a7emenov.domain.game.{GameSession, SessionParticipant}
import com.github.a7emenov.service.gamesession
import cats.syntax.option.*
import com.github.a7emenov.service.gamesession.GameSessionService

trait GameSessionProcess[F[_]]:

  def record(session: GameSession): F[Either[GameSessionProcess.Error.Record, GameSession.WithId]]

  def getByHost(host: SessionParticipant): F[Either[GameSessionProcess.Error.Get, Map[GameSession.Id, GameSession]]]

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
