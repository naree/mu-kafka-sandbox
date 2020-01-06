package foo.main

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import fs2.Stream
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.UsePlaintext
import foo.{UserV1, UserWithCountry}
import cats.implicits._

import scala.language.higherKinds

class UserV1RpcClient {
  def runClient[F[_]]()(implicit cs: ContextShift[F], timer: Timer[F], ce: ConcurrentEffect[F]): Resource[F, UserV1[F]] =
    UserV1.client[F](ChannelForAddress("localhost", 8080),
      List(UsePlaintext()))
}

object UserV1Client extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Stream.resource(new UserV1RpcClient()
    .runClient[IO]())
    .flatMap{ client => Stream.eval(client.sendUser(UserWithCountry("n", 1, "singa"))) }
    .compile
    .drain
    .as(ExitCode.Success)
    .unsafeRunSync()
    IO(ExitCode.Success)
  }
}
