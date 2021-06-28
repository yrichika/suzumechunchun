package modules

import testhelpers.UnitTestCase
import modules.JSoupInputSanitizer
import testhelpers.utils.TestRandom

class JSoupInputSanitizerSpec extends UnitTestCase {
  val inputSanitizer = new JSoupInputSanitizer

  "sanitize" should {
    "sanitize polluted html string" in {
      val okTag = "<p>it should pass</p>"
      val tagName = TestRandom.string(5)
      val ignoreTag = s"<$tagName></$tagName>"
      val result = inputSanitizer.sanitize(s"$okTag$ignoreTag")
      result shouldBe okTag
    }
  }

}
