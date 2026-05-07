package com.github.a7emenov.api.telegram.dialoguestate

import cats.MonadThrow
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.bot4s.telegram.api.declarative.Action
import com.github.a7emenov.domain.dialoguestate.DialogueState
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.process.dialoguestate.DialogueStateProcess

trait BotDialogueStateHandler[F[_]]:

  implicit val monad: MonadThrow[F]

  protected def dialogueStateProcess: DialogueStateProcess[F]

  // todo: ensure this locks the state during processing.
  /** Proceeds only on state matched by the partial function. Non-matching states are ignored.
   *  New state returned from `action` is automatically updated.
   *
   *  Dependent state transitions should be handled within a single message/callback handler to avoid cases
   *  when a message is handled twice, i.e.
   *  onMessage {
   *   onDialogueState {
   *     case state A => transition to state B
   *     case state B => transition to state C
   *   }
   *  }
   *
   *  and not
   *  onMessage {
   *   onDialogueState {
   *     case state A => transition to state B
   *   }
   *  }
   *
   *  onMessage {
   *   onDialogueState {
   *     case state B => transition to state C
   *   }
   *  }
   */
  def onDialogueState(userId: User.Id)(action: PartialFunction[
    DialogueState,
    F[DialogueState]
  ])(onError: Action[F, DialogueStateProcess.Error.Get] = _ => ().pure[F]): F[Unit] =
    dialogueStateProcess.get(userId).flatMap {
      case Right(state) =>
        if action.isDefinedAt(state) then
          action(state)
            .flatMap(dialogueStateProcess.update(userId, _))
            .void
        else
          ().pure[F]

      case Left(error) =>
        onError(error)
    }
