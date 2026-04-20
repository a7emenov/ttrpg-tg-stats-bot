package com.github.a7emenov.domain.gamesession

opaque type GameSystem = String

object GameSystem:
  def apply(value: String): GameSystem =
    value

  extension (system: GameSystem)
    def value: String = system
