package com.github.a7emenov.service.user

import cats.Functor
import cats.effect.{Ref, Sync}
import cats.effect.Resource
import com.github.a7emenov.domain.user.{User, UserRole}
import cats.syntax.either.*
import cats.syntax.functor.*

class UserServiceInMemory[F[_]: Functor](ref: Ref[F, Map[User.Id, User]]) extends UserService[F]:

  override def create(id: User.Id, role: UserRole): F[Either[UserService.Error.Create, User]] =
    ref.modify { map =>
      if map.contains(id) then
        (map, UserService.Error.Create.AlreadyExists(id).asLeft)
      else
        val user = User(id, role)
        (map.updated(id, user), user.asRight)
    }

  override def get(id: User.Id): F[Either[UserService.Error.Get, Option[User]]] =
    ref.get.map { map =>
      map.get(id).asRight
    }

object UserServiceInMemory:

  def make[F[_]: Sync]: Resource[F, UserService[F]] =
    Resource.eval(Ref.of(Map.empty[User.Id, User]).map(UserServiceInMemory(_)))
