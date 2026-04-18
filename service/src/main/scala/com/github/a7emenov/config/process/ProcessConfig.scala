package com.github.a7emenov.config.process

import pureconfig.ConfigReader

case class ProcessConfig(user: UserProcessConfig) derives ConfigReader
