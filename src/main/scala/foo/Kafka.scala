package foo

import cats.effect._
import com.sksamuel.avro4s.{FromRecord, SchemaFor, ToRecord}
import fs2._
import fs2.concurrent.Queue
import fs2.kafka._
import scala.concurrent.duration._
import scala.language.higherKinds

object Kafka {

  object producer {
    def settings[F[_]](broker: String)(implicit sync: Sync[F]): ProducerSettings[F, String, Array[Byte]] = ProducerSettings[F, String, Array[Byte]]
      .withBootstrapServers(broker)

    def pipe[F[_]](broker: String)(implicit contextShift: ContextShift[F],
                             concurrentEffect: ConcurrentEffect[F], sync: Sync[F]):
    Pipe[F, ProducerRecords[String, Array[Byte], Unit], ProducerResult[String, Array[Byte], Unit]] =
      produce(settings(broker))

    def apply[F[_], A: SchemaFor : ToRecord : FromRecord]
      (broker: String, topic: String, messageQueue: Stream[F, Queue[F, A]], users: Stream[F, A])
      (implicit contextShift: ContextShift[F], concurrentEffect: ConcurrentEffect[F], timer: Timer[F]): Stream[F, Unit] =
      for {
        queue <- messageQueue
        _ <- Stream.sleep_[F](5.seconds) concurrently producerStreamWithInputQueue(broker, topic, users, queue).drain
      } yield ()

    def producerStreamWithInputQueue[F[_], A : SchemaFor : ToRecord : FromRecord]
      (broker: String, topic: String, users: Stream[F, A], queue: Queue[F, A])
      (implicit contextShift: ContextShift[F], concurrentEffect: ConcurrentEffect[F], timer: Timer[F], sync: Sync[F]): Stream[F, Any] =
      Stream(
        users.through(queue.enqueue),
        queue.dequeue
          .evalMap(a =>
            concurrentEffect.delay(ProducerRecords
              .one(ProducerRecord(topic, "dummy-key", Avro.encode[A](a))))
          ).covary[F]
          .through(producer.pipe[F](broker))
//          .flatMap(result => Stream.eval(
//            IO(println(result.records
//              .map(record => Avro.decode[A](record._1.value))
//              .head
//              .fold("Error: ProducerResult contained empty records.")(a => s"Published $a")))
//          )
//          )
      ).parJoin(2)
  }

  object consumer {
    def settings(groupId: String, broker: String): ConsumerSettings[IO, String, Array[Byte]] =
      ConsumerSettings[IO, String, Array[Byte]]
        .withGroupId(groupId)
        .withBootstrapServers(broker)
        .withAutoOffsetReset(AutoOffsetReset.Latest)

    def apply[A: SchemaFor : FromRecord](groupId: String, topic: String, broker: String)
                                        (implicit contextShift: ContextShift[IO],
                                         concurrentEffect: ConcurrentEffect[IO], timer: Timer[IO]): IO[Unit] =
      stream(groupId, topic, broker).compile.drain

    def stream[A: SchemaFor : FromRecord](groupId: String, topic: String, broker: String)
                                         (implicit contextShift: ContextShift[IO],
                                          concurrentEffect: ConcurrentEffect[IO], timer: Timer[IO]): Stream[IO, Unit] =
      consumerStream(settings(groupId, broker))
        .evalTap(_.subscribeTo(topic))
        .flatMap {
          _.stream
            .flatMap(message => Stream.eval(IO {
              println(Avro.decode[A](message.record.value))
            }))
        }
  }

}
