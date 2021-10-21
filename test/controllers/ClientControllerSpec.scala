package controllers


import akka.actor.ActorSystem
import akka.stream.Materializer
import com.github.benmanes.caffeine.cache.AsyncCache
import helpers.streams.ChatStream
import modules.InputSanitizer
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api
import play.api.cache.AsyncCacheApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient
import testhelpers.specificcontrollers.CreatingFakeChannel
import testhelpers.utils.TestRandom
import testhelpers.websocket.WebSocketApp.setAppConfiguration
import testhelpers.websocket.{JwtMaker, TestCookie, WebSocketApp, WebSocketClient}
import play.shaded.ahc.org.asynchttpclient.ws.WebSocket

import java.io.IOException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.matching.Regex

class ClientControllerSpec extends CreatingFakeChannel with Injecting {

  val cache = injectDependency[AsyncCacheApi]
  val inputSanitizer = injectDependency[InputSanitizer]
  implicit val system = ActorSystem("testing-client-controller")
  implicit val materializer = Materializer

  override def beforeEach() = {
    Await.result(ChannelsTable.seed(channels), 2.seconds)
    Await.result(ChannelTokensTable.seed(channelTokens), 2.seconds)
    Await.result(ClientLoginRequestsTable.seed(clientLoginRequests), 2.seconds)
    Await.result(AuthenticatedClientsTable.seed(authenticatedClients), 2.seconds)
  }

  "GET request" should {
    val url = routes.ClientController.request(loginChannelToken).url
    "show request view" in {
      val request = getRequest(url)
      val response = route(app, request).get

      status(response) mustBe OK
      contentAsString(response) must include regex(s"${channelName}")
      // (?s) for dot all. matching multiline
      contentAsString(response) must include regex("(?s)<client-login-request.*?>")
      contentAsString(response) must include regex(s"""sse-url=['"]${routes.ClientController.sse(loginChannelToken)}['"]""")
      contentAsString(response) must include regex(s"""request-url=['"]${routes.ClientController.validate(loginChannelToken)}['"]""")
      contentAsString(response) must include regex(s"""chat-url=['"]https?://.+/client/chat/view/['"]""")
    }

    "redirect to not found page" in {
      val wrongToken = TestRandom.string(5)
      val request = FakeRequest(GET, routes.ClientController.request(wrongToken).url)
      val response = route(app, request).get

      status(response) mustBe NOT_FOUND
    }

    "show bad request page if you have host session data" in {
      val request = getRequest(url)
        .withSession("hostId" -> TestRandom.string(5))
      val response = route(app, request).get
      status(response) mustBe BAD_REQUEST
      // asserting error message because this message is kind of important.
      contentAsString(response) must include regex(s"""(?s)If you are a host and want to.+もし同じブラウザから同時に""")
    }
  }

  "POST validate" should {
    val requestUrl = routes.ClientController.validate(loginChannelToken).url
    "return ok status with authenticated session value" in {
      val codename = TestRandom.string(5)
      val passphrase = TestRandom.string(5)
      val request = postRequest(
        requestUrl,
        s"""{"codename": "${codename}", "passphrase": "${passphrase}"}"""
      )
      val response = route(app, request).get
      status(response) mustBe OK
      Json.parse(contentAsJson(response).toString) mustBe Json.obj("message" -> "ok")
      // any random string/UUID
      session(response).get("requestClientId").get must include regex (".+")
    }

    "return bad request status if submit with empty value" in {
      val request = postRequest(
        requestUrl,
        s"""{"codename": "", "passphrase": ""}"""
      )
      val response = route(app, request).get

      status(response) mustBe BAD_REQUEST
      contentAsString(response) must include regex s"""\\{.*"passphrase":.+\\}"""
      contentAsString(response) must include regex s"""\\{.*"codename":.+\\}"""
    }

    "return closed status string in json" in {
      val codename = TestRandom.string(5)
      val passphrase = TestRandom.string(5)
      Await.result(ChannelTokensTable.trashSecretKeyByHostChannelToken(hostChannelToken), 2.seconds)

      val request = postRequest(
        requestUrl,
        s"""{"codename": "${codename}", "passphrase": "${passphrase}"}"""
      )
      val response = route(app, request).get

      status(response) mustBe OK
      Json.parse(contentAsJson(response).toString) mustBe Json.obj("message" -> "__closed__")

    }
  }

  "GET chat" should {
    val requestUrl = routes.ClientController.chat(clientChannelToken).url
    "show chat view" in {
      val request = getRequest(requestUrl)
        .withSession("requestClientId" -> requestClientId)

      val response = route(app, request).get

      status(response) mustBe OK
      val cspContent = s"""connect-src 'self' wss?://.+/client/chat/ws/$clientChannelToken"""
      header("Content-Security-Policy", response).get must include regex(cspContent)
      contentAsString(response) must include regex(s"${channelName}")
      contentAsString(response) must include regex (s"""<chat.+>""")
      contentAsString(response) must include regex(s"""web-socket-url=['"]wss?://.+/client/chat/ws/${clientChannelToken}['"]""")
      contentAsString(response) must include regex(s"""codename=['"]${codename}['"]""")
      contentAsString(response) must include regex(s"""secret-key=['"]${secretKey}['"]""")
    }

    "return unauthorized if requestClientId has a wrong value in session" in {
      val wrongRequestClientId = TestRandom.string(10)
      val request = getRequest(requestUrl)
        .withSession("requestClientId" -> wrongRequestClientId)
      val response = route(app, request).get
      status(response) mustBe UNAUTHORIZED
    }
  }


  // streaming test is done in helpers.streams.ClientEventSource
  "GET sse" should {
    val requestUrl = routes.ClientController.sse(loginChannelToken).url
    "return content as stream" in {
      val request = getRequest(requestUrl)
        .withSession("requestClientId" -> requestClientId)
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some(EVENT_STREAM)
    }
  }




  "WebSocket ws" should {
    val port = Helpers.testServerPort
    val server = s"localhost:${Helpers.testServerPort}"
    val webSocketUrl = s"ws://${server}" + routes.ClientController.ws(clientChannelToken).url

    // cache before accessing websocket
    cache.set(clientChannelToken, ChatStream.create(inputSanitizer))

    "return websocket if websocket exists in cache" in WsTestClient.withClient { client =>

      val webSocketApp = WebSocketApp.createWithCache(cache)
      Helpers.running(TestServer(port, webSocketApp)) {
        val asyncHttpClient = client.underlying[AsyncHttpClient]

        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)
        val jwt = JwtMaker.clientSession(requestClientId, authenticatedClientId)
        val cookie = TestCookie("PLAY_SESSION", jwt)
        val result = webSocketClient.addCookie(cookie).call()
        whenReady(result, timeout = Timeout(1.second)) { webSocket =>
          webSocket mustBe a [WebSocket]
        }
      }
    }


    "reject websocket connection if the origin is not valid" in WsTestClient.withClient { client =>
      val webSocketApp = WebSocketApp.createWithCache(cache)
      Helpers.running(TestServer(port, webSocketApp)) {
        val asyncHttpClient = client.underlying[AsyncHttpClient]
        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)
        val wrongServerName = TestRandom.string(5)
        val wrongOrigin = s"ws://$wrongServerName/client/chat/ws/$clientChannelToken"
        val result = webSocketClient.call(wrongOrigin)

        Await.result(result, atMost = 1000.millis)
        webSocketClient.caughtThrowable() mustBe an [IOException]
      }
    }

    "reject a websocket connection if client session is not valid" in WsTestClient.withClient { client =>
      val webSocketApp = WebSocketApp.createWithCache(cache)
      Helpers.running(TestServer(port, webSocketApp)) {
        val asyncHttpClient = client.underlying[AsyncHttpClient]
        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)

        val wrongSession = if (TestRandom.boolean()) {
          (TestRandom.string(10), authenticatedClientId)
        } else {
          (requestClientId, TestRandom.string(10))
        }

        val jwt = JwtMaker.clientSession(wrongSession._1, wrongSession._2)
        val cookie = TestCookie("PLAY_SESSION", jwt)
        val result = webSocketClient.addCookie(cookie).call()

        Await.result(result, atMost = 1000.millis)
        webSocketClient.caughtThrowable() mustBe an [IOException]
      }
    }

    "reject a websocket connection if client session is missing" in WsTestClient.withClient { client =>
      val webSocketApp = WebSocketApp.createWithCache(cache)
      Helpers.running(TestServer(port, webSocketApp)) {
        val asyncHttpClient = client.underlying[AsyncHttpClient]
        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)

        val result = webSocketClient.call()

        Await.result(result, atMost = 1000.millis)
        webSocketClient.caughtThrowable() mustBe an [IOException]
      }
    }

    "return exception if websocket does not exist in cache" in WsTestClient.withClient { client =>
      cache.removeAll()
      val webSocketApp = WebSocketApp.createWithCache(cache)
      Helpers.running(TestServer(port, webSocketApp)) {
        val asyncHttpClient = client.underlying[AsyncHttpClient]

        val webSocketClient = new WebSocketClient(asyncHttpClient, webSocketUrl)
        val jwt = JwtMaker.clientSession(requestClientId, authenticatedClientId)
        val cookie = TestCookie("PLAY_SESSION", jwt)
        val result = webSocketClient.addCookie(cookie).call()

        Await.result(result, atMost = 1000.millis)
        webSocketClient.caughtThrowable() mustBe an [IOException]

      }
    }

  }

}
