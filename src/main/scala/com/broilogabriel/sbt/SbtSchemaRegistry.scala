package com.broilogabriel.sbt

import com.broilogabriel.core.SchemaDownloader
import com.typesafe.config.ConfigFactory
import sbt._
import complete.DefaultParsers._

import scala.util.Try

object SbtSchemaRegistry extends AutoPlugin {

  object autoImport {
    val hello = inputKey[Unit]("Says hello!")
    val schemaRegistryDownload = inputKey[Unit]("Download schemas")

    val schemaRegistryUrl = settingKey[String]("Url to schema registry")
    val schemaRegistrySubjects = settingKey[Seq[String]]("Subject names to download")
    val schemaRegistryDebugMode = settingKey[Boolean]("Enable full stack trace")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    hello := {
      val args = spaceDelimited("").parsed
      System.out.println(s"Hellooou, ${args.head}")
    },
    schemaRegistryDownload in Compile := {
      // TODO load from config?
      // ConfigFactory.load()
      System.out.println(s"\n\nURL ${schemaRegistryUrl.value}\n")
      Try(SchemaDownloader.getUri(schemaRegistryUrl.value))
        .map(uri => SchemaDownloader.download(uri, schemaRegistrySubjects.value))
        .recover {
          case e =>
            if (schemaRegistryDebugMode.value) {
              e.printStackTrace()
            } else {
              System.err.println(s"Failed to download schemas ${e.getMessage}. Run with ")
            }
        }
      System.out.println(s"\n\n")
    },
    schemaRegistryDebugMode in schemaRegistryDownload := false
  )

}