package com.github.a7emenov.service.user

import cats.effect.Sync
import com.github.a7emenov.domain.user.User
import cats.syntax.option.*
import cats.syntax.show.*

trait UserService[F[_]]:

  def create(id: User.Id): F[Either[UserService.Error.Create, User]]

  def get(id: User.Id): F[Either[UserService.Error.Get, Option[User]]]

object UserService:

  def make[F[_]: Sync]: F[UserService[F]] =
    UserServiceInMemory.make

  sealed abstract class Error(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull)

  object Error:
    sealed abstract class Create(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Create:
      case class AlreadyExists(id: User.Id)
          extends UserService.Error.Create(show"User with id $id already exists", none)
      case class System(message: String, cause: Throwable) extends UserService.Error.Create(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Get:
      case class System(message: String, cause: Throwable) extends UserService.Error.Get(message, cause.some)
