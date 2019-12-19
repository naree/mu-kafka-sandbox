name := "mu-kafka-sandbox"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.ovoenergy" %% "fs2-kafka" % "0.20.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.4", // using an old version that is compatible with mu libs
  "io.higherkindness" %% "mu-rpc-fs2" % "0.19.1", // required for imports & annotations in the generated code
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test"
)

import higherkindness.mu.rpc.idlgen.IdlGenPlugin.autoImport._

idlType := "avro"
srcGenSerializationType := "AvroWithSchema"
sourceGenerators in Compile += (srcGen in Compile).taskValue
