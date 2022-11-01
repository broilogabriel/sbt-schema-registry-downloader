package io.github.broilogabriel.core

import java.nio.file

import scala.concurrent.{ ExecutionContextExecutor, Future }

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ ActorMaterializer, Attributes }
import akka.stream.scaladsl.{ FileIO, Flow, Sink, Source }
import argonaut._
import argonaut.Argonaut._
import com.typesafe.config.ConfigFactory
import org.slf4j

import io.github.broilogabriel.model.Subject

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

  def searchSubjects(
    subjects: Seq[String]
  )(implicit mat: ActorMaterializer): Flow[String, HttpRequest, NotUsed] = {

    def subjectUri(subject: String): Uri =
      Uri.Empty.withPath(Path(s"/subjects/$subject/versions/latest"))

    Flow[String]
      .map(Parse.decodeOption[List[String]])
      .map(_.map(_.intersect(subjects)).getOrElse(List.empty))
      .filter(_.nonEmpty)
      .mapConcat(identity)
      .map(subjectUri)
      .map(uri => HttpRequest(uri = uri))
  }

  def saveFile(
    folderPath: file.Path
  )(implicit mat: ActorMaterializer, ec: ExecutionContextExecutor): Flow[String, String, NotUsed] =
    Flow[String]
      .map(_.decodeOption[Subject])
      .collect { case Some(subject) =>
        subject
      }
      .mapAsync(2) { subject =>
        val path = folderPath.resolve(subject.filename)
        Source
          .single(subject.schemaAsByteString)
          .runWith(FileIO.toPath(path))
          .map(io => s"$path saved with size ${io.count}")
      }

  /** @param mat
    *   implicit mat to Unmarshal
    * @return
    *   Flow with the materialized
    */
  def responseToString(implicit mat: ActorMaterializer): Flow[HttpResponse, String, NotUsed] =
    Flow[HttpResponse]
      .collect {
        case response if response.status == StatusCodes.OK =>
          response.entity
        case response =>
          throw new Exception(s"Failed: ${response.status}")
      }
      .mapAsync(1) { e =>
        Unmarshal(e).to[String]
      }

}

case class SchemaDownloader(uri: Uri, subjects: Seq[String], folderPath: file.Path)(implicit
  logger: slf4j.Logger
) {
  private val cl = getClass.getClassLoader
  implicit val system: ActorSystem =
    ActorSystem("SchemaDownloaderActorSystem", ConfigFactory.load(cl), cl)
  implicit val ec: ExecutionContextExecutor    = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def download(
    conn: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection(
        host = uri.authority.host.address(),
        port = uri.effectivePort
      )
  ): Future[Done] = {
    Source
      .single("/subjects")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .map(path => HttpRequest(HttpMethods.GET, uri = path))
      .via(conn)
      .via(SchemaDownloader.responseToString)
      .via(SchemaDownloader.searchSubjects(subjects))
      .via(conn)
      .via(SchemaDownloader.responseToString)
      .via(SchemaDownloader.saveFile(folderPath))
      .runWith(Sink.foreach(logger.info))
      .andThen { case _ =>
        logger.info("Shutting down...")
        Http().shutdownAllConnectionPools().flatMap { _ =>
          materializer.shutdown()
          system.terminate()
        }
      }
  }
}
