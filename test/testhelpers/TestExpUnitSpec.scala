package testhelpers


import java.time.LocalDateTime
import _root_.helpers.utils.CaseConverter._
import com.typesafe.config.ConfigFactory
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.http.SecretConfiguration
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.json.Json
import testhelpers.utils.TestRandom

import java.sql.Timestamp


/**
 * this is a playground for unit test.
 */
class TestExpUnitSpec extends UnitTestCase {

  "testing" should {
    "do testing" in {

      true shouldBe true
    }
  }
}
