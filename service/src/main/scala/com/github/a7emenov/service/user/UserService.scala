package com.github.a7emenov.service.user

import cats.effect.Resource
import cats.effect.Sync
import com.github.a7emenov.domain.user.{User, UserRole}
import cats.syntax.option.*
import cats.syntax.show.*
import com.github.a7emenov.util.error.AppError

trait UserService[F[_]]:

  def create(id: User.Id, role: UserRole): F[Either[UserService.Error.Create, User]]

  def get(id: User.Id): F[Either[UserService.Error.Get, Option[User]]]

object UserService:

  def make[F[_]: Sync]: Resource[F, UserService[F]] =
    UserServiceInMemory.make

  sealed abstract class Error(message: String, cause: Option[Throwable])
      extends AppError(message, cause)

  object Error:
    sealed abstract class Create(message: String, cause: Option[Throwable])
        extends UserService.Error(message, cause)

    object Create:
      final case class AlreadyExists(id: User.Id)
          extends UserService.Error.Create(show"User with id $id already exists", none)
      final case class System(override val message: String, cause: Throwable)
          extends UserService.Error.Create(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable])
        extends UserService.Error(message, cause)

    object Get:
      final case class System(override val message: String, cause: Throwable)
          extends UserService.Error.Get(message, cause.some)
