package helpers.crypto

import testhelpers.UnitTestCase

import scala.util.Random

class CrypterSpec extends UnitTestCase {

  "Encrypter" should {
    "encrypt text and decrypt that encrypted text" in {
      val text = "sometext"
      val ad = Random.alphanumeric.take(10).mkString

      val encryptedBytes = Crypter.encrypt(text, ad)
      val decrypted = Crypter.decrypt(encryptedBytes, ad)
      decrypted shouldBe text
    }

    "also work with japanese" in {
      val text ="あいうえお"
      val ad = Random.alphanumeric.take(10).mkString

      val encryptedBytes = Crypter.encrypt(text, ad)
      val decrypted = Crypter.decrypt(encryptedBytes, ad)
      decrypted shouldBe text
    }
  }

  it should {
    "throw exception if ads are different" in {
      val text = "sometext"
      val ad = Random.alphanumeric.take(10).mkString
      val differentAd = Random.alphanumeric.take(5).mkString

      val encryptedBytes = Crypter.encrypt(text, ad)
      a [java.security.GeneralSecurityException] should be thrownBy {
        Crypter.decrypt(encryptedBytes, differentAd)
      }

    }
  }

  // TODO: no test for keysetHandle method for now.
}
