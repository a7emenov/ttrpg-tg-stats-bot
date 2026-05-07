package com.github.a7emenov.service.dialoguestate

import cats.effect.{Resource, Sync}
import cats.syntax.option.*
import cats.syntax.show.*
import com.github.a7emenov.domain.dialoguestate.DialogueState
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.util.error.AppError

trait DialogueStateService[F[_]]:

  // todo: refactor to prevent concurrent updates
  def update(id: User.Id, state: DialogueState): F[Either[DialogueStateService.Error.Update, DialogueState]]

  def get(id: User.Id): F[Either[DialogueStateService.Error.Get, DialogueState]]

object DialogueStateService:

  def make[F[_]: Sync]: Resource[F, DialogueStateService[F]] =
    DialogueStateServiceInMemory.make

  sealed abstract class Error(message: String, cause: Option[Throwable])
      extends AppError(message, cause)

  object Error:
    sealed abstract class Update(message: String, cause: Option[Throwable])
        extends DialogueStateService.Error(message, cause)

    object Update:
      final case class System(override val message: String, cause: Throwable)
          extends DialogueStateService.Error.Update(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable])
        extends DialogueStateService.Error(message, cause)

    object Get:
      final case class System(override val message: String, cause: Throwable)
          extends DialogueStateService.Error.Get(message, cause.some)
