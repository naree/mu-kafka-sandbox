package foo.main

import cats.effect._
import foo._
import fs2.concurrent.Queue
import foo.main.Config.kafka.{broker, topic}
import fs2.Stream

object Producer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val messageQueue: Stream[IO, Queue[IO, Option[UserWithCountry]]] = Stream.eval(Queue.bounded[IO, Option[UserWithCountry]](1))
    val users: Stream[IO, Option[UserWithCountry]] = Stream(Some(UserWithCountry("naree", 1, "singapore")))

    val producer = for {
      queue <- messageQueue
      result <- Stream(
        users.through(queue.enqueue),
        foo.kafka.Producer.streamWithQueue(broker, topic, queue))
        .parJoin(2)
    } yield result

    producer.compile.drain.unsafeRunSync()
    IO(ExitCode.Success)
  }
}
