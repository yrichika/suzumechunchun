package helpers.crypto

import testhelpers.UnitTestCase
import testhelpers.utils.TestRandom

class RandomSpec extends UnitTestCase {
  "alphanumeric" should {
    "return random string with default number of chars 64" in {
      val randomAlphaNumeric = Random.alphanumeric()
      randomAlphaNumeric.size shouldBe 64
    }

    "return number of chars given to the parameter" in {
      val num = TestRandom.int(72)
      val randomAlphaNumeric = Random.alphanumeric(num)
      randomAlphaNumeric.size shouldBe num
    }
  }
}
