package com.github.a7emenov.config

import com.github.a7emenov.domain.util.Secret
import pureconfig.ConfigReader

implicit val secretConfigReader: ConfigReader[Secret] =
  ConfigReader.stringConfigReader.map(Secret(_))
