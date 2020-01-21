package foo.kafka

import cats.effect._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import foo.Avro
import foo.codec.Decoder
import fs2._
import fs2.concurrent.Queue
import fs2.kafka._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.language.higherKinds

object Consumer {

  def settings[F[_]](groupId: String, broker: String)(implicit sync: Sync[F]): ConsumerSettings[F, String, Array[Byte]] =
    ConsumerSettings[F, String, Array[Byte]]
      .withGroupId(groupId)
      .withBootstrapServers(broker)
      .withAutoOffsetReset(AutoOffsetReset.Latest)

  def apply[F[_]: Logger, A](groupId: String, topic: String, broker: String)
                         (implicit contextShift: ContextShift[F],
                         concurrentEffect: ConcurrentEffect[F], timer: Timer[F], decoder: Decoder[A]): F[Unit] =
    stream(groupId, topic, broker).compile.drain

  def stream[F[_]: Logger, A](groupId: String, topic: String, broker: String)
                             (implicit contextShift: ContextShift[F],
                             concurrentEffect: ConcurrentEffect[F], timer: Timer[F], decoder: Decoder[A]): Stream[F, A] =
    consumerStream(settings(groupId, broker))
      .evalTap(_.subscribeTo(topic))
      .flatMap {
        _.stream
          .flatMap { message =>
            val a = decoder.decode(message.record.value)
            Stream.eval(Logger[F].info(a.toString)).map(_ => a)
          }
      }
}
