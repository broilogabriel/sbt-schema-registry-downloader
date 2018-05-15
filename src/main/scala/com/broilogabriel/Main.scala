package com.broilogabriel

import java.nio.file.Paths

import akka.http.scaladsl.model.Uri
import com.broilogabriel.core.SchemaDownloader
import org.slf4j
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
