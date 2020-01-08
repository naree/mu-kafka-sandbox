package foo.main

import cats.effect._
import foo.main.Config.kafka.{broker, topic}
import foo.{UserV1, UserWithCountry}
import fs2._
import fs2.concurrent.Queue
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.implicits._

class UserV1Server(queue: Stream[IO, Queue[IO, Option[UserWithCountry]]])(implicit timer: Timer[IO]) extends UserV1[IO] {

  implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

  def sendUser(user: foo.UserWithCountry): IO[UserWithCountry] = {
    val processUser = for {
      _ <- Stream.eval(Logger[IO].info(s"Received $user"))
      q <- queue
      _ = q.enqueue1(Some(user))
    } yield user

    processUser.compile.lastOrError
  }
}

object UserV1Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

    val messageQueue: Stream[IO, Queue[IO, Option[UserWithCountry]]] = Stream.eval(Queue.bounded[IO, Option[UserWithCountry]](1))
    implicit val service: UserV1[IO] = new UserV1Server(messageQueue)

    val run = for {
      queue <- messageQueue
      _ <- foo.kafka.Producer.streamWithQueue(broker, topic, queue)
      grpcConfig <- Stream.eval(UserV1.bindService[IO])
      server <- Stream.eval(GrpcServer.default[IO](8080, List(AddService(grpcConfig))))
      _ <- Stream.eval(Logger[IO].info("Starting the server"))
      runServer <- Stream.eval(GrpcServer.server[IO](server))
    } yield runServer

    run.compile.drain.as(ExitCode.Success)
  }
}
