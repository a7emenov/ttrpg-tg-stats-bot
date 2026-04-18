package com.github.a7emenov.service.gamesession

import cats.Functor
import cats.effect.{Ref, Resource, Sync}
import cats.syntax.either.*
import cats.syntax.functor.*
import com.github.a7emenov.domain.game.{GameSession, SessionParticipant}

class GameSessionServiceInMemory[F[_]: Functor](
    ref: Ref[F, List[GameSession.WithId]],
) extends GameSessionService[F]:

  def record(session: GameSession): F[Either[GameSessionService.Error.Record, GameSession.WithId]] =
    ref.modify { sessions =>
      val newId = GameSession.Id((sessions.map(_.id.value.toInt).maxOption.getOrElse(0) + 1).toString)
      val sessionWithId = GameSession.WithId(newId, session)
      (sessionWithId :: sessions, sessionWithId)
    }.map(_.asRight[GameSessionService.Error.Record])

  def getByHost(host: SessionParticipant): F[Either[GameSessionService.Error.Get, Map[GameSession.Id, GameSession]]] =
    ref.get
      .map(_.filter { case GameSession.WithId(_, session) =>
        session.hosts.exists { sessionHost =>
          sessionHost.username == host.username || sessionHost.id.exists(host.id.contains)
        }
      }
        .map { case GameSession.WithId(id, session) => id -> session }
        .toMap).map(_.asRight[GameSessionService.Error.Get])

object GameSessionServiceInMemory:

  def make[F[_]: Sync]: Resource[F, GameSessionService[F]] =
    Resource.eval(Ref.of(List.empty[GameSession.WithId]).map(GameSessionServiceInMemory(_)))
