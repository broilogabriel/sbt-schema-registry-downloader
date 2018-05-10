package com.broilogabriel.core

import java.nio.file
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ ActorMaterializer, Attributes, IOResult }
import akka.stream.scaladsl.{ FileIO, Flow, Keep, Sink, Source }
import akka.util.ByteString
import argonaut.Argonaut._
import argonaut._
import com.broilogabriel.model.Subject
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ ExecutionContextExecutor, Future }

object SchemaDownloader {

  def getUri(url: String): Uri = {
    Uri(url) match {
      case uri if !uri.scheme.matches("^https?") =>
        throw IllegalUriException(s"Invalid scheme: ${uri.scheme} | Expected: http[s]")
      case uri if uri.authority.isEmpty =>
        throw IllegalUriException(s"Missing authority ${uri.toString()}")
      case uri if uri.authority.host.isEmpty =>
        throw IllegalUriException(s"Missing host ${uri.toString()}")
      case uri => uri
    }
  }

  def searchSubjects(subjects: Seq[String])(implicit materializer: ActorMaterializer): Flow[String,
    HttpRequest, NotUsed] =
    Flow[String]
      .map {
        msg =>
          Parse.decodeOption[List[String]](msg)
      }
      .map(_.map(_.intersect(subjects)).getOrElse(List.empty))
      .filter(_.nonEmpty)
      //    .takeWithin(10.seconds)
      .mapConcat(identity)
      .map {
        subject =>
          Path(s"/subjects/$subject/versions/latest")
      }
      .map(p => Uri.Empty.withPath(p))
      .map {
        uri =>
          HttpRequest(uri = uri)
      }

  // TODO: custom path for each file, substream? multiple streams?
  def saveFile(destinationPath: file.Path): Sink[String, Future[IOResult]] = Flow[String]
    .map(_.decodeOption[Subject])
    .collect {
      case Some(subject) => subject
    }
    .map(_.schemaAsByteString)
    .toMat(FileIO.toPath(destinationPath))(Keep.right)

  /**
    *
    * @param materializer implicit mat to Unmarshal
    * @return Flow with the materialized
    */
  def responseToString(implicit materializer: ActorMaterializer): Flow[HttpResponse, String, NotUsed] =
    Flow[HttpResponse]
      .map {
        a =>
          println(a)
          a
      }
      .collect {
        case response if response.status == StatusCodes.OK =>
          println(response)
          response.entity
        case response =>
          println(response)
          throw new Exception(s"Failed: ${response.status}")
      }
      .mapAsync(1) {
        e =>
          Unmarshal(e).to[String]
      }

}

case class SchemaDownloader(uri: Uri, subjects: Seq[String], destinationFolder: String) {
  private val cl = getClass.getClassLoader
  implicit val system: ActorSystem = ActorSystem("SchemaDownloaderActorSystem", ConfigFactory.load(cl), cl)
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val adapter: LoggingAdapter = Logging(system, "logger")

  def download(conn: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnection(
    host = uri.authority.host.address(),
    port = uri.authority.port
  )) = {
    val c = Http().outgoingConnection(
      host = uri.authority.host.address(),
      port = uri.authority.port
    )
    Source.single("/subjects")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .map(path => HttpRequest(HttpMethods.GET, uri = path))
      .via(c)
      .via(SchemaDownloader.responseToString)
      .log("response")
      .via(SchemaDownloader.searchSubjects(subjects))
      .via(c)
      .via(SchemaDownloader.responseToString)
      .runWith(SchemaDownloader.saveFile(Paths.get(destinationFolder)))
      .andThen {
        case result =>
          println(s"Shutting down...$result")
          Http().shutdownAllConnectionPools().flatMap { _ =>
            materializer.shutdown()
            system.terminate()
          }
      }
  }
}
