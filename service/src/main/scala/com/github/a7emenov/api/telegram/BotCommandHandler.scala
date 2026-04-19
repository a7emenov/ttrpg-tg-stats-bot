package com.github.a7emenov.api.telegram

import cats.effect.Async
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.github.a7emenov.domain.util.Secret
import com.github.a7emenov.process.user.UserProcess
import sttp.client4.Backend

class BotCommandHandler[F[_]](
    token: Secret,
    backend: Backend[F],
    protected val userProcess: UserProcess[F]
)(implicit F: Async[F]) extends TelegramBot[F](token.value, backend) with Polling[F] with BotUserCommandHandler[F] {

  override implicit val async: Async[F] = F
}
