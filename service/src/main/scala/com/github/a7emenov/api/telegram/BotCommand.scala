package com.github.a7emenov.api.telegram

enum BotCommand(val name: String):
  case Echo extends BotCommand("echo")
  case GetUser extends BotCommand("get_user")
