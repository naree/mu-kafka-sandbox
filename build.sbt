name := "mu-kafka-sandbox"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.ovoenergy" %% "fs2-kafka" % "0.20.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.4", // using an old version that is compatible with mu libs
  "io.higherkindness" %% "mu-rpc-fs2" % "0.19.1", // required for imports & annotations in the generated code
  "io.higherkindness" %% "mu-rpc-netty" % "0.19.1",
  "io.higherkindness" %% "mu-rpc-server" % "0.19.1",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
  "io.chrisdavenport" %% "log4cats-core"  % "1.0.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test"
)

import higherkindness.mu.rpc.idlgen.IdlGenPlugin.autoImport._

idlType := "avro"
srcGenSerializationType := "AvroWithSchema"
idlGenIdiomaticEndpoints := true
sourceGenerators in Compile += (srcGen in Compile).taskValue
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)

enablePlugins(JavaAppPackaging)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
