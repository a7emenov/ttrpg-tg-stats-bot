package com.github.a7emenov.process.user

import scala.util.control.NonFatal

import cats.Functor
import cats.effect.{Ref, Resource, Sync}
import cats.effect.kernel.MonadCancelThrow
import com.github.a7emenov.domain.user.{User, UserRole}
import com.github.a7emenov.process.user.UserProcess
import com.github.a7emenov.service.user.UserService
import cats.syntax.flatMap.*
import cats.syntax.option.*
import com.github.a7emenov.process.user
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.functor.*
import com.github.a7emenov.process.user.UserProcessDefault.*
import cats.syntax.either.*

class UserProcessDefault[F[_]: MonadCancelThrow](
    userService: UserService[F],
    setupTokenRef: Ref[F, Option[UserProcess.SetupToken]]
) extends UserProcess[F]:

  override def setupAdmin(id: User.Id, token: UserProcess.SetupToken): F[Either[UserProcess.Error.SetupAdmin, User]] = {
    setupTokenRef.flatModify {
      case state @ Some(setupToken) =>
        if setupToken.value == token.value then
          (
            none,
            userService.create(id, UserRole.Admin)
              .map(_.leftMap(toSetupAdminError))
              .flatTap {
                case Left(_) => setupTokenRef.set(state)
                case Right(_) => ().pure[F]
              }
              .onError {
                case NonFatal(e) => setupTokenRef.set(state)
              }
          )
        else
          (state, UserProcess.Error.SetupAdmin.WrongToken.asLeft[User].pure[F])

      case None =>
        (None, UserProcess.Error.SetupAdmin.AlreadyExists.asLeft[User].pure[F])
    }
  }

  override def create(id: User.Id, role: UserRole): F[Either[UserProcess.Error.Create, User]] =
    userService.create(id, role)
      .map(_.leftMap(toCreateError))

  override def get(id: User.Id): F[Either[UserProcess.Error.Get, Option[User]]] =
    userService.get(id)
      .map(_.leftMap(toGetError))

object UserProcessDefault:

  def make[F[_]: Sync](userService: UserService[F], setupToken: UserProcess.SetupToken): Resource[F, UserProcess[F]] =
    Resource.eval(Ref.of(setupToken.some).map(UserProcessDefault(userService, _)))

  private def toSetupAdminError(error: UserService.Error.Create): UserProcess.Error.SetupAdmin =
    error match {
      case e: UserService.Error.Create.AlreadyExists =>
        UserProcess.Error.SetupAdmin.AlreadyExists
      case cause: UserService.Error.Create.System =>
        UserProcess.Error.SetupAdmin.System("Unknown user service create error", cause)
    }

  private def toCreateError(error: UserService.Error.Create): UserProcess.Error.Create =
    error match {
      case e: UserService.Error.Create.AlreadyExists =>
        UserProcess.Error.Create.AlreadyExists(e.id)
      case cause: UserService.Error.Create.System =>
        UserProcess.Error.Create.System("Unknown user service create error", cause)
    }

  private def toGetError(error: UserService.Error.Get): UserProcess.Error.Get =
    error match {
      case cause: UserService.Error.Get.System =>
        UserProcess.Error.Get.System("Unknown user service get error", cause)
    }
