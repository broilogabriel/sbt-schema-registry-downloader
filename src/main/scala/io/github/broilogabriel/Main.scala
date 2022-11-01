package io.github.broilogabriel

import java.nio.file.Paths

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.http.scaladsl.model.Uri
import org.slf4j
import org.slf4j.LoggerFactory

import io.github.broilogabriel.core.SchemaDownloader

object Main {

  def main(args: Array[String]): Unit = {

    implicit val logger: slf4j.Logger = LoggerFactory.getLogger(Main.getClass)
    val r = new SchemaDownloader(
      Uri("http://schemaregistry.broilogabriel.com"),
      List("avrotest"),
      Paths.get(System.getProperty("java.io.tmpdir"))
    ).download()

    val res = Await.result(r, Duration.Inf)
    logger.info(s"Result $res")

  }

}
