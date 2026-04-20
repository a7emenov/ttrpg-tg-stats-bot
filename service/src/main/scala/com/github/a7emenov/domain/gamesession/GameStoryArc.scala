package com.github.a7emenov.domain.gamesession

opaque type GameStoryArc = String

object GameStoryArc:
  def apply(value: String): GameStoryArc =
    value

  extension (arc: GameStoryArc)
    def value: String = arc

  opaque type SessionNumber = Int

  object SessionNumber:
    def apply(value: Int): GameStoryArc.SessionNumber =
      value

    extension (number: GameStoryArc.SessionNumber)
      def value: Int = number
