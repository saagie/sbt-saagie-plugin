package io.saagie.sbt.plugin.enum

/**
  * Created by aurelien on 27/01/17.
  */


sealed case class JobType(value: String)

object JobType {
  object JAVA_SCALA extends JobType("java-scala")
  object SPARK extends JobType("spark")

  val values = Seq(JAVA_SCALA, SPARK)
}
