package testhelpers

import play.api.test.Injecting

/**
 * this is a playground for PlaySpec feature testing.
 */
class TestExpFeatureSpec extends TestCase
  with Injecting
  with TestDatabaseConfiguration
{

  "experimenting with feature test case" should {
    "do something" in {
      true mustBe true
    }
  }
}
