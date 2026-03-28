ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.2"

lazy val root = (project in file("."))
  .settings(
    name := "ttrpg-tg-stats-bot"
  )
  .aggregate(service)

lazy val service = (project in file("service"))
  .settings(
    name := "service"
  )

