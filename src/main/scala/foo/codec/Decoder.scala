package foo.codec

import com.sksamuel.avro4s.{FromRecord, SchemaFor}
import foo.Avro

trait Decoder[A] {
  def decode(a: Array[Byte]): A
}

object AvroDecoder {
  implicit def apply[A : SchemaFor : FromRecord]: Decoder[A] = new Decoder[A] {
    override def decode(a: Array[Byte]): A = Avro.decode[A](a)
  }
}
