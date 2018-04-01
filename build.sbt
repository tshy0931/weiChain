name := "weiChain"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

libraryDependencies += "io.monix" % "monix_2.12" % "3.0.0-RC1"

libraryDependencies += "com.github.julien-truffaut" %% "monocle-core" % "1.5.1-cats"

libraryDependencies += "com.github.julien-truffaut" %% "monocle-macro" % "1.5.1-cats"

libraryDependencies += "com.github.julien-truffaut" %% "monocle-generic" % "1.5.1-cats"

libraryDependencies += "net.debasishg" %% "redisclient" % "3.5"

libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.11"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.11"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.11"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.11"

libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.11"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

libraryDependencies += "com.google.guava" % "guava" % "24.0-jre"

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.1.0" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.11" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % Test

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)