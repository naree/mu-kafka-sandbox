package foo.main

import cats.effect.{IO, _}
import foo.UserWithRegion
import fs2.Stream
import higherkindness.mu.kafka.producer

object Producer2 extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import SandboxConfig.kafka._
    import higherkindness.mu.format.AvroWithSchema._

    val users: Stream[IO, Option[UserWithRegion]] = Stream(Some(UserWithRegion("naree", 1, Some("apac"))))
    producer(topic, users).unsafeRunSync()

    IO(ExitCode.Success)
  }
}
