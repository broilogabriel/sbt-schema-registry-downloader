package com.broilogabriel.core

import akka.http.scaladsl.model.{IllegalUriException, Uri}

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

  def download(uri: Uri, subjects: Seq[String]): Unit = {

  }

}
