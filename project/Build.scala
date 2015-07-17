import sbt._
import Keys._

object Build extends sbt.Build {
  override lazy val settings = super.settings ++ Seq(
    organization := "philcali.github.com",
    version := "1.0.0",
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
      libraryDependencies += "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.4"
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
      libraryDependencies += "net.databinder" %% "unfiltered-directives" % "0.8.3"
    )
  ) dependsOn (client, argonaut)

  lazy val server = Project(
    "carwings-server",
    file("server"),
    settings = Seq(
      libraryDependencies ++= Seq(
        "net.databinder" %% "unfiltered-jetty" % "0.8.3",
        "net.databinder" %% "unfiltered-filter" % "0.8.3"
      )
    )
  ) dependsOn (api, dynamo)
}
