package com.github.a7emenov

import cats.effect.{ExitCode, IO, IOApp}
import com.github.a7emenov.api.telegram.BotCommandHandler
import com.github.a7emenov.domain.user.User
import com.github.a7emenov.service.user.UserService
import sttp.client4.httpclient.cats.HttpClientCatsBackend

object Main extends IOApp:

  private val botToken: String = ""

  override def run(args: List[String]): IO[ExitCode] = {
    HttpClientCatsBackend.resource().use { backend =>
      for {
        userService <- UserService.make[IO]
        _           <- userService.create(User.Id("test_id"))
        handler = BotCommandHandler(botToken, backend, userService)
        result <- handler.startPolling()
      } yield ExitCode.Success
    }

  }
