package com.github.a7emenov.process.user

import scala.util.control.NonFatal

import cats.data.EitherT
import cats.effect.{MonadCancelThrow, Ref, Resource, Sync}
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.show.*
import com.github.a7emenov.domain.user.{User, UserApplication, UserRole}
import com.github.a7emenov.domain.user.User.Id
import com.github.a7emenov.process.user
import com.github.a7emenov.process.user.UserProcess
import com.github.a7emenov.process.user.UserProcessDefault.*
import com.github.a7emenov.service.user.UserService
import com.github.a7emenov.service.userapplication
import com.github.a7emenov.service.userapplication.UserApplicationService

class UserProcessDefault[F[_]: MonadCancelThrow](
    setupTokenRef: Ref[F, Option[UserProcess.SetupToken]],
    userService: UserService[F],
    userApplicationService: UserApplicationService[F]
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

  def apply(id: User.Id, role: UserRole): F[Either[UserProcess.Error.Apply, UserApplication]] =
    (for {
      _           <- validateUserNotExists(id)
      application <- createApplication(id, role)
    } yield application).value

  override def approveApplication(id: Id): F[Either[UserProcess.Error.ApproveApplication, User]] =
    (for {
      application <- getApplication(id)
      user        <- createUser(application)
      _           <- deleteApplication(id)
    } yield user).value

  def getUserApplications: fs2.Stream[F, Either[UserProcess.Error.Get, List[UserApplication]]] =
    userApplicationService.getAll(chunkSize = 100)
      .map(_.leftMap(toGetUserApplicationsError))

  override def get(id: User.Id): F[Either[UserProcess.Error.Get, Option[User]]] =
    userService.get(id)
      .map(_.leftMap(toGetError))

  private def validateUserNotExists(id: User.Id): EitherT[F, UserProcess.Error.Apply, Unit] =
    EitherT(userService.get(id))
      .leftMap(toApplyError)
      .subflatMap {
        case Some(_) => UserProcess.Error.Apply.UserAlreadyExists(id).asLeft
        case None => ().asRight
      }

  private def createApplication(id: User.Id, role: UserRole): EitherT[F, UserProcess.Error.Apply, UserApplication] =
    EitherT(userApplicationService.create(id, role))
      .leftMap(toApplyError)

  private def getApplication(
      id: User.Id
  ): EitherT[F, UserProcess.Error.ApproveApplication, UserApplication] =
    EitherT(userApplicationService.get(id))
      .leftMap(toApproveApplicationError)
      .subflatMap {
        case Some(application) => application.asRight
        case None => UserProcess.Error.ApproveApplication.NotFound(id).asLeft
      }

  private def createUser(application: UserApplication): EitherT[F, UserProcess.Error.ApproveApplication, User] =
    EitherT(userService.create(application.id, application.role))
      .leftMap(toApproveApplicationError)

  private def deleteApplication(id: User.Id): EitherT[F, UserProcess.Error.ApproveApplication, Unit] =
    EitherT(userApplicationService.delete(id))
      .leftMap(toApproveApplicationError)

object UserProcessDefault:

  def make[F[_]: Sync](
      setupToken: UserProcess.SetupToken,
      userService: UserService[F],
      userApplicationService: UserApplicationService[F]
  ): Resource[F, UserProcess[F]] =
    Resource.eval(Ref.of(setupToken.some).map(UserProcessDefault(_, userService, userApplicationService)))

  private def toSetupAdminError(error: UserService.Error.Create): UserProcess.Error.SetupAdmin =
    error match {
      case e: UserService.Error.Create.AlreadyExists =>
        UserProcess.Error.SetupAdmin.AlreadyExists
      case cause: UserService.Error.Create.System =>
        UserProcess.Error.SetupAdmin.System("Unknown user service create error", cause)
    }

  private def toApplyError(error: UserService.Error.Get): UserProcess.Error.Apply =
    error match {
      case cause: UserService.Error.Get.System =>
        UserProcess.Error.Apply.System("Unknown user service get error", cause)
    }

  private def toApplyError(error: UserApplicationService.Error.Create): UserProcess.Error.Apply =
    error match {
      case e: UserApplicationService.Error.Create.AlreadyExists =>
        UserProcess.Error.Apply.ApplicationAlreadyExists(e.id)
      case cause: UserApplicationService.Error.Create.System =>
        UserProcess.Error.Apply.System("Unknown user application service create error", cause)
    }

  private def toApproveApplicationError(error: UserApplicationService.Error.Get): UserProcess.Error.ApproveApplication =
    error match {
      case cause: UserApplicationService.Error.Get.System =>
        UserProcess.Error.ApproveApplication.System("Unknown user application service get error", cause)
    }

  private def toApproveApplicationError(error: UserService.Error.Create): UserProcess.Error.ApproveApplication =
    error match {
      case e: UserService.Error.Create.AlreadyExists =>
        UserProcess.Error.ApproveApplication.System(show"Invalid state: user with id ${e.id} already exists", e)
      case cause: UserService.Error.Create.System =>
        UserProcess.Error.ApproveApplication.System("Unknown user service create error", cause)
    }

  private def toApproveApplicationError(error: UserApplicationService.Error.Delete)
      : UserProcess.Error.ApproveApplication =
    error match {
      case e: UserApplicationService.Error.Delete.NotFound =>
        UserProcess.Error.ApproveApplication.System(
          show"Invalid state: user application with id ${e.id} already deleted",
          e
        )
      case cause: UserApplicationService.Error.Delete.System =>
        UserProcess.Error.ApproveApplication.System("Unknown user application service create error", cause)
    }

  private def toGetError(error: UserService.Error.Get): UserProcess.Error.Get =
    error match {
      case cause: UserService.Error.Get.System =>
        UserProcess.Error.Get.System("Unknown user service get error", cause)
    }

  private def toGetUserApplicationsError(error: UserApplicationService.Error.Get): UserProcess.Error.Get =
    error match {
      case cause: UserApplicationService.Error.Get.System =>
        UserProcess.Error.Get.System("Unknown user application service get error", cause)
    }
