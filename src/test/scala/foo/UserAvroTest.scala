package foo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import Avro._

class UserAvroTest extends AnyWordSpecLike with Matchers {

  "Avro.encode[User]" should {
     "encode User in Avro format with schema v1 and decode with v1" in {
       val naree = UserWithCountry("naree", 1, "Singa")

       decode[UserWithCountry](encode(naree)) shouldBe naree
     }

    "encode User in Avro format with schema v1 and decode with v2" in {
      val naree = UserWithCountry("naree", 1, "Singa")

      decode[UserWithRegion](encode(naree)) shouldBe UserWithRegion("naree", 1, None)
    }
  }
}
