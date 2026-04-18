package com.github.a7emenov.api.telegram

enum BotCommand(val name: String):
  case Echo extends BotCommand("echo")
  case SetupAdmin extends BotCommand("setup_admin")
  case CheckPermissions extends BotCommand("check_permissions")
  case Apply extends BotCommand("apply")
  case ApproveUserApplication extends BotCommand("approve_user_application")
  case ListUserApplications extends BotCommand("list_user_applications")
