package com.broilogabriel.sbt

import akka.http.scaladsl.model.Uri
import com.broilogabriel.core.SchemaDownloader
import sbt._
import complete.DefaultParsers._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }
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
  import scala.concurrent.ExecutionContext.Implicits.global

  override lazy val projectSettings = Seq(
    hellow := {
      val args = spaceDelimited("").parsed
      System.out.println(s"Helalooou, ${args.head}")
    },
    schemaRegistryDownload in Compile := {
      System.out.println(
        s"""
           |schemaRegistryUrl: ${schemaRegistryUrl.value}
           |schemaRegistryTargetFolder: ${schemaRegistryTargetFolder.value}
           |schemaRegistrySubjects: ${schemaRegistrySubjects.value.mkString(",")}
           |""".stripMargin)
      val res = Future.fromTry(Try[Uri](SchemaDownloader.getUri(schemaRegistryUrl.value)))
        .flatMap {
          uri =>
            SchemaDownloader(uri, schemaRegistrySubjects.value, schemaRegistryTargetFolder.value).download()
        }
      Await.result(res, 2.minutes)
      System.out.println(s"\n\n")
    }
  )

}