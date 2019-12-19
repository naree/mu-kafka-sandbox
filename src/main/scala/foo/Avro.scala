package foo

import java.io.ByteArrayOutputStream

import com.sksamuel.avro4s._

object Avro {
  def decode[A : SchemaFor : FromRecord](bytes: Array[Byte]): A = {
    val in = AvroInputStream.data[A](bytes)
    in.close()
    in.iterator.toSet.head
  }

  def encode[A : SchemaFor: ToRecord](value: A): Array[Byte] = {
    val bOut = new ByteArrayOutputStream()
    val out = AvroOutputStream.data[A](bOut)
    out.write(value)
    out.close()
    bOut.toByteArray
  }
}
