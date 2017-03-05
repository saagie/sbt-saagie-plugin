sbtPlugin := true

// Metadata
organization := "io.saagie"
name := "sbt-saagie-plugin"
scalaVersion := "2.10.5"
description := "An SBT plugin for deploying scala and spark jobs on Saagie"

// Distribution
licenses := Seq("Apache" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage := Some(url("https://github.com/saagie/sbt-saagie-plugin"))

// Compiler Settings
javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
scalacOptions += "-target:jvm-1.7"

// Dependencies
resolvers += Resolver.sonatypeRepo("public")
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.typesafe.play" %% "play-ws" % "2.4.3")

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)

// Publication
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  println("isSnapshot: " + isSnapshot.value)
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

// publishTo := Some(Resolver.file("file",  new File( "/home/aurelien/.m2/releases" )) )

pomExtra := {
  <scm>
    <url>git@github.com:saagie/saagie-sbt-plugin.git</url>
    <connection>scm:git@github.com:saagie/saagie-sbt-plugin.git</connection>
  </scm>
    <developers>
      <developer>
        <id>avandel</id>
        <name>Aurelien Vandel</name>
        <url>https://github.com/avandel</url>
      </developer>
    </developers>
}
