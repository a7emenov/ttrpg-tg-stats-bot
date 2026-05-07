package com.github.a7emenov

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.Resource
import com.github.a7emenov.api.telegram.BotCommandHandler
import com.github.a7emenov.config.ApplicationConfig
import com.github.a7emenov.process.dialoguestate.DialogueStateProcess
import com.github.a7emenov.process.gamesession.GameSessionProcess
import com.github.a7emenov.process.user.UserProcess
import com.github.a7emenov.service.dialoguestate.DialogueStateService
import com.github.a7emenov.service.gamesession.GameSessionService
import com.github.a7emenov.service.user.UserService
import com.github.a7emenov.service.userapplication.UserApplicationService
import sttp.client4.httpclient.cats.HttpClientCatsBackend

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- ApplicationConfig.read[IO]
      _      <- IO.delay(println(config))
      result <- program(config).use(_.run())
    } yield ExitCode.Success

  private def program(config: ApplicationConfig): Resource[IO, BotCommandHandler[IO]] =
    for {
      httpBackend            <- HttpClientCatsBackend.resource()
      userService            <- UserService.make[IO]
      userApplicationService <- UserApplicationService.make[IO]
      userProcess            <- UserProcess.make(config.process.user, userService, userApplicationService)
      gameSessionService     <- GameSessionService.make[IO]
      gameSessionProcess     <- GameSessionProcess.make(gameSessionService)
      dialogueStateService   <- DialogueStateService.make[IO]
      dialogueStateProcess   <- DialogueStateProcess.make(dialogueStateService)
      handler = BotCommandHandler(config.bot.token, httpBackend, userProcess, gameSessionProcess, dialogueStateProcess)
    } yield handler
