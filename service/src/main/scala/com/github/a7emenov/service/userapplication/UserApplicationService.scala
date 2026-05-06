package com.github.a7emenov.service.userapplication

import cats.effect.{Resource, Sync}
import cats.syntax.option.*
import cats.syntax.show.*
import com.github.a7emenov.domain.user.{User, UserApplication, UserRole}
import com.github.a7emenov.util.error.AppError

trait UserApplicationService[F[_]]:

  def create(id: User.Id, role: UserRole): F[Either[UserApplicationService.Error.Create, UserApplication]]

  def get(id: User.Id): F[Either[UserApplicationService.Error.Get, Option[UserApplication]]]

  def getAll(chunkSize: Int): fs2.Stream[F, Either[UserApplicationService.Error.Get, List[UserApplication]]]

  def delete(id: User.Id): F[Either[UserApplicationService.Error.Delete, Unit]]

object UserApplicationService:

  def make[F[_]: Sync]: Resource[F, UserApplicationService[F]] =
    UserApplicationServiceInMemory.make

  sealed abstract class Error(message: String, cause: Option[Throwable])
      extends AppError(message, cause)

  object Error:
    sealed abstract class Create(message: String, cause: Option[Throwable])
        extends UserApplicationService.Error(message, cause)

    object Create:
      final case class AlreadyExists(id: User.Id)
          extends UserApplicationService.Error.Create(show"User with id $id already exists", none)
      final case class System(override val message: String, cause: Throwable)
          extends UserApplicationService.Error.Create(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable])
        extends UserApplicationService.Error(message, cause)

    object Get:
      final case class System(override val message: String, cause: Throwable)
          extends UserApplicationService.Error.Get(message, cause.some)

    sealed abstract class Delete(message: String, cause: Option[Throwable])
        extends UserApplicationService.Error(message, cause)

    object Delete:
      final case class NotFound(id: User.Id)
          extends UserApplicationService.Error.Delete(show"Application for user $id not found", none)

      final case class System(override val message: String, cause: Throwable)
          extends UserApplicationService.Error.Delete(message, cause.some)
