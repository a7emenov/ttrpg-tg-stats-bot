package com.github.a7emenov.api.telegram.command

enum BotCommand(val name: String):
  case SetupAdmin extends BotCommand("setup_admin")
  case CheckPermissions extends BotCommand("check_permissions")
  case Apply extends BotCommand("apply")
  case ApproveUserApplication extends BotCommand("approve_user_application")
  case ListUserApplications extends BotCommand("list_user_applications")
  case RecordGameSession extends BotCommand("record")
  case ListGames extends BotCommand("list_games")
  case RemoveKeyboard extends BotCommand("remove_keyboard")
