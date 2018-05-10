package com.broilogabriel.sbt

import com.broilogabriel.core.SchemaDownloader
import sbt._
import complete.DefaultParsers._

import scala.util.Try

object SbtSchemaRegistry extends AutoPlugin {

  object autoImport {
    val hellow = inputKey[Unit]("Says hello!")
    val schemaRegistryDownload = inputKey[Unit]("Download schemas")

    val schemaRegistryUrl = settingKey[String]("Url to schema registry")
    val schemaRegistryTargetFolder = settingKey[String]("Target for storing the avro schemas")
    val schemaRegistrySubjects = settingKey[Seq[String]]("Subject names to download")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    hellow := {
      val args = spaceDelimited("").parsed
      System.out.println(s"Helalooou, ${args.head}")
    },
    schemaRegistryDownload := {
      System.out.println(s"\n\nURL ${schemaRegistryUrl.value}\n")
      Try(SchemaDownloader.getUri(schemaRegistryUrl.value))
        .map {
          uri =>
            SchemaDownloader(uri, schemaRegistrySubjects.value, schemaRegistryTargetFolder.value).download()
        }
        .recover {
          case e =>
              System.err.println(s"Failed to download schemas ${e.getMessage}. Run with ")
              e.printStackTrace()
        }
      System.out.println(s"\n\n")
    }
  )

}