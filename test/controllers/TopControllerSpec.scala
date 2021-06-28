package controllers


import helpers.crypto.Crypter
import testhelpers.specificcontrollers.CreatingFakeChannel
import org.scalatest.concurrent.ScalaFutures
import play.api.test._
import play.api.test.Helpers._
import testhelpers.utils.TestRandom

import scala.concurrent.ExecutionContext.Implicits.global


class TopControllerSpec extends CreatingFakeChannel
  with Injecting
  with ScalaFutures
{


  "GET index" should {
    val requestUrl = routes.TopController.index().url
    "show top page" in {
      val request = getRequest(requestUrl).withHeaders()
      val response = route(app, request).get

      status(response) mustBe OK
      // `?` after quantifiers [*, +, ?, {}] will make it non-greedy.
      contentAsString(response) must include regex ("<h1.*>SuzumeChunChun.*?</h1>")
      contentAsString(response) must include regex ("<input.+type=\"text\".+name=\"channelName\".*?>")
    }

    "redirect to chat page if it already created chat session" in {

      val request = getRequest(requestUrl)
        .withSession("hostId" -> hostId, "secretKeyHost" -> secretKey)
        .withHeaders()
      val response = route(app, request).get

      redirectLocation(response).get mustBe (routes.HostController.chat(hostId).toString())
      session(response).get("hostId").get mustBe (hostId)
      session(response).get("secretKeyHost").get mustBe (secretKey)

    }
  }

  /**
   * validation tests are done in `test.forms.*`.
   */
  "POST validate" should {
    val requestUrl = routes.TopController.validate().url
    "redirect to /create" in {
      val channelName = TestRandom.string(5)
      val request = postRequest(requestUrl, s"""{ "channelName": "$channelName" }""")
      val response = route(app, request).get

      redirectLocation(response).get mustBe (routes.TopController.create().url)
    }
  }

  "GET create" should {
    val requestUrl = routes.TopController.create().url
    val channelName = TestRandom.string(5)
    "create channel data in db" in {
      // Don't put this `getRequest` line out of this brackets scope. It will likely to fail when testing all tests
      val request = getRequest(requestUrl).withSession("channelName"-> channelName)

      val response = route(app, request).get

      status(response) mustBe SEE_OTHER
      whenReady(ChannelTokensTable.all()) {
        result => {
          val storedChannelName = Crypter.decrypt(result.head.channelNameEnc, result.head.channelId)
          storedChannelName mustBe channelName
        }
      }

    }

    "redirect to host page" in {
      val request = getRequest(requestUrl).withSession("channelName"-> channelName)
      val response = route(app, request).get

      redirectLocation(response).get must fullyMatch regex routes.HostController.chat(".+").toString()
    }
  }


}
