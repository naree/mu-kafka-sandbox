package foo.main

import java.util.concurrent.Executors

import cats.{Monad, MonadError}
import cats.effect._
import foo.main.Config.kafka.{broker, topic}
import foo.{UserV1, UserWithCountry}
import fs2._
import fs2.concurrent.Queue
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.implicits._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}

import scala.concurrent.ExecutionContext
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

    def startGrp[F[_]: Timer](queue: Queue[F, Option[UserWithCountry]])(implicit concurrentEffect: ConcurrentEffect[F]): Stream[F, Unit] = {
      for {
        grpcConfig <- Stream.eval(UserV1.bindService[F](CE = concurrentEffect, algebra = new UserV1Server(queue)))
        server <- Stream.eval(GrpcServer.default[F](8080, List(AddService(grpcConfig)))) // TODO clean shutdown?
        _ <- Stream.eval(Logger[F].info("Starting the server"))
        runServer <- Stream.eval(GrpcServer.server[F](server))
      } yield runServer
    }

    def startKafkaProducer[F[_]: ConcurrentEffect: ContextShift: Timer, A : SchemaFor: ToRecord: FromRecord](queue: Queue[F, Option[A]]): Stream[F, foo.kafka.Producer.ByteArrayProducerResult] =
      Stream
        .eval(Logger[F].info("Starting the Kafka Producer"))
        .flatMap(_ => foo.kafka.Producer.streamWithQueue(broker, topic, queue))

    implicit def unsafeLogger[F[_] : Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F] // TODO make this purer?

    val run = for {
      queue <- Stream.eval(Queue.bounded[IO, Option[UserWithCountry]](1))
      runServer <- startKafkaProducer(queue) concurrently startGrp(queue)
    } yield runServer

    val program: IO[Unit] = run.compile.drain
    program.as(ExitCode.Success)
  }
}
