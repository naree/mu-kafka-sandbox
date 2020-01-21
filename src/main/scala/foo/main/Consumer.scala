package foo.main

import cats.effect._
import foo.UserWithRegion
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.syntax.functor._

object Consumer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import Config.kafka._
    val consumerGroup = "test-consumer-group"
    import AvroDecoderForUserWithRegion._

    val program = for {
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
    } yield foo.kafka.Consumer[IO, UserWithRegion](consumerGroup, topic, broker)

    program.unsafeRunSync().as(ExitCode.Success)
  }
}
