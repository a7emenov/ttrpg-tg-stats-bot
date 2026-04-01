package com.github.a7emenov.service.user

import cats.Functor
import cats.effect.Ref
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.domain.user.User.Id
import cats.syntax.either.*
import com.github.a7emenov.service.user.UserService.Error
import cats.syntax.functor.*

class UserServiceInMemory[F[_]: Functor](ref: Ref[F, Map[User.Id, User]]) extends UserService[F]:

  override def create(id: Id): F[Either[UserService.Error.Create, User]] =
    ref.modify { map =>
      if map.contains(id) then
        (map, UserService.Error.Create.AlreadyExists(id).asLeft)
      else
        val user = User(id)
        (map.updated(id, user), user.asRight)
    }

  override def get(id: Id): F[Either[Error.Get, Option[User]]] =
    ref.get.map { map =>
      map.get(id).asRight
    }
