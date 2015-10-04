import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object Build extends sbt.Build {
  override lazy val settings = super.settings ++ Seq(
    organization := "philcali.github.com",
    version := "1.3.0",
    scalaVersion := "2.11.5",
    scalacOptions ++= Seq("-feature", "-deprecation")
  )

  lazy val root = Project(
    "carwings",
    file(".")
  ) aggregate (data, client, dynamo, api, server)

  lazy val data = Project(
    "carwings-data",
    file("data")
  )

  lazy val argonaut = Project(
    "carwings-argonaut",
    file("argonaut"),
    settings = Seq(
      libraryDependencies += "io.argonaut" %% "argonaut" % "6.0.4"
    )
  ) dependsOn data

  lazy val dynamo = Project(
    "carwings-dynamo",
    file("dynamo"),
    settings = Seq(
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-log4j12" % "1.7.12",
        "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.4"
      )
    )
  ) dependsOn data

  lazy val client = Project(
    "carwings-client",
    file("client"),
    settings = Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
        "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
      )
    )
  ) dependsOn data

  lazy val api = Project(
    "carwings-api",
    file("api"),
    settings = Seq(
      libraryDependencies += "net.databinder" %% "unfiltered-directives" % "0.8.4"
    )
  ) dependsOn (client, argonaut)

  lazy val bulletData = Project(
    "carwings-pushbullet-data",
    file("bullet-data")
  )

  lazy val bulletArgonaut = Project(
    "carwings-pushbullet-argonaut",
    file("bullet-argonaut"),
    settings = Seq(
      libraryDependencies += "io.argonaut" %% "argonaut" % "6.0.4"
    )
  ) dependsOn bulletData

  lazy val bulletClient = Project(
    "carwings-pushbullet-client",
    file("bullet-client"),
    settings = Seq(
      libraryDependencies ++= Seq(
        "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
      )
    )
  ) dependsOn bulletArgonaut

  lazy val bulletApi = Project(
    "carwings-pushbullet-api",
    file("bullet-api"),
    settings = Seq(
      libraryDependencies += "net.databinder" %% "unfiltered-directives" % "0.8.4"
    )
  ) dependsOn bulletClient

  lazy val bulletDynamo = Project(
    "carwings-pushbullet-dynamo",
    file("bullet-dynamo"),
    settings = Seq(
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-log4j12" % "1.7.12",
        "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.4"
      )
    )
  ) dependsOn bulletData

  lazy val server = Project(
    "carwings-server",
    file("server"),
    settings = assemblySettings ++ Seq(
      libraryDependencies ++= Seq(
        "net.databinder" %% "unfiltered-jetty" % "0.8.4",
        "net.databinder" %% "unfiltered-filter" % "0.8.4"
      )
    )
  ) dependsOn (api, dynamo, bulletApi, bulletDynamo)
}
