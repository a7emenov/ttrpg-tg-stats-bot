package com.github.a7emenov.process.dialoguestate

import cats.effect.{Resource, Sync}
import cats.syntax.option.*
import com.github.a7emenov.domain.dialoguestate.DialogueState
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.service.dialoguestate.DialogueStateService
import com.github.a7emenov.util.error.AppError

trait DialogueStateProcess[F[_]]:

  // todo: refactor to prevent concurrent updates
  def update(id: User.Id, state: DialogueState): F[Either[DialogueStateProcess.Error.Update, DialogueState]]

  def get(id: User.Id): F[Either[DialogueStateProcess.Error.Get, DialogueState]]

object DialogueStateProcess:

  def make[F[_]: Sync](service: DialogueStateService[F]): Resource[F, DialogueStateProcess[F]] =
    DialogueStateProcessDefault.make(service)

  sealed abstract class Error(message: String, cause: Option[Throwable])
      extends AppError(message, cause)

  object Error:
    sealed abstract class Update(message: String, cause: Option[Throwable])
        extends DialogueStateProcess.Error(message, cause)

    object Update:
      final case class System(override val message: String, cause: Throwable)
          extends DialogueStateProcess.Error.Update(message, cause.some)

    sealed abstract class Get(message: String, cause: Option[Throwable])
        extends DialogueStateProcess.Error(message, cause)

    object Get:
      final case class System(override val message: String, cause: Throwable)
          extends DialogueStateProcess.Error.Get(message, cause.some)
