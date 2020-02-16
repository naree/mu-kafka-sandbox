package foo.main

import cats.effect._
import foo.UserWithRegion
import fs2.Pipe
import higherkindness.mu.kafka.consumer

object Consumer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    // config & dependencies
    import SandboxConfig.kafka._
    import higherkindness.mu.format.AvroWithSchema._

    // message processing
    val printMessage: Pipe[IO, UserWithRegion, UserWithRegion] = _.map{ user =>
      println(s"Processing $user")
      user
    }
    val program = consumer(topic, consumerGroup, printMessage)

    program.unsafeRunSync()
    IO(ExitCode.Success)
  }
}
