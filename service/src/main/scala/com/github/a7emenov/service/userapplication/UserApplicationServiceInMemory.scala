package com.github.a7emenov.service.userapplication

import cats.Functor
import cats.effect.{Ref, Resource, Sync}
import cats.syntax.either.*
import cats.syntax.functor.*
import com.github.a7emenov.domain.user.{User, UserApplication, UserRole}
import com.github.a7emenov.service.userapplication

class UserApplicationServiceInMemory[F[_]: Functor](ref: Ref[F, Map[User.Id, UserApplication]])
    extends UserApplicationService[F]:

  override def create(id: User.Id, role: UserRole): F[Either[UserApplicationService.Error.Create, UserApplication]] =
    ref.modify { map =>
      if map.contains(id) then
        (map, UserApplicationService.Error.Create.AlreadyExists(id).asLeft)
      else
        val application = UserApplication(id, role)
        (map.updated(id, application), application.asRight)
    }

  override def get(id: User.Id): F[Either[UserApplicationService.Error.Get, Option[UserApplication]]] =
    ref.get.map { map =>
      map.get(id).asRight
    }

  def getAll(chunkSize: Int): fs2.Stream[F, Either[UserApplicationService.Error.Get, List[UserApplication]]] =
    fs2.Stream.eval(ref.get.map(_.values.toList.asRight[UserApplicationService.Error.Get]))

  def delete(id: User.Id): F[Either[UserApplicationService.Error.Delete, Unit]] =
    ref.modify { map =>
      if map.contains(id) then
        (map.removed(id), ().asRight)
      else
        (map, UserApplicationService.Error.Delete.NotFound(id).asLeft)
    }

object UserApplicationServiceInMemory:

  def make[F[_]: Sync]: Resource[F, UserApplicationService[F]] =
    Resource.eval(Ref.of(Map.empty[User.Id, UserApplication]).map(UserApplicationServiceInMemory(_)))
