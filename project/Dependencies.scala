import sbt._

object Dependencies {

  private val akkaOrg = "com.typesafe.akka"

  private val akkaVersion = "2.4.20"
  private val akkaHttpVersion = "10.0.11"

  private val scalatestVersion = "3.0.5"

  val libDeps = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.log4s" %% "log4s" % "1.6.0",

    akkaOrg %% "akka-actor" % akkaVersion,
    akkaOrg %% "akka-http" % akkaHttpVersion,


    // Test dependencies
    akkaOrg %% "akka-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )
}
