package com.github.a7emenov.service.dialoguestate

import cats.Functor
import cats.effect.{Ref, Resource, Sync}
import cats.syntax.either.*
import cats.syntax.functor.*
import com.github.a7emenov.domain.dialoguestate.DialogueState
import com.github.a7emenov.domain.user.{User, UserRole}
import com.github.a7emenov.service.user.UserService.Error

class DialogueStateServiceInMemory[F[_]: Functor](ref: Ref[F, Map[User.Id, DialogueState]])
    extends DialogueStateService[F]:

  def update(id: User.Id, state: DialogueState): F[Either[DialogueStateService.Error.Update, DialogueState]] =
    ref.modify { map =>
      (map.updated(id, state), state.asRight)
    }

  override def get(id: User.Id): F[Either[DialogueStateService.Error.Get, DialogueState]] =
    ref.get.map { map =>
      map.getOrElse(id, DialogueState.Empty).asRight
    }

object DialogueStateServiceInMemory:

  def make[F[_]: Sync]: Resource[F, DialogueStateService[F]] =
    Resource.eval(Ref.of(Map.empty[User.Id, DialogueState]).map(DialogueStateServiceInMemory(_)))
