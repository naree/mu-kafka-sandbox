package foo.main

import cats.effect._
import foo._
import fs2.Stream
import higherkindness.mu.kafka._

object Producer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import SandboxConfig.kafka._
    import higherkindness.mu.format.AvroWithSchema._

    val users: Stream[IO, Option[UserWithCountry]] = Stream(Some(UserWithCountry("naree", 1, "singapore")))
    producer(topic, users).unsafeRunSync()

    IO(ExitCode.Success)
  }
}
