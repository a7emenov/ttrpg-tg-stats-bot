package com.github.a7emenov.domain.user

enum UserRole(val defaultPermissions: Set[UserPermission]):
  /** Represents a user who has no access to record sessions,
   *  but may view recorded sessions.
   */
  case Reader extends UserRole(Set(UserPermission.ViewGameSessions))

  /** Represents a user who can view and record new sessions.
   */
  case Scribe extends UserRole(Set(UserPermission.ViewGameSessions, UserPermission.RecordGameSessions))

  /** Represents a user who has access to all parts of the application.
   */
  case Admin extends UserRole(UserPermission.values.toSet)
