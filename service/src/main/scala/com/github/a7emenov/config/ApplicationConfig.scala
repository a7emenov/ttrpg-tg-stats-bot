package com.github.a7emenov.config

import cats.effect.kernel.Sync
import com.github.a7emenov.config.bot.BotConfig
import com.github.a7emenov.config.process.ProcessConfig
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.ConfigReader.Result
import pureconfig.error.ConfigReaderFailures

case class ApplicationConfig(
    bot: BotConfig,
    process: ProcessConfig
) derives ConfigReader

object ApplicationConfig:

  def read[F[_]: Sync]: F[ApplicationConfig] =
    Sync[F].delay {
      ConfigSource.default.loadOrThrow[ApplicationConfig]
    }
