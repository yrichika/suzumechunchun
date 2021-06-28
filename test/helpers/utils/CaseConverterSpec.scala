package helpers.utils


import testhelpers.UnitTestCase
import helpers.utils.CaseConverter._

class CaseConverterSpec extends UnitTestCase {

  case class DummyCase(name: String, age: Int)
  val originalCase = DummyCase("John", 35)

  "toMap" should {
    "convert case class to map" in {
      val result = originalCase.toMap()

      result("name") should be (originalCase.name)
      result("age") should be (originalCase.age)
    }
  }

  "toList" should {
    "convert case class to List[String]" in {
      val resultList = originalCase.toList()
      val expectedList = List("John", 35)

      for ((result, expected) <- (resultList zip expectedList)) {
        result should be (expected)
      }

    }
  }
}
