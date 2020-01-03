package foo

import cats.effect.{ContextShift, IO, Timer}
import foo.kafka.Producer
import fs2.Stream
import fs2.concurrent.Queue
import fs2.kafka.ProducerResult
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.global

class KafkaTest extends AnyFlatSpec with Matchers {
  behavior of "Kafka"

  "producer" should "accept a user via a queue and side effect publish to a topic" in {
    implicit def contextShift: ContextShift[IO] = IO.contextShift(global)

    implicit def timer: Timer[IO] = IO.timer(global)

    val messageQueue: Stream[IO, Queue[IO, Option[UserWithCountry]]] = Stream.eval(Queue.bounded[IO, Option[UserWithCountry]](2))
    val naree = UserWithCountry("naree", 1, "singapore")
    val users: Stream[IO, Option[UserWithCountry]] = Stream(Some(naree), None)

    val recordMetadata = new RecordMetadata(new TopicPartition("", 1), 0, 0,
      0, 0l, 0, 0)
    val publishToKafkaMock: foo.kafka.Producer.PublishToKafka[IO] = records => records.map{ producerRecords =>
        println(producerRecords.records.head.map(r => Avro.decode[UserWithCountry](r.value)))
        ProducerResult(producerRecords.records.map(r => (r, recordMetadata)), producerRecords.passthrough)
    }

    val producer = for {
      queue <- messageQueue
      result <- Stream(users.through(queue.enqueue), foo.kafka.Producer.streamWithQueue(publishToKafkaMock) ("topic1", queue)).parJoin(2)
    } yield result

    val results = producer.compile.toList.unsafeRunSync()
    println (results)
    Avro.decode[UserWithCountry] (results(2).asInstanceOf[ProducerResult[String, Array[Byte], Unit]].records.head.get._1.value) shouldBe naree
  }
}
