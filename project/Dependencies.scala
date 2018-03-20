import sbt._

object Dependencies {

  val libDeps = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.log4s" %% "log4s" % "1.6.0"
  )
}
