package helpers.crypto

import java.math.BigInteger
import java.security.SecureRandom

object Random {
  private val random = new SecureRandom()

  def alphanumeric(length: Int = 64): String = {
    new BigInteger(length * 5, random).toString(32)
  }
}
