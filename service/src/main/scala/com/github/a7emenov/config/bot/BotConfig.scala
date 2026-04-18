package com.github.a7emenov.config.bot

import com.github.a7emenov.domain.util.Secret
import pureconfig.ConfigReader
import com.github.a7emenov.config.secretConfigReader

case class BotConfig(token: Secret) derives ConfigReader
