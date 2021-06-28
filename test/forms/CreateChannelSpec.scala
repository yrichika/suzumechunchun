package forms

import testhelpers.{TestCase, TestDatabaseConfiguration}
import forms.CreateChannel
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Injecting
import testhelpers.utils.TestRandom

class CreateChannelSpec extends TestCase
  with TestDatabaseConfiguration
{

  // TODO: Not sure how to assert multilingual error messages

  val form = CreateChannel.form

  "CreateChannelForm" should {
    "be valid if within 32 chars" in {
      val inputLessThan32 = TestRandom.string(5)
      val boundForm = form.bind(Map("channelName" -> inputLessThan32))
      boundForm.hasErrors mustBe false
    }

    "be invalid if empty string" in {
      val boundForm = form.bind(Map("channelName" -> ""))
      boundForm.hasErrors mustBe true
    }

    "be invalid if more than 32 chars" in {

      val inputMoreThan32 = TestRandom.string(33)
      val boundForm = form.bind(Map("channelName" -> inputMoreThan32))
      boundForm.hasErrors mustBe true
    }
  }
}
