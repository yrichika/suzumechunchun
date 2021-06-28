package helpers.crypto

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.password.BCryptSha256PasswordHasher

object Password {
  val passwordHasher = new BCryptSha256PasswordHasher()

  def hash(password: String) = {
    passwordHasher.hash(password)
  }

  def verify(hashedPassword: PasswordInfo, passwordString: String) = {
    passwordHasher.matches(hashedPassword, passwordString)
  }
}
