package com.github.a7emenov.api.telegram.callback

import scala.util.Try

import BotCommandCallbackData.HandlerTag.formatString

/** A helper object for creating callback data across multiple handlers.
 */
object BotCommandCallbackData:

  /** A tag to uniquely identify a callback handler (a class or a trait that handles callbacks for a given sub-domain)
   *  through [[uniquePrefix]].
   *  Handlers are responsible for distinguishing between commands by using [[CommandTag]].
   *
   *  In principle, tag definitions should not be rearranged as tag creation depends on [[ordinal]].
   *  The assumption is there will never be more than 1000 handler tags.
   */
  enum HandlerTag:
    case GameSession extends BotCommandCallbackData.HandlerTag

    val uniquePrefix: String =
      formatString.format(ordinal)

  object HandlerTag:
    // Has to be lazy due to enum initialization, see:
    // https://users.scala-lang.org/t/unexpected-null-when-scala-3-enum-references-a-member-from-its-companion-object/10146/7
    private[BotCommandCallbackData] lazy val uniquePrefixLength: Int = 3

    private lazy val formatString =
      s"%0${uniquePrefixLength}d"

  /** This function is expected to align with [[com.bot4s.telegram.api.declarative.Callbacks.prefixTag]].
   *  Any changes to this function should be reflected in [[BotCommandCallbackData.unapply]] and vice versa.
   *  Additionally, Callback data must not exceed 64 bytes in total.
   *
   *  @param handlerTag - a tag to uniquely identify the command handler.
   *  @param commandTag - a string corresponding to the command being processed (at the discretion of the handler).
   *  @param data - additional data to forward to the callback (will be separated by commas).
   */
  def apply(handlerTag: BotCommandCallbackData.HandlerTag, commandTag: String, data: String*): String =
    s"${handlerTag.uniquePrefix}$commandTag,${data.mkString(",")}"

  def unapply(rawData: String): Option[(BotCommandCallbackData.HandlerTag, String, List[String])] =
    val handlerTagOpt = rawData.take(BotCommandCallbackData.HandlerTag.uniquePrefixLength)
      .toIntOption
      .flatMap(i => Try(BotCommandCallbackData.HandlerTag.fromOrdinal(i)).toOption)

    for {
      handlerTag <- rawData.take(BotCommandCallbackData.HandlerTag.uniquePrefixLength)
        .toIntOption
        .flatMap(i => Try(BotCommandCallbackData.HandlerTag.fromOrdinal(i)).toOption)
      commandTagAndData = rawData.drop(BotCommandCallbackData.HandlerTag.uniquePrefixLength).split(",")
      commandTag <- commandTagAndData.headOption
      data = commandTagAndData.tail.toList
    } yield (handlerTag, commandTag, data)
