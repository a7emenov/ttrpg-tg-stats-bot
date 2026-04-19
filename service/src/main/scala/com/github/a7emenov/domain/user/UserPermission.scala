package com.github.a7emenov.domain.user

import cats.Order

enum UserPermission:
  case ViewGameSessions, RecordGameSessions, InternalAccess

object UserPermission:
  implicit val order: Order[UserPermission] =
    Order.by(_.ordinal)
