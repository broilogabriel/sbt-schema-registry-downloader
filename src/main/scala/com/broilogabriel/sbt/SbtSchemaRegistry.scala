package com.broilogabriel.sbt

import sbt._
import complete.DefaultParsers._

object SbtSchemaRegistry extends AutoPlugin {

  object autoImport {
    val hello = inputKey[Unit]("Says hello!")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    hello := {
      val args = spaceDelimited("").parsed
      System.out.println(s"Hello, ${args.head}")
    }
  )

}