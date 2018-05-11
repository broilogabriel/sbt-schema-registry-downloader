package com.broilogabriel.sbt

import java.nio.file.{ Files, Paths }

import akka.http.scaladsl.model.Uri
import com.broilogabriel.core.SchemaDownloader
import sbt._
import complete.DefaultParsers._
import sbt.Keys.{ logLevel, _ }

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }
import scala.util.Try

object SbtSchemaRegistry extends AutoPlugin {

  object autoImport {
    val hellow = inputKey[Unit]("Says hello!")
    val schemaRegistryDownload = taskKey[Unit]("Download schemas")

    val schemaRegistryUrl = settingKey[String]("Url to schema registry")
    val schemaRegistryTargetFolder = settingKey[String]("Target for storing the avro schemas")
    val schemaRegistrySubjects = settingKey[Seq[String]]("Subject names to download")
  }

  import autoImport._

  import scala.concurrent.ExecutionContext.Implicits.global

  override lazy val projectSettings = Seq(
    hellow := {
      val args = spaceDelimited("").parsed
      System.out.println(s"Helalooou, ${args.head}")
    },
    logLevel in schemaRegistryDownload := (logLevel ?? Level.Info).value,
    schemaRegistryDownload in Compile := {
      implicit val logger: Logger = streams.value.log
      logger.debug(s"schemaRegistryUrl: ${schemaRegistryUrl.value}")
      logger.debug(s"schemaRegistryTargetFolder: ${schemaRegistryTargetFolder.value}")
      logger.debug(s"schemaRegistrySubjects: ${schemaRegistrySubjects.value.mkString(",")}")
      val folderPath = Paths.get(schemaRegistryTargetFolder.value)
      if (!Files.exists(folderPath)) Files.createDirectories(folderPath)
      val res = Future.fromTry(Try[Uri](SchemaDownloader.getUri(schemaRegistryUrl.value)))
        .flatMap {
          uri =>
            SchemaDownloader(uri, schemaRegistrySubjects.value, folderPath).download()
        }
      Await.result(res, 2.minutes)
    }
  )

}