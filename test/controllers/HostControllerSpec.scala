package controllers

import java.io.IOException
import play.api.test._
import testhelpers.specificcontrollers.CreatingFakeChannel
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.test.Helpers._
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import testhelpers.websocket.{JwtMaker, LogListener, TestCookie, WebSocketApp, WebSocketClient}
import testhelpers.utils.TestRandom
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import play.shaded.ahc.org.asynchttpclient.ws.WebSocket
import play.api.libs.json.Json


class HostControllerSpec extends CreatingFakeChannel
  with Injecting
{

  override def beforeEach() = {
    Await.result(ChannelsTable.seed(channels), 2.seconds)
    Await.result(ChannelTokensTable.seed(channelTokens), 2.seconds)
    Await.result(ClientLoginRequestsTable.seed(clientLoginRequests), 2.seconds)
  }


  "GET chat" should {
    val requestUrl = routes.HostController.chat(hostChannelToken).url
    "show host page" in {
      val request = FakeRequest(GET, requestUrl)
        .withSession("hostId" -> hostId, "secretKeyHost" -> secretKey)
        .withHeaders()

      val response = route(app, request).get

      status(response) mustBe OK
      val cspContent = s"connect-src +'self' wss?://.+/host/ws/$hostChannelToken"
      header("Content-Security-Policy", response).get must include regex(cspContent)
      val content = contentAsString(response)
      content must include regex(s"<h1.*>$channelName.*?</h1>")
      content must include regex(s"${routes.ClientController.request(loginChannelToken)}")
      content must include regex(s"chat.+?web-socket-url=.+?${routes.HostController.ws(hostChannelToken)}.+?")
      content must include regex(s"""<chat.+?is-host.*?>""")
      content must include regex(s"""<chat.+?codename="Host".*?>""")
      content must include regex(s"""<chat.+?secret-key="$secretKey".*?>""")
      content must include regex(s"manage-client-request(?s).+?sse-url=.+?${routes.HostController.sse(hostChannelToken)}")
      content must include regex(s"manage-client-request(?s).+?post-url=.+?${routes.HostController.manageRequest(hostChannelToken)}")
      content must include regex(s"manage-client-request(?s).+?close-request-url=.+?${routes.HostController.closeRequest(hostChannelToken)}")

    }

    "should redirect to unauthorized if no session" in {
      val request = FakeRequest(GET, requestUrl)
      val response = route(app, request).get
      status(response) mustBe UNAUTHORIZED
    }

    "should redirect to unauthorized if host id does not exist" in {
      val nonExistingHostId = TestRandom.string(3)
      val request = FakeRequest(GET, requestUrl)
        .withSession("hostId" -> nonExistingHostId, "secretKeyHost" -> secretKey)
        .withHeaders()
      val response = route(app, request).get

      status(response) mustBe UNAUTHORIZED

    }

    "should redirect to unauthorized if host id and channel token are not the same channel" in {
      ChannelsTable.seed(anotherChannels)
      ChannelTokensTable.seed(anotherChannelTokens)

      val request = FakeRequest(GET, requestUrl)
        // supply session with different channel's hostId and secretKey
        .withSession("hostId" -> anotherHostId, "secretKeyHost" -> anotherSecretKey)
        .withHeaders()
      val response = route(app, request).get

      status(response) mustBe UNAUTHORIZED
    }
  }


  "GET sse" should {
    val requestUrl = routes.HostController.sse(hostChannelToken).url
    "return Ok if provided with right host id" in {
      val request = FakeRequest(GET, requestUrl)
        .withSession("hostId" -> hostId, "secretKeyHost" -> secretKey)
        .withHeaders()
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some(EVENT_STREAM)
    }

    "give unauthorized if session with host id not given" in {
      val request = FakeRequest(GET, requestUrl)
      val response = route(app, request).get
      status(response) mustBe UNAUTHORIZED
    }
  }


  "POST manageRequest" should {
    val request = postRequest(
      routes.HostController.manageRequest(hostChannelToken).url,
      s"""{"requestClientId": "$requestClientId", "status": "true"}"""
    )
    "update requested client status" in {

      val response = route(app, request).get

      status(response) mustBe OK
      whenReady(ClientLoginRequestsTable.getByRequestClientId(requestClientId)) {
        result => result.get.isAuthenticated mustBe Some(true)
      }
    }
    "create authenticated client based on request client id" in {

      whenReady(AuthenticatedClientsTable.getByRequestClientId(requestClientId)) {
        result => result mustBe None
      }

      val response = route(app, request).get

      status(response) mustBe OK
      whenReady(AuthenticatedClientsTable.getByRequestClientId(requestClientId)) {
        result => result.get.channelId mustBe dummyChannelId
      }
    }

    "return json value ok" in {
      // fixme: Request should have host id. Fix the implementation and then this test

      val response = route(app, request).get

      status(response) mustBe OK
      contentAsJson(response) mustBe Json.toJson("ok")
    }

    "return json error value" in {
      val nonExistingId = TestRandom.string(10)
      val request = postRequest(
        s"/host/manage-request/$nonExistingId",
        s"""{"requestClientId": "$requestClientId", "status": "true"}"""
      )
      val response = route(app, request).get
      status(response) mustBe BAD_REQUEST
      contentAsJson(response) mustBe Json.toJson("channel does not exits")
    }
  }


  "GET closeRequest" should {
    val requestUrl = FakeRequest(routes.HostController.closeRequest(hostChannelToken))
    "delete secret key from channel_tokens table" in {
      // fixme: Request should have host id. Fix the implementation and then this test

      val response = route(app, requestUrl).get

      status(response) mustBe OK
      whenReady(ChannelTokensTable.getByHostChannelToken(hostChannelToken)) {
        result => result.get.secretKeyEnc mustBe None
      }
    }

    "return json ok message" in {
      val response = route(app, requestUrl).get

      status(response) mustBe OK
      contentAsString(response) mustBe Json.toJson("message" -> "ok").toString()
    }
  }



  // https://github.com/playframework/play-samples/tree/2.8.x/play-scala-chatroom-example
  "WebSocket ws" should {
    val port = Helpers.testServerPort
    val server = s"localhost:${Helpers.testServerPort}"
    val webSocketUrl = s"ws://$server" + routes.HostController.ws(hostChannelToken).url

    "accept a websocket flow if the origin is correct and host id exists in the cookie" in WsTestClient.withClient { client =>
      val webSocketApp = WebSocketApp.create()
      Helpers.running(TestServer(port, webSocketApp)) {
        val asyncHttpClient: AsyncHttpClient = client.underlying[AsyncHttpClient]

        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)
        val jwt = JwtMaker.hostSession(hostId, secretKey)

        val cookie = TestCookie("PLAY_SESSION", jwt)
        val result = webSocketClient.addCookie(cookie).call()
        whenReady(result, timeout = Timeout(1.second)) { webSocket =>
          webSocket mustBe a [WebSocket]
        }
      }
    }

    "reject a websocket flow if the origin is incorrect" in WsTestClient.withClient{ client =>
      // !important: use different Application instances on every websocket test
      val webSocketApp = WebSocketApp.create()
      Helpers.running(TestServer(port, webSocketApp)) {

        val asyncHttpClient: AsyncHttpClient = client.underlying[AsyncHttpClient]
        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)

        val wrongOrigin = s"ws://wrong-origin.com/host/ws/$hostChannelToken"
        val result = webSocketClient.call(wrongOrigin)

        Await.result(result, atMost = 1000.millis)
        webSocketClient.caughtThrowable() mustBe an [IOException]
      }
    }


    "reject a websocket flow if no host id in session even if origin is correct" in WsTestClient.withClient { client =>
      val webSocketApp = WebSocketApp.create()
      Helpers.running(TestServer(port, webSocketApp)) {

        val asyncHttpClient: AsyncHttpClient = client.underlying[AsyncHttpClient]
        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)

        val result = webSocketClient.call()

        Await.result(result, atMost = 1000.millis)
        webSocketClient.caughtThrowable() mustBe an [IOException]
      }
    }

  }

}
