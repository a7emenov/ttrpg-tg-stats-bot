package com.github.a7emenov.domain.gamesession

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.data.{NonEmptyList, NonEmptySet}
import cats.syntax.option.*
import com.github.a7emenov.domain.user.User

/** A representation of a recorded game session.
 *  Optional fields indicate whether they were provided by the user.
 */
case class GameSession(
    requiredData: GameSession.RequiredData,
    coreData: Option[GameSession.CoreData],
    storyArcData: Option[GameSession.StoryArcData],
    deathData: Option[GameSession.DeathData]
)

object GameSession:

  def newSession(
      scribe: SessionParticipant.WithId,
      name: GameSession.GameName,
      timeframe: Timeframe,
      gameType: GameType
  ): GameSession =
    GameSession(
      requiredData = GameSession.RequiredData(
        name = name,
        scribes = NonEmptySet.one(scribe.id),
        hosts = NonEmptyList.one(scribe),
        timeframe = timeframe,
        gameType = gameType
      ),
      coreData = none,
      storyArcData = none,
      deathData = none
    )

  final case class WithId(id: GameSession.Id, session: GameSession)

  /** An artificial unique identifier for a game session.
   */
  opaque type Id = String

  object Id:
    def apply(value: String): GameSession.Id =
      value

    extension (id: GameSession.Id)
      def value: String = id

  final case class RequiredData(
      name: GameSession.GameName,
      scribes: NonEmptySet[User.Id],
      hosts: NonEmptyList[SessionParticipant],
      timeframe: GameSession.Timeframe,
      gameType: GameType
  )

  final case class CoreData(
      genres: Option[NonEmptyList[GameGenre]],
      system: Option[GameSystem],
      players: Option[GameSession.Players],

  )

  final case class StoryArcData(arc: Option[GameStoryArc], sessionNumber: Option[GameStoryArc.SessionNumber])

  final case class DeathData(deadCharactersCount: Option[Int], playersWithDeadCharactersCount: Option[Int])

  opaque type GameName = String

  object GameName:
    def apply(value: String): GameName =
      value

    extension (system: GameName)
      def value: String = system

  sealed trait Players:
    def count: Int

  object Players:
    final case class CountOnly(count: Int) extends Players
    final case class NameList(nonEmptyList: NonEmptyList[SessionParticipant]) extends Players:
      override def count: Int = nonEmptyList.size

  sealed trait Timeframe:
    def date: LocalDate

    def duration: Option[FiniteDuration]

  object Timeframe:
    final case class DateOnly(date: LocalDate, duration: Option[FiniteDuration]) extends Timeframe

    sealed abstract case class Precise(start: LocalDateTime, end: LocalDateTime) extends Timeframe:
      override def date: LocalDate = start.toLocalDate

      override def duration: Option[FiniteDuration] =
        FiniteDuration(ChronoUnit.SECONDS.between(start, end), TimeUnit.SECONDS).some

    object Precise:
      def apply(start: LocalDateTime, end: LocalDateTime): GameSession.Timeframe.Precise =
        if start.isBefore(end) then
          new GameSession.Timeframe.Precise(start, end) {}
        else
          new GameSession.Timeframe.Precise(end, start) {}
