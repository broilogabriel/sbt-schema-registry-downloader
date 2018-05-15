package com.broilogabriel.core

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings, scaladsl }
import akka.testkit.{ TestKit, TestProbe }
import argonaut.EncodeJsons
import argonaut.JsonIdentity.ToJsonIdentity
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.slf4j
import org.slf4j.LoggerFactory

class SchemeDownloaderTest extends TestKit(ActorSystem("SchemeDownloaderTest")) with FlatSpecLike with Matchers with
  MockFactory with EncodeJsons {

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  trait Downloader {
    implicit val logger: slf4j.Logger = LoggerFactory.getLogger(Downloader.this.getClass)
    val downloader = SchemaDownloader(Uri.Empty.withHost("www.schemaregistry.com").withScheme("http"), Seq
    ("testschema"), Paths.get(System.getProperty("java.io.tmpdir")))
  }

  trait Schemas extends EncodeJsons {
    val subjects = List("testschema")
    val subject_testschema: String =
      """
        |{
        |  "subject": "testschema",
        |  "version": 1,
        |  "id": 1,
        |  "schema": "{\"type\":\"record\",\"name\":\"TestSchema\",\"namespace\":\"com.broilogabriel.domain\",
        |  \"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"username\",\"type\":\"string\"},
        |  {\"name\":\"phone\",\"type\":{\"type\":\"array\",\"items\":\"long\"},\"default\":[]}]}"
        |}
      """.stripMargin
    //    val subject_testschema: String = Source.fromURL(getClass.getResource("/testschema.json")).mkString

  }

  "An url" should "raise IllegalUriException if it has no schema" in {
    a[IllegalUriException] should be thrownBy {
      SchemaDownloader.getUri("localhost:8080")
    }
  }

  it should "raise IllegalUriException if it has no authority" in {
    a[IllegalUriException] should be thrownBy {
      SchemaDownloader.getUri("http://")
    }
  }

  it should "raise IllegalUriException if it has no host" in {
    a[IllegalUriException] should be thrownBy {
      SchemaDownloader.getUri("http://:8080")
    }
  }

  it should "return a valid uri" in {
    val uri = SchemaDownloader.getUri("http://localhost:8080")
    uri.scheme should be("http")
    uri.authority.toString() should be("localhost:8080")
  }

  it should "return a valid uri for a domain" in {
    val uri = SchemaDownloader.getUri("http://schemaregistry.broilogabriel.com")
    uri.scheme should be("http")
    uri.authority.host.address() should be("schemaregistry.broilogabriel.com")
    uri.effectivePort should be(80)
  }

  "When searching subjects" should "create the next request with the ones found" in new Downloader with Schemas {
    val probe = TestProbe()
    val responseEntity: String = (subjects :+ "invalid").asJson.nospaces
    scaladsl.Source.single(responseEntity)
      .via(SchemaDownloader.searchSubjects(subjects = subjects))
      .runWith(TestSink.probe[HttpRequest])
      .request(1)
      .expectNext(HttpRequest(uri = Uri.Empty.withPath(Path(s"/subjects/${subjects.head}/versions/latest"))))
      .onComplete()
  }

  it should "only complete without message in case it can't find the subject" in new Downloader with Schemas {
    val probe = TestProbe()
    val responseEntity: String = List("invalid").asJson.nospaces
    scaladsl.Source.single(responseEntity)
      .via(SchemaDownloader.searchSubjects(subjects = subjects))
      .runWith(TestSink.probe[HttpRequest])
      .request(1)
      .expectComplete()
  }

  "Saving files" should "" in new Downloader with Schemas {
    //    val probe = TestProbe()
    //    val r = scaladsl.Source.single(subject_testschema)
    //      .runWith(SchemaDownloader.saveFile(Paths.get(System.getProperty("java.io.tmpdir"), "test.avro")))
    //
    //    val x = Await.result(r, Duration.Inf)
    //    println(x)
  }

}
