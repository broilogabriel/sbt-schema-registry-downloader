lazy val root = (project in file(".")).
  settings(
    name := "sbt-schema-registry-downloader",
    version := "0.0.1-SNAPSHOT",
    organization := "io.github.broilogabriel",
    scalaVersion := "2.12.6",
    mainClass in(Compile, run) := Some("io.github.broilogabriel.Main"),
    sbtPlugin := true,
    sbtVersion := "1.1.4",
    coverageEnabled := true,
    isSnapshot := true // workaround to be able to overwrite the file when executing publishLocal
  )

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "io.argonaut" %% "argonaut" % "6.2",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.12" % Test,
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.12",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
)