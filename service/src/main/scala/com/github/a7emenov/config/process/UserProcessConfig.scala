package com.github.a7emenov.config.process

import com.github.a7emenov.process.user.UserProcess
import pureconfig.ConfigReader
import com.github.a7emenov.config.secretConfigReader

case class UserProcessConfig(setupToken: UserProcess.SetupToken) derives ConfigReader

object UserProcessConfig:

  implicit val setupTokenConfigReader: ConfigReader[UserProcess.SetupToken] =
    secretConfigReader.map(UserProcess.SetupToken(_))
