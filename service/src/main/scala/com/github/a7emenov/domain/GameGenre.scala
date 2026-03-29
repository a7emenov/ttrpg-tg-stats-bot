package com.github.a7emenov.domain

opaque type GameGenre = String

object GameGenre:
  def apply(value: String): GameGenre =
    value

  extension (genre: GameGenre)
    def value: String = genre
