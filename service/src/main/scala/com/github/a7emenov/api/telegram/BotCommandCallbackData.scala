package com.github.a7emenov.api.telegram

/** A helper object for creating callback data unique across multiple handlers.
 */
object BotCommandCallbackData:

  enum HandlerTag(val name: String):
    case GameSession extends BotCommandCallbackData.HandlerTag("game_session")

  /** This function and functions relying on it should align
   *  with the implementation of [[com.bot4s.telegram.api.declarative.Callbacks.prefixTag]]
   *  Callback data must not exceed 64 bytes in total.
   */
  def tag(handler: BotCommandCallbackData.HandlerTag, commandTag: String): String =
    handler.name + commandTag

  /** This function and functions relying on it should align
   *  with the implementation of [[com.bot4s.telegram.api.declarative.Callbacks.prefixTag]]
   *  Callback data must not exceed 64 bytes in total (handler + commandTag + data).
   */
  def of(handler: BotCommandCallbackData.HandlerTag, commandTag: String, data: String): String =
    of(wholeTag = tag(handler, commandTag), data = data)

  /** This function and functions relying on it should align
   *  with the implementation of [[com.bot4s.telegram.api.declarative.Callbacks.prefixTag]]
   *  Callback data must not exceed 64 bytes in total (wholeTag + data).
   */
  def of(wholeTag: String, data: String): String =
    wholeTag + data
