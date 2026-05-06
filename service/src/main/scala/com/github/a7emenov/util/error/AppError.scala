package com.github.a7emenov.util.error

abstract class AppError(val message: String, val causeOpt: Option[Throwable])
    extends Exception(message, causeOpt.orNull)
