package com.broilogabriel.model

import akka.util.ByteString
import argonaut.Argonaut.casecodec4
import argonaut.CodecJson

object Subject {
  implicit def TwitterUserCodecJson: CodecJson[Subject] =
    casecodec4(Subject.apply, Subject.unapply)("subject", "version", "id", "schema")
}

case class Subject(subject: String, version: Long, id: Long, schema: String) extends Serializable {
  def schemaAsByteString = ByteString(schema + "\n")
}
