package foo.main

import cats.effect._
import foo._
import fs2.concurrent.Queue
import foo.main.Config.kafka.{broker, topic}
import fs2.Stream

object Producer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val messageQueue: Stream[IO, Queue[IO, UserWithCountry]] = Stream.eval(Queue.bounded[IO, UserWithCountry](1))
    val users: Stream[IO, UserWithCountry] = Stream(UserWithCountry("naree", 1, "singapore"))

    Kafka.producer(broker, topic, messageQueue, users)
      .compile
      .drain
      .unsafeRunSync()

    IO(ExitCode.Success)
  }
}
