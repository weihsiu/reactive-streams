import sbt._

name := "reactive-streams-root"
scalaVersion in ThisBuild := "2.12.4"

val akkaVersion = "2.5.9"
val monixVersion = "3.0.0-M3"
val circeVersion = "0.9.1"

lazy val root = project.in(file("."))
  .aggregate(reactiveStreamsJVM, reactiveStreamsJS)
  .settings(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.5"),
    scalacOptions += "-Ypartial-unification"
  )

lazy val reactiveStreams = crossProject.in(file("."))
  .settings(
    name := "reactive-streams",
    version := "0.0.2-SNAPSHOT",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "1.0.1",
      "org.typelevel" %%% "cats-mtl-core" % "0.2.1",
      "co.fs2" %%% "fs2-core" % "0.9.7",
      "co.fs2" %%% "fs2-cats" % "0.5.0",
      "io.monix" %%% "monix" % monixVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "com.typesafe.akka" %% "akka-typed" % "2.5.8",
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.h2database" % "h2" % "1.4.196",
      "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.4"
    )
  )

lazy val reactiveStreamsJVM = reactiveStreams.jvm
lazy val reactiveStreamsJS = reactiveStreams.js

//libraryDependencies ++= {
//  val akkaVersion = "2.4.10"
//  val monixVersion = "2.0.2"
//  val circeVersion = "0.5.2"
//  Seq(
//    "org.typelevel" %% "cats" % "0.7.2",
//    "co.fs2" %% "fs2-core" % "0.9.1",
//    "co.fs2" %% "fs2-cats" % "0.1.0",
//    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
//    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
//    "com.typesafe.akka" %% "akka-typed-experimental" % akkaVersion,
//    "io.monix" %% "monix" % monixVersion,
//    "io.monix" %% "monix-cats" % monixVersion,
//    "io.circe" %% "circe-core" % circeVersion,
//    "io.circe" %% "circe-generic" % circeVersion,
//    "io.circe" %% "circe-parser" % circeVersion,
//    "com.typesafe.slick" %% "slick" % "3.1.1",
//    "ch.qos.logback" % "logback-classic" % "1.1.7",
//    "com.h2database" % "h2" % "1.4.192",
//    "org.scalacheck" %% "scalacheck" % "1.13.2" % "test",
//    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test"
//  )
//}
