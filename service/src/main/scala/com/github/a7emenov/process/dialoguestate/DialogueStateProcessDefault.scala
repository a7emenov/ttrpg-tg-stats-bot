package com.github.a7emenov.process.dialoguestate

import cats.Functor
import cats.effect.Resource
import cats.syntax.either.*
import cats.syntax.functor.*
import com.github.a7emenov.domain.dialoguestate.DialogueState
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.process.dialoguestate
import com.github.a7emenov.process.dialoguestate.DialogueStateProcessDefault.*
import com.github.a7emenov.service
import com.github.a7emenov.service.dialoguestate.DialogueStateService

class DialogueStateProcessDefault[F[_]: Functor](
    dialogueStateService: DialogueStateService[F],
) extends DialogueStateProcess[F]:

  override def update(id: User.Id, state: DialogueState): F[Either[DialogueStateProcess.Error.Update, DialogueState]] =
    dialogueStateService.update(id, state)
      .map(_.leftMap(toUpdateError))

  override def get(id: User.Id): F[Either[DialogueStateProcess.Error.Get, DialogueState]] =
    dialogueStateService.get(id)
      .map(_.leftMap(toGetError))

object DialogueStateProcessDefault:

  def make[F[_]: Functor](dialogueStateService: DialogueStateService[F]): Resource[F, DialogueStateProcess[F]] =
    Resource.pure(DialogueStateProcessDefault(dialogueStateService))

  private def toUpdateError(error: DialogueStateService.Error.Update): DialogueStateProcess.Error.Update =
    error match {
      case cause: DialogueStateService.Error.Update.System =>
        DialogueStateProcess.Error.Update.System("Unknown dialogue state service record error", cause)
    }

  private def toGetError(error: DialogueStateService.Error.Get): DialogueStateProcess.Error.Get =
    error match {
      case cause: DialogueStateService.Error.Get.System =>
        DialogueStateProcess.Error.Get.System("Unknown game session service get error", cause)
    }
