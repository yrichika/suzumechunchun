package testhelpers

import org.scalatest._
import org.scalatest.wordspec.AnyWordSpecLike


trait UnitTestCase extends AnyWordSpecLike
  with matchers.should.Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
{

}
