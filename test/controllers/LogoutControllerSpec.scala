package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import helpers.streams.ChatStream
import modules.InputSanitizer
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.cache.AsyncCacheApi
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import testhelpers.specificcontrollers.CreatingFakeChannel

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class LogoutControllerSpec extends CreatingFakeChannel with Injecting {

  val cache = injectDependency[AsyncCacheApi]
  val inputSanitizer = injectDependency[InputSanitizer]
  implicit val system = ActorSystem("testing-client-controller")
  implicit val materializer = Materializer

  override def beforeEach() = {
    Await.result(ChannelsTable.seed(channels), 2.seconds)
    Await.result(ChannelTokensTable.seed(channelTokens), 2.seconds)
    Await.result(ClientLoginRequestsTable.seed(clientLoginRequests), 2.seconds)
  }

  "GET /logout/[token]" should {
    "redirect to the top page" in {
      val chatStream = ChatStream.create(inputSanitizer)
      cache.set(hostChannelToken, chatStream)
      cache.set(clientChannelToken, chatStream)
      val request = FakeRequest(POST, s"/logout/$hostChannelToken")

      val response = route(app, request).get
      status(response) mustBe SEE_OTHER
      val redirectTo = routes.TopController.index().toString()
      redirectLocation(response).get mustBe redirectTo
    }

    "delete cached streams" in {
      val chatStream = ChatStream.create(inputSanitizer)
      cache.set(hostChannelToken, chatStream)
      cache.set(clientChannelToken, chatStream)
      val request = FakeRequest(POST, s"/logout/$hostChannelToken")

      val response = route(app, request).get

      status(response) mustBe SEE_OTHER
      whenReady(cache.get[Any](hostChannelToken) ){ result =>
        result mustBe None
      }
      whenReady(cache.get[Any](clientChannelToken)) { result =>
        result mustBe None
      }
    }

    "delete the secret key" in {
      val chatStream = ChatStream.create(inputSanitizer)
      cache.set(hostChannelToken, chatStream)
      cache.set(clientChannelToken, chatStream)
      val request = FakeRequest(POST, s"/logout/$hostChannelToken")

      val response = route(app, request).get

      status(response) mustBe SEE_OTHER
      whenReady(ChannelTokensTable.getByHostChannelToken(hostChannelToken)) {
        result => result.get.secretKeyEnc mustBe None
      }
    }
  }
}
