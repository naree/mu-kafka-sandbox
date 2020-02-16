package foo.main

import cats.Monad
import cats.effect._
import cats.implicits._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import foo.{UserV1, UserWithCountry}
import fs2._
import fs2.concurrent.Queue
import higherkindness.mu.kafka._
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}

import scala.language.higherKinds

class UserV1Server[F[_] : Monad: Sync](queue: Queue[F, Option[UserWithCountry]])(implicit timer: Timer[F]) extends UserV1[F] {

  implicit def unsafeLogger[F[_] : Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  def sendUser(user: foo.UserWithCountry): F[UserWithCountry] = {
    for {
      _ <- Logger[F].info(s"Received $user")
      _ <- queue.enqueue(Stream.eval(implicitly[Sync[F]].delay(Some(user)))).compile.drain
      _ <- Logger[F].info(s"Queued $user to be sent to test-topic")
    } yield user
  }
}

object UserV1Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    import SandboxConfig.kafka._

    def startGrp[F[_]: Timer](queue: Queue[F, Option[UserWithCountry]])(implicit concurrentEffect: ConcurrentEffect[F]): Stream[F, Unit] = {
      for {
        grpcConfig <- Stream.eval(UserV1.bindService[F](CE = concurrentEffect, algebra = new UserV1Server(queue)))
        server <- Stream.eval(GrpcServer.default[F](8080, List(AddService(grpcConfig))))
        _ <- Stream.eval(Logger[F].info("Starting the server"))
        runServer <- Stream.eval(GrpcServer.server[F](server))
      } yield runServer
    }

    import higherkindness.mu.format.AvroWithSchema._

    def startKafkaProducer[F[_]: ConcurrentEffect: ContextShift: Timer, A: SchemaFor: ToRecord: FromRecord]
    (queue: Queue[F, Option[A]]): Stream[F, ByteArrayProducerResult] =
      Stream
        .eval(Logger[F].info("Starting the Kafka Producer"))
        .flatMap(_ => ProducerStream(topic, queue, producerSettings(brokers)))

    implicit def unsafeLogger[F[_] : Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

    val run = for {
      queue <- Stream.eval(Queue.bounded[IO, Option[UserWithCountry]](1))
      runServer <- startKafkaProducer(queue) concurrently startGrp(queue)
    } yield runServer

    val program: IO[Unit] = run.compile.drain
    program.as(ExitCode.Success)
  }
}
