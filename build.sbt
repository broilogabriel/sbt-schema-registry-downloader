lazy val root = (project in file(".")).
  settings(
    name := "sbt-schema-registry",
    version := "0.0.1",
    organization := "com.broilogabriel",
    scalaVersion := "2.12.6",
    mainClass in (Compile, run) := Some("com.broilogabriel.Main"),
    sbtPlugin := true,
    sbtVersion := "1.1.4"
  )