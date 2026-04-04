package com.github.a7emenov

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.Resource
import com.github.a7emenov.api.telegram.BotCommandHandler
import com.github.a7emenov.process.user.UserProcess
import com.github.a7emenov.service.user.UserService
import sttp.client4.httpclient.cats.HttpClientCatsBackend

object Main extends IOApp:

  private val botToken: String = ""

  override def run(args: List[String]): IO[ExitCode] =
    program.use { handler =>
      handler.run().as(ExitCode.Success)
    }

  private def program: Resource[IO, BotCommandHandler[IO]] =
    for {
      httpBackend <- HttpClientCatsBackend.resource()
      userService <- UserService.make[IO]
      userProgram <- UserProcess.make(userService, UserProcess.SetupToken(botToken))
      handler = BotCommandHandler(botToken, httpBackend, userProgram)
    } yield handler
