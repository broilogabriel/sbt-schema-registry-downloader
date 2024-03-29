package io.github.broilogabriel.sbt

import java.nio.file.{ Files, Paths }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.DurationInt
import scala.util.Try

import akka.http.scaladsl.model.Uri
import org.slf4j
import sbt.{ settingKey, taskKey, AutoPlugin, Compile, Level }
import sbt.Keys.{ logLevel, streams }

import io.github.broilogabriel.core.SchemaDownloader

object SbtSchemaRegistryDownloader extends AutoPlugin {

  object autoImport {
    val schemaRegistryDownload = taskKey[Unit]("Download schemas")

    val schemaRegistryUrl          = settingKey[String]("Url to schema registry")
    val schemaRegistryTargetFolder = settingKey[String]("Target for storing the avro schemas")
    val schemaRegistrySubjects     = settingKey[Seq[String]]("Subject names to download")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  import autoImport._

  override lazy val projectSettings = Seq(
    logLevel in schemaRegistryDownload := (logLevel ?? Level.Info).value,
    schemaRegistryDownload in Compile := {
      val loggerName                    = streams.value.log.name
      implicit val logger: slf4j.Logger = org.slf4j.LoggerFactory.getLogger(loggerName)
      logger.debug(s"schemaRegistryUrl: ${schemaRegistryUrl.value}")
      logger.debug(s"schemaRegistryTargetFolder: ${schemaRegistryTargetFolder.value}")
      logger.debug(s"schemaRegistrySubjects: ${schemaRegistrySubjects.value.mkString(",")}")
      val folderPath = Paths.get(schemaRegistryTargetFolder.value)
      if (!Files.exists(folderPath)) Files.createDirectories(folderPath)
      val res = Future
        .fromTry(Try[Uri](SchemaDownloader.getUri(schemaRegistryUrl.value)))
        .flatMap { uri =>
          SchemaDownloader(uri, schemaRegistrySubjects.value, folderPath).download()
        }
      Await.result(res, 2.minutes)
    }
  )

}
