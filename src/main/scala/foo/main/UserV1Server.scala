package foo.main

import cats.{Monad, MonadError}
import cats.effect._
import foo.main.Config.kafka.{broker, topic}
import foo.{UserV1, UserWithCountry}
import fs2._
import fs2.concurrent.Queue
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.implicits._

import scala.language.higherKinds

class UserV1Server[F[_] : Logger : Monad](queue: Queue[F, Option[UserWithCountry]])(implicit timer: Timer[F]) extends UserV1[F] {

  implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

  def sendUser(user: foo.UserWithCountry): F[UserWithCountry] = {
    for {
      _ <- Logger[F].info(s"Received $user")
      _ = queue.enqueue1(Some(user))
    } yield user
  }
}

object UserV1Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F] // TODO make this purer?

    val run = for {
      queue <- Stream.eval(Queue.bounded[IO, Option[UserWithCountry]](1))
      service = new UserV1Server(queue)
//      _ <- foo.kafka.Producer.streamWithQueue(broker, topic, queue) // TODO use contextshifting?
      grpcConfig <- Stream.eval(UserV1.bindService[IO](CE = implicitly[ConcurrentEffect[IO]], algebra = service))
      server <- Stream.eval(GrpcServer.default[IO](8080, List(AddService(grpcConfig)))) // TODO clean shutdown?
      _ <- Stream.eval(Logger[IO].info("Starting the server"))
      runServer <- Stream.eval(GrpcServer.server[IO](server))
    } yield runServer

    run.compile.drain.as(ExitCode.Success)
  }
}
