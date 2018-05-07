package com.broilogabriel.core

import java.nio.file
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import argonaut.Argonaut._
import argonaut._
import com.broilogabriel.model.Subject

import scala.concurrent.Future

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

  def saveFile(destinationPath: file.Path): Sink[String, Future[IOResult]] = {
    println(destinationPath.toAbsolutePath.toString)
    Flow[String]
      .map(_.decode[Subject])
      .map(ByteString(_))
      // TODO: custom path for each file, substream? multiple streams?
      .toMat(FileIO.toPath(destinationPath))(Keep.right)
  }

  /**
    *
    * @param materializer implicit mat to Unmarshal
    * @return Flow with the materialized
    */
  def responseToString(implicit materializer: ActorMaterializer): Flow[HttpResponse, String, NotUsed] =
    Flow[HttpResponse]
      .filter(_.status == StatusCodes.OK)
      .map(_.entity)
      .mapAsync(1) {
        e =>
          Unmarshal(e).to[String]
      }

}

case class SchemaDownloader(uri: Uri, subjects: Seq[String], destinationFolder: String) {
  implicit val system: ActorSystem = ActorSystem() // to get an implicit ExecutionContext into scope
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def download(conn: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnection(uri
    .authority.toString())): Unit = {
    Source.single("subjects")
      .map(path => HttpRequest(uri = Uri.Empty.withPath(Path(path))))
      .via(conn)
      .via(SchemaDownloader.responseToString)
      .via(SchemaDownloader.searchSubjects(subjects))
      .via(conn)
      .via(SchemaDownloader.responseToString)
      .to(SchemaDownloader.saveFile(Paths.get(destinationFolder)))
  }

}
