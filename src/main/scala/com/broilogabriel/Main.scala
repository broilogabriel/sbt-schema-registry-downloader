package com.broilogabriel

import akka.http.scaladsl.model.Uri
import com.broilogabriel.core.SchemaDownloader

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  def main(args: Array[String]): Unit = {
    val r = new SchemaDownloader(
      Uri.Empty.withHost("localhost").withPort(8080),
      List("subs"),
      "av.avsc"
    ).download(null)

    println(Await.result(r, Duration.Inf))

  }

}
