package com.broilogabriel.core

import akka.http.scaladsl.model.IllegalUriException
import org.scalatest._

class SchemeDownloaderTest extends FlatSpec with Matchers {

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

}
