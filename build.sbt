import sbt.Keys.{ organization, publishArtifact }

lazy val root = (project in file(".")).
  settings(
    name := "sbt-schema-registry-downloader",
    organization := "io.github.broilogabriel",
    description := "Sbt plugin to download avro schemas from schema registry",
    version := "0.1.0-SNAPSHOT",
    //    scalaVersion := "2.12.6",
    scalaVersion := appConfiguration.value.provider.scalaProvider.version,
    mainClass in(Compile, run) := Some("io.github.broilogabriel.Main"),
    sbtPlugin := true,
    sbtVersion := "1.2.8",
    coverageEnabled := true,
    // workaround to be able to overwrite the file when executing publishLocal
    isSnapshot := version.value.trim.endsWith("SNAPSHOT"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/broilogabriel/sbt-schema-registry-downloader")),
    pomExtra := {
      <scm>
        <url>git://github.com/broilogabriel/sbt-schema-registry-downloader.git</url>
        <connection>scm:git://github.com/broilogabriel/sbt-schema-registry-downloader.git</connection>
      </scm>
        <developers>
          <developer>
            <id>broilogabriel</id>
            <name>Gabriel Broilo</name>
            <url>http://github.com/broilogabriel</url>
          </developer>
        </developers>
    }
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
