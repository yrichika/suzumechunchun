package helpers.crypto

import testhelpers.UnitTestCase

class PasswordSpec extends UnitTestCase {
  "Password hash and verify" should {
    "be true if the same password" in {
      val samePassword ="samepassword"
      val hashedPass = Password.hash(samePassword)
      Password.verify(hashedPass, samePassword) shouldBe true
    }
  }


  "Password hash and verify" should {
    "be false if different passwords" in {
      val password ="password"
      val hashedPass = Password.hash(password)
      Password.verify(hashedPass, "differentPassword") shouldBe false
    }
  }
}
