
lazy val root = (project in file("."))
  .settings(
    name := "stockdemo",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.3",

    libraryDependencies := Dependencies.libDeps
  )
