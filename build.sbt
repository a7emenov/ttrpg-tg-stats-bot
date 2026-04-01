ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.2"

val commonSettings = Seq(
  scalafmtOnCompile := true
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "ttrpg-tg-stats-bot"
  )
  .aggregate(service)

lazy val service = (project in file("service"))
  .settings(commonSettings)
  .settings(
    name := "service",
    libraryDependencies ++= Seq(
      Dependencies.bot4sTelegramCore,
      Dependencies.cats,
      Dependencies.catsEffect,
      Dependencies.sttp4
    )
  )

