package foo.main

import cats.effect._
import foo.{Kafka, UserWithRegion}

object Consumer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import Config.kafka._
    val consumerGroup = "test-consumer-group"

    Kafka.consumer[IO, UserWithRegion](consumerGroup, topic, broker).unsafeRunSync()
    IO(ExitCode.Success)
  }
}
