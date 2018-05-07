lazy val root = (project in file(".")).
  settings(
    name := "sbt-schema-registry",
    version := "0.0.1-SNAPSHOT",
    organization := "com.broilogabriel",
    scalaVersion := "2.12.6",
    mainClass in(Compile, run) := Some("com.broilogabriel.Main"),
    sbtPlugin := true,
    sbtVersion := "1.1.4",
    isSnapshot := true // workaround to be able to overwrite the file when executing publishLocal
  )

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "io.argonaut" %% "argonaut" % "6.2",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.12" % Test
)