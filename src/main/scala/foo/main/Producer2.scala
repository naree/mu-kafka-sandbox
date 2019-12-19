package foo.main

import cats.effect.{IO, _}
import foo.main.Config.kafka._
import foo.{Kafka, UserWithRegion}
import fs2.Stream
import fs2.concurrent.Queue

object Producer2 extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val messageQueue: Stream[IO, Queue[IO, UserWithRegion]] = Stream.eval(Queue.bounded[IO, UserWithRegion](1))
    val users: Stream[IO, UserWithRegion] = Stream(UserWithRegion("naree", 1, Some("apac")))

    Kafka.producer(broker, topic, messageQueue, users)
      .compile
      .drain
      .unsafeRunSync()

    IO(ExitCode.Success)
  }
}
