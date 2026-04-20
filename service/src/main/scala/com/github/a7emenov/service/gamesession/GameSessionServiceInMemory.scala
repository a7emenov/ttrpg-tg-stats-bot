package com.github.a7emenov.service.gamesession

import cats.Functor
import cats.effect.{Ref, Resource, Sync}
import cats.syntax.either.*
import cats.syntax.functor.*
import com.github.a7emenov.domain.gamesession.{GameSession, SessionParticipant}
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.service.gamesession.GameSessionService.Error

class GameSessionServiceInMemory[F[_]: Functor](
    ref: Ref[F, List[GameSession.WithId]],
) extends GameSessionService[F]:

  def record(session: GameSession): F[Either[GameSessionService.Error.Record, GameSession.WithId]] =
    ref.modify { sessions =>
      val newId = GameSession.Id((sessions.map(_.id.value.toInt).maxOption.getOrElse(0) + 1).toString)
      val sessionWithId = GameSession.WithId(newId, session)
      (sessionWithId :: sessions, sessionWithId)
    }.map(_.asRight[GameSessionService.Error.Record])

  override def getByScribe(
      userId: User.Id,
      chunkSize: Int
  ): fs2.Stream[F, Either[Error.Get, List[GameSession.WithId]]] =
    fs2.Stream.eval(
      ref.get
        .map { sessions =>
          sessions
            .filter { case GameSession.WithId(_, session) =>
              session.scribe.id == userId
            }
        }.map(_.asRight[GameSessionService.Error.Get])
    )

  def getByHost(host: SessionParticipant, chunkSize: Int)
      : fs2.Stream[F, Either[GameSessionService.Error.Get, List[GameSession.WithId]]] =
    fs2.Stream.eval(
      ref.get
        .map { sessions =>
          sessions
            .filter { case GameSession.WithId(_, session) =>
              session.hosts.exists(_.exists { sessionHost =>
                sessionHost.username == host.username || sessionHost.id.exists(host.id.contains)
              })
            }
        }.map(_.asRight[GameSessionService.Error.Get])
    )

object GameSessionServiceInMemory:

  def make[F[_]: Sync]: Resource[F, GameSessionService[F]] =
    Resource.eval(Ref.of(List.empty[GameSession.WithId]).map(GameSessionServiceInMemory(_)))
