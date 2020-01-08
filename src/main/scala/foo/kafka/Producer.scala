package foo.kafka

import cats.effect._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import foo.Avro
import fs2._
import fs2.concurrent.Queue
import fs2.kafka._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.language.higherKinds

object Producer {
  implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

  def settings[F[_]](broker: String)(implicit sync: Sync[F]): ProducerSettings[F, String, Array[Byte]] =
    ProducerSettings[F, String, Array[Byte]]
      .withBootstrapServers(broker)

  type PublishToKafka[F[_]] = Pipe[F, ProducerRecords[String, Array[Byte], Unit], ProducerResult[String, Array[Byte], Unit]]

  def pipe[F[_]](broker: String)(implicit contextShift: ContextShift[F],
                                 concurrentEffect: ConcurrentEffect[F], sync: Sync[F]): PublishToKafka[F] =
    produce(settings(broker))

  def apply[F[_], A: SchemaFor : ToRecord : FromRecord]
  (broker: String, topic: String, messageQueue: Stream[F, Queue[F, Option[A]]])
  (implicit contextShift: ContextShift[F], concurrentEffect: ConcurrentEffect[F], timer: Timer[F]): F[Unit] =

    (for {
      queue <- messageQueue
      _ <- streamWithQueue(broker, topic, queue).drain
    } yield ()).compile.drain

  def streamWithQueue[F[_], A: SchemaFor : ToRecord : FromRecord]
  (broker: String, topic: String, queue: Queue[F, Option[A]])
  (implicit contextShift: ContextShift[F], concurrentEffect: ConcurrentEffect[F], timer: Timer[F], sync: Sync[F]): Stream[F, Any] =

    streamWithQueue(pipe(broker))(topic, queue)

  def streamWithQueue[F[_], A: SchemaFor : ToRecord : FromRecord](publishToKafka: PublishToKafka[F])
                                                                 (topic: String, queue: Queue[F, Option[A]])
                                                                 (implicit contextShift: ContextShift[F], concurrentEffect: ConcurrentEffect[F], timer: Timer[F], sync: Sync[F]): Stream[F, Any] =
    queue.dequeue
      .unNoneTerminate
      .evalMap(a => concurrentEffect.delay(ProducerRecords.one(ProducerRecord(topic, "dummy-key", Avro.encode[A](a)))))
      .covary[F]
      .through(publishToKafka)
      .flatMap(result => Stream.eval(
        Logger[F].info(result.records.head.fold("Error: ProducerResult contained empty records.")(a => s"Published $a"))
      ).flatMap(_ => Stream.eval(sync.delay(result)))
      )
}
