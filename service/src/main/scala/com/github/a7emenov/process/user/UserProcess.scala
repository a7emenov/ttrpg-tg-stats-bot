package com.github.a7emenov.process.user

import cats.effect.{Resource, Sync}
import cats.syntax.option.*
import cats.syntax.show.*
import com.github.a7emenov.config.process.UserProcessConfig
import com.github.a7emenov.domain.user.{User, UserApplication, UserRole}
import com.github.a7emenov.domain.util.Secret
import com.github.a7emenov.process.user
import com.github.a7emenov.service.user.UserService
import com.github.a7emenov.service.userapplication.UserApplicationService
import com.github.a7emenov.util.error.AppError

trait UserProcess[F[_]]:

  def setupAdmin(id: User.Id, token: UserProcess.SetupToken): F[Either[UserProcess.Error.SetupAdmin, User]]

  def apply(id: User.Id, role: UserRole): F[Either[UserProcess.Error.Apply, UserApplication]]

  def approveApplication(id: User.Id): F[Either[UserProcess.Error.ApproveApplication, User]]

  def getUserApplications: fs2.Stream[F, Either[UserProcess.Error.Get, List[UserApplication]]]

  def get(id: User.Id): F[Either[UserProcess.Error.Get, Option[User]]]

object UserProcess:

  def make[F[_]: Sync](
      config: UserProcessConfig,
      userService: UserService[F],
      userApplicationService: UserApplicationService[F]
  ): Resource[F, UserProcess[F]] =
    UserProcessDefault.make(config.setupToken, userService, userApplicationService)

  opaque type SetupToken = Secret

  object SetupToken:

    def fromString(value: String): UserProcess.SetupToken =
      UserProcess.SetupToken(Secret(value))

    def apply(value: Secret): UserProcess.SetupToken =
      value

    extension (token: UserProcess.SetupToken)
      def value: Secret = token

  sealed abstract class Error(message: String, cause: Option[Throwable])
      extends AppError(message, cause)

  object Error:
    sealed abstract class SetupAdmin(message: String, cause: Option[Throwable])
        extends UserProcess.Error(message, cause)

    object SetupAdmin:
      case object InvalidToken
          extends UserProcess.Error.SetupAdmin(show"Invalid setup token", none)
      case object AlreadyExists
          extends UserProcess.Error.SetupAdmin(show"An admin account already exists", none)
      final case class System(override val message: String, cause: Throwable)
          extends UserProcess.Error.SetupAdmin(message, cause.some)

    sealed abstract class Apply(message: String, cause: Option[Throwable])
        extends UserProcess.Error(message, cause)

    object Apply:
      final case class UserAlreadyExists(id: User.Id)
          extends UserProcess.Error.Apply(show"User with id $id already exists", none)
      final case class ApplicationAlreadyExists(id: User.Id)
          extends UserProcess.Error.Apply(show"Application for user with id $id already exists", none)
      final case class System(override val message: String, cause: Throwable)
          extends UserProcess.Error.Apply(message, cause.some)

    sealed abstract class ApproveApplication(message: String, cause: Option[Throwable])
        extends UserProcess.Error(message, cause)

    object ApproveApplication:
      final case class NotFound(id: User.Id)
          extends UserProcess.Error.ApproveApplication(show"Application for user $id not found", none)
      final case class System(override val message: String, cause: Throwable)
          extends UserProcess.Error.ApproveApplication(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable])
        extends UserProcess.Error(message, cause)

    object Get:
      case class System(override val message: String, cause: Throwable)
          extends UserProcess.Error.Get(message, cause.some)
