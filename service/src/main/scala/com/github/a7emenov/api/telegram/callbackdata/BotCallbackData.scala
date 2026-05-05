package com.github.a7emenov.api.telegram.callbackdata

import scala.util.Try

import BotCallbackData.HandlerTag.formatString

/** A helper object for creating callback data across multiple handlers.
 */
object BotCallbackData:

  /** A tag to uniquely identify a callback handler (a class or a trait that handles callbacks for a given sub-domain)
   *  through [[uniquePrefix]].
   *  Handlers are responsible for distinguishing between callback by using handler-specific callback tags.
   *
   *  In principle, tag definitions should not be rearranged as tag creation depends on [[ordinal]].
   *  The assumption is there will never be more than 1000 handler tags.
   */
  enum HandlerTag:
    case GameSession extends BotCallbackData.HandlerTag

    val uniquePrefix: String =
      formatString.format(ordinal)

  object HandlerTag:
    // Has to be lazy due to enum initialization, see:
    // https://users.scala-lang.org/t/unexpected-null-when-scala-3-enum-references-a-member-from-its-companion-object/10146/7
    private[BotCallbackData] lazy val uniquePrefixLength: Int = 3

    private lazy val formatString =
      s"%0${uniquePrefixLength}d"

  /** This function is expected to align with [[com.bot4s.telegram.api.declarative.Callbacks.prefixTag]].
   *  Any changes to this function should be reflected in [[BotCallbackData.unapply]] and vice versa.
   *  Additionally, callback data must not exceed 64 bytes in total.
   *
   *  @param handlerTag - a tag to uniquely identify the command handler.
   *  @param callbackTag - a tag corresponding to the callback being processed (at the discretion of the handler).
   *  @param data - additional data to forward to the callback (will be separated by commas).
   */
  def apply(handlerTag: BotCallbackData.HandlerTag, callbackTag: String, data: String*): String =
    s"${handlerTag.uniquePrefix}$callbackTag,${data.mkString(",")}"

  def unapply(rawData: String): Option[(BotCallbackData.HandlerTag, String, List[String])] =
    val handlerTagOpt = rawData.take(BotCallbackData.HandlerTag.uniquePrefixLength)
      .toIntOption
      .flatMap(i => Try(BotCallbackData.HandlerTag.fromOrdinal(i)).toOption)

    for {
      handlerTag <- rawData.take(BotCallbackData.HandlerTag.uniquePrefixLength)
        .toIntOption
        .flatMap(i => Try(BotCallbackData.HandlerTag.fromOrdinal(i)).toOption)
      callbackTagAndData = rawData.drop(BotCallbackData.HandlerTag.uniquePrefixLength).split(",")
      callbackTag <- callbackTagAndData.headOption
      data = callbackTagAndData.tail.toList
    } yield (handlerTag, callbackTag, data)
