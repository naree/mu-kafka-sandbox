package foo.main

import cats.effect._
import foo.{UserV1, UserWithCountry}
import fs2._
import higherkindness.mu.rpc.protocol.Empty
import higherkindness.mu.rpc.server.{AddService, GrpcServer}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

class UserV1Server(implicit timer: Timer[IO]) extends UserV1[IO] {

  implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

  def sendUser(user: foo.UserWithCountry): IO[UserWithCountry] = {
    for {
      _ <- Logger[IO].info(s"Received $user")
    } yield user
  }
}

object UserV1Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    implicit val service: UserV1[IO] = new UserV1Server()
    implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

    val run = for {
      grpcConfig <- UserV1.bindService[IO]
      server     <- GrpcServer.default[IO](8080, List(AddService(grpcConfig)))
      _ <- Logger[IO].info("Starting the server")
      runServer  <- GrpcServer.server[IO](server)
    } yield runServer

    run.unsafeRunSync()
    IO(ExitCode.Success)
  }
}
