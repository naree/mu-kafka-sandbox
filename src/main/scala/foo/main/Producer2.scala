package foo.main

import cats.effect.{IO, _}
import foo.main.Config.kafka._
import foo.UserWithRegion
import fs2.Stream
import fs2.concurrent.Queue

object Producer2 extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val messageQueue: Stream[IO, Queue[IO, Option[UserWithRegion]]] = Stream.eval(Queue.bounded[IO, Option[UserWithRegion]](1))
    val users: Stream[IO, Option[UserWithRegion]] = Stream(Some(UserWithRegion("naree", 1, Some("apac"))))

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
