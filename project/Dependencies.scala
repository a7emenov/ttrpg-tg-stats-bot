import sbt.*

object Dependencies {

  val bot4sTelegramCore = "com.bot4s" %% "telegram-core" % Versions.bot4sTelegram
  val cats = "org.typelevel" %% "cats-core" % Versions.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
  val fs2 = "co.fs2" %% "fs2-core" % Versions.fs2
  val sttp4 = "com.softwaremill.sttp.client4" %% "cats" % Versions.sttp4
}
