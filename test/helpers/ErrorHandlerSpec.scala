package helpers

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import testhelpers.UnitTestCase
import testhelpers.utils.TestRandom

import scala.concurrent.Await
import scala.concurrent.duration._

class ErrorHandlerSpec extends UnitTestCase {

  implicit val system = ActorSystem("testing")
  implicit val materializer = Materializer
  "onClientError" should {
    val request = FakeRequest(GET, "/")

    "return e400 page" in new ErrorHandler {
      val statusCode = TestRandom.intBetween(400, 499)
      val message = TestRandom.string(10)
      whenReady(onClientError(request, statusCode, message)) { result =>
        result.header.status shouldBe statusCode
        val htmlContent = Await.result(result.body.consumeData, 2.seconds)
        htmlContent.utf8String should include regex("id=\"e400-content\"")
        htmlContent.utf8String should include regex(s"Sorry!.+$statusCode.+Error")
        htmlContent.utf8String should include regex(s"$message")

      }
    }
  }

  "onServerError" should {
    val request = FakeRequest(GET, "/")

    "return e500 page" in new ErrorHandler {
      val message = TestRandom.string(10)
      val exception = new Exception(message)
      whenReady(onServerError(request, exception)) { result =>
        result.header.status shouldBe INTERNAL_SERVER_ERROR
        val htmlContent = Await.result(result.body.consumeData, 2.seconds)
        htmlContent.utf8String should include regex("id=\"e500-content\"")
        htmlContent.utf8String should include regex(s"Sorry!.+$INTERNAL_SERVER_ERROR.+Error")
        htmlContent.utf8String should include regex(s"$message")
      }
    }
  }
}
