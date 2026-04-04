package com.github.a7emenov.process.user

import cats.effect.{Resource, Sync}
import cats.syntax.option.*
import cats.syntax.show.*
import com.github.a7emenov.domain.user.{User, UserRole}
import com.github.a7emenov.process.user
import com.github.a7emenov.service.user.UserService

trait UserProcess[F[_]]:

  def setupAdmin(id: User.Id, token: UserProcess.SetupToken): F[Either[UserProcess.Error.SetupAdmin, User]]

  def create(id: User.Id, role: UserRole): F[Either[UserProcess.Error.Create, User]]

  def get(id: User.Id): F[Either[UserProcess.Error.Get, Option[User]]]

object UserProcess:

  def make[F[_]: Sync](userService: UserService[F], setupToken: UserProcess.SetupToken): Resource[F, UserProcess[F]] =
    UserProcessDefault.make(userService, setupToken)

  opaque type SetupToken = String

  object SetupToken:

    def apply(value: String): UserProcess.SetupToken =
      value

    extension (token: UserProcess.SetupToken)
      def value: String = token

  sealed abstract class Error(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull)

  object Error:
    sealed abstract class SetupAdmin(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object SetupAdmin:
      case object WrongToken extends UserProcess.Error.SetupAdmin(show"Wrong setup token", none)
      case object AlreadyExists
          extends UserProcess.Error.SetupAdmin(show"An admin account already exists", none)

      case class System(message: String, cause: Throwable) extends UserProcess.Error.SetupAdmin(message, cause.some)

    sealed abstract class Create(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Create:
      case class AlreadyExists(id: User.Id)
          extends UserProcess.Error.Create(show"User with id $id already exists", none)
      case class System(message: String, cause: Throwable) extends UserProcess.Error.Create(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable]) extends Error(message, cause)

    object Get:
      case class System(message: String, cause: Throwable) extends UserProcess.Error.Get(message, cause.some)
