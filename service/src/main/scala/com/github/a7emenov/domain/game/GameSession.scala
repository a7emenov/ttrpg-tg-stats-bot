package com.github.a7emenov.domain.game

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.data.NonEmptyList
import cats.syntax.option.*

/** A representation of a recorded game session.
 *  Some root fields are optional to indicate whether they were provided by the user.
 *  Session are not intended to be recorded without non-optional fields.
 */
case class GameSession(
    genres: Option[NonEmptyList[GameGenre]],
    system: Option[GameSystem],
    gameType: Option[GameType],
    hosts: NonEmptyList[SessionParticipant],
    players: Option[GameSession.Players],
    timeframe: GameSession.Timeframe,
    storyArcData: Option[GameSession.StoryArcData],
    deathData: Option[GameSession.DeathData]
)

object GameSession:

  case class WithId(id: GameSession.Id, session: GameSession)

  /** An artificial unique identifier for a game session.
   */
  opaque type Id = String

  object Id:
    def apply(value: String): GameSession.Id =
      value

    extension (id: GameSession.Id)
      def value: String = id

  sealed trait Players:
    def count: Int

  object Players:
    case class CountOnly(count: Int) extends Players
    case class NameList(nonEmptyList: NonEmptyList[SessionParticipant]) extends Players:
      override def count: Int = nonEmptyList.size

  sealed trait Timeframe:
    def date: LocalDate

    def duration: Option[FiniteDuration]

  object Timeframe:
    case class DateOnly(date: LocalDate, duration: Option[FiniteDuration]) extends Timeframe

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

  case class StoryArcData(arc: Option[GameStoryArc], sessionNumber: Option[GameStoryArc.SessionNumber])

  case class DeathData(deadCharactersCount: Option[Int], playersWithDeadCharactersCount: Option[Int])
