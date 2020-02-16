name := "mu-kafka-sandbox"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.ovoenergy" %% "fs2-kafka" % "0.20.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.4", // using an old version that is compatible with mu libs
  "io.higherkindness" %% "mu-rpc-fs2" % "0.19.1", // required for imports & annotations in the generated code
  "io.higherkindness" %% "mu-rpc-netty" % "0.19.1",
  "io.higherkindness" %% "mu-rpc-server" % "0.19.1",
  "io.higherkindness" %% "mu-rpc-kafka" % "0.21.0-SNAPSHOT",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
  "io.chrisdavenport" %% "log4cats-core"  % "1.0.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-config" % "0.9.9" % "test",
  "javax.xml.bind" % "jaxb-api" % "2.3.0" % "test", // required after java 8
  "javax.activation" % "activation" % "1.1" % "test",

  "org.scalatest" %% "scalatest" % "3.1.0" % "test"
)

import higherkindness.mu.rpc.srcgen.SrcGenPlugin.autoImport._

muSrcGenIdlType := "avro"
muSrcGenSerializationType := "AvroWithSchema"
muSrcGenIdiomaticEndpoints := true

sourceGenerators in Compile += (muSrcGen in Compile).taskValue
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch)

enablePlugins(JavaAppPackaging)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
