package com.github.a7emenov.domain.util

import cats.Show
import cats.syntax.show.*

class Secret private (val value: String) extends AnyVal:

  override def toString: String =
    this.show

object Secret:

  implicit val show: Show[Secret] =
    Show.show(_ => "****")

  def apply(value: String): Secret =
    new Secret(value)
