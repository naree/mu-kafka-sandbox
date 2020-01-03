package foo.main

import cats.effect._
import foo.UserWithRegion

object Consumer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import Config.kafka._
    val consumerGroup = "test-consumer-group"

    foo.kafka.Consumer[IO, UserWithRegion](consumerGroup, topic, broker).unsafeRunSync()
    IO(ExitCode.Success)
  }
}
