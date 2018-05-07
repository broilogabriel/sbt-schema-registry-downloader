package com.broilogabriel.core

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, scaladsl}
import akka.testkit.{TestKit, TestProbe}
import argonaut.JsonIdentity.ToJsonIdentity
import argonaut._
import org.scalamock.scalatest.MockFactory
import org.scalatest._

class SchemeDownloaderTest extends TestKit(ActorSystem("SchemeDownloaderTest")) with FlatSpecLike with Matchers with
  MockFactory with EncodeJsons {

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  trait Downloader {
    val downloader = SchemaDownloader(Uri.Empty.withHost("www.schemaregistry.com").withScheme("http"), Seq
    ("testschema"), "")
  }

  trait Schemas {
    val subjects = List("testschema")
    val subjectsJson: String = subjects.asJson.nospaces
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

  "When download is called" should "work" in new Downloader with Schemas {
    val probe = TestProbe()
    val responseMock: HttpResponse = HttpResponse(entity = HttpEntity(contentType = ContentTypes.`application/json`,
      string = subjectsJson))
    scaladsl.Source.single(responseMock)
      .via(SchemaDownloader.searchSubjects(subjects = subjects))
      .runWith(TestSink.probe[HttpRequest])
      .request(1)
      .expectNext(HttpRequest(uri = Uri.Empty.withPath(Path(s"/subjects/${subjects.head}/versions/latest"))))
      .onComplete()
  }

}
