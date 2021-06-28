package controllers

import akka.actor.ActorSystem
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{Flow, Keep}
import com.mohiva.play.silhouette.api.crypto.Hash
import database.factories.{AuthenticatedClientFactory, ChannelFactory, ChannelTokenFactory, ClientLoginRequestFactory}
import database.tables.{AuthenticatedClients, ChannelTokens, Channels, ClientLoginRequests}
import helpers.crypto.Crypter
import helpers.streams.ChatStream
import modules.InputSanitizer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.Environment
import play.api.mvc.{BaseController, ControllerComponents, RequestHeader, WebSocket}
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import testhelpers.specificcontrollers.CreatingFakeChannel
import testhelpers.utils.TestRandom
import testhelpers.{TestCase, TestDatabaseConfiguration}

import java.net.{URI, URL}
import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ChatSpec extends CreatingFakeChannel
  with Injecting {

  class ChatController(implicit val controllerComponents: ControllerComponents) extends BaseController with Chat{
    override protected val inputSanitizer: InputSanitizer = injectDependency[InputSanitizer]
    override val channels = injectDependency[Channels]
    override val clientLoginRequests: ClientLoginRequests = injectDependency[ClientLoginRequests]
    override val authenticatedClients: AuthenticatedClients = injectDependency[AuthenticatedClients]
    override val channelTokens: ChannelTokens = injectDependency[ChannelTokens]

    override implicit protected val materializer: Materializer = injectDependency[Materializer]
    override implicit protected val actorSystem: ActorSystem = injectDependency[ActorSystem]
    override implicit protected val executionContext:  ExecutionContext = injectDependency[ExecutionContext]
    override implicit protected val environment: Environment = injectDependency[Environment]

    override protected def chatFlow(channel: String): Future[Flow[String, String, UniqueKillSwitch]] = {
      val stubFlow = Flow[String].joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
      Future.successful(stubFlow)
    }
  }

  implicit val controllerComponents = injectDependency[ControllerComponents]

  override def beforeEach(): Unit = {
    Await.result(ClientLoginRequestsTable.seed(clientLoginRequests), 2.seconds)
    Await.result(AuthenticatedClientsTable.seed(authenticatedClients), 2.seconds)
    Await.result(ChannelsTable.seed(channels), 2.seconds)
    Await.result(ChannelTokensTable.seed(channelTokens), 2.seconds)
  }

  val channelTokenModel = channelTokens.head
  val clientLoginRequestModel = clientLoginRequests.head
  val authenticatedClientModel = authenticatedClients.head
  val channelModel = channels.head
  val acceptableHost = "localhost"
//  val acceptablePorts = List(9000, 19001)
//  def getAcceptablePortRandomly() = acceptablePorts(TestRandom.int(acceptablePorts.length))


  /**
   * NOTICE: This method is used from controllers. Do not call directly.
   *   Handle as a controller method.
   *   This test is just a reminder
   */
  "ws test" should {
    "be done in HostControllerSpec and ClientControllerSpec" in new ChatController {
      val result = ws(hostChannelToken)
      result mustBe a [WebSocket]
    }
  }


  "createChatStream" should {
    "create flow that merge and broadcast" in new ChatController {
      val resultFut = createChatStream(hostChannelToken)
      whenReady(resultFut) {result =>
        result.isRight mustBe true
        result.getOrElse(0) mustBe a [Flow[_, _, _]]
      }
    }

    "return 500 status if exception is thrown" in {
      val exceptionThrowingChat = new ChatController {
        override def chatFlow(channel: String): Future[Flow[String, String, UniqueKillSwitch]] = {
          Future.failed(new Exception())
        }
      }

      val failedResult = exceptionThrowingChat.createChatStream(hostChannelToken)
      whenReady(failedResult) { resultTry =>
        resultTry match {
          case Left(failedFlow) => failedFlow.header.status mustBe INTERNAL_SERVER_ERROR
          case Right(x) => x mustBe false // fail any way
        }
      }
    }
  }

  "isSameOrigin" should {

    "return true if origin in headers is acceptable" in new ChatController {
      val requestHeader = getRequest("/fake")
        .withHeaders("Origin" -> s"http://${acceptableHost}")
      val result = isSameOrigin(requestHeader)
      result mustBe true
    }

    "return false if origin in headers is not acceptable" in new ChatController {
      val wrongOrigin = TestRandom.string(5)
      val requestHeader = getRequest("/fake")
        .withHeaders("Origin" -> wrongOrigin)
      val result = isSameOrigin(requestHeader)
      result mustBe false
    }
  }


  "isAcceptableOrigin" should {
    "return true if origin is acceptable origin" in new ChatController {
      val result = isAcceptableOrigin(s"http://${acceptableHost}")
      result mustBe true
    }

    "return false if hostname is not acceptable origin" in new ChatController {
      val notAcceptableHost = TestRandom.string(5)
      val result = isAcceptableOrigin(s"http://${notAcceptableHost}")
      result mustBe false
    }

    "return false if no origin is provided and not throw exception" in new ChatController {
      val result = isAcceptableOrigin("")
      result mustBe false
    }

  }



  "hasValidSessionValues" should {
    "return true if clients session values are valid with client channel token" in new ChatController {
      val requestHeader = getRequest("/fake").withSession(
        requestClientIdSessionKey -> requestClientId,
        authenticatedClientIdSessionKey -> authenticatedClientId
      )
      // NOTICE: use `clientchannelToken` if client
      val resultFut = hasValidSessionValues(requestHeader, clientChannelToken)
      whenReady(resultFut) { result => result mustBe true }
    }

    "return false if requestClientId is missing with client channel token" in new ChatController {
      val requestHeader = getRequest("/fake")
        .withSession(authenticatedClientIdSessionKey -> authenticatedClientId)
      val resultFut = hasValidSessionValues(requestHeader, clientChannelToken)
      whenReady(resultFut) { result => result mustBe false }
    }

    "return false if authenticatedClientId is missing with client channel token" in new ChatController {
      val requestHeader = getRequest("/fake")
        .withSession(requestClientIdSessionKey -> requestClientId)

      val resultFut = hasValidSessionValues(requestHeader, clientChannelToken)
      whenReady(resultFut) { result => result mustBe false }
    }


    "return false if no session data with client channel token" in new ChatController {
      val requestHeader = getRequest("/fake")
      val resultFut = hasValidSessionValues(requestHeader, clientChannelToken)
      whenReady(resultFut) { result => result mustBe false }
    }

    "return true if host session values are valid with host channel token" in new ChatController {
      val requestHeader = getRequest("/fake")
        .withSession(hostIdSessionKey -> hostId)
      // NOTICE: use `hostChannelToken` if host
      val resultFut = hasValidSessionValues(requestHeader, hostChannelToken)
      whenReady(resultFut) { result => result mustBe true }
    }

    "return false if host is is missing with host channel" in new ChatController {
      val requestHeader = getRequest("/fake")
      val resultFut = hasValidSessionValues(requestHeader, hostChannelToken)
      whenReady(resultFut) { result => result mustBe false }
    }


  }


  "isValidHost" should {
    "return true if hostChannelToken and host id are valid" in new ChatController {
      val resultFut = isValidHost(hostChannelToken, hostId)
      whenReady(resultFut) { result => result mustBe true}
    }
    "return false if hostChannelToken is not found in db" in new ChatController {
      val resultFut = isValidHost(TestRandom.string(5), hostId)
      whenReady(resultFut) { result => result mustBe false}
    }
    "return false if host id is not found in db" in new ChatController {
      val resultFut = isValidHost(hostChannelToken, TestRandom.string(5))
      whenReady(resultFut) { result => result mustBe false}
    }
  }

  "areSameHostChannel" should {
    "return true if channelToken's channel id and channel's channel id matched" in new ChatController {
      val result = areSameHostChannel(channelTokenModel, channelModel)
      result mustBe true
    }
    "return false if channelToken's channel id and channel's channel id are not the same" in new ChatController {
      val differentChannel = new ChannelFactory {}.create(1).head
      val result = areSameHostChannel(channelTokenModel, differentChannel)
      result mustBe false
    }
  }

  "isValidClient" should {
    "return true if token and ids belong to the same channel" in new ChatController {
      val resultFut = isValidClient(clientChannelToken, requestClientId, authenticatedClientId)
      whenReady(resultFut) { result => result mustBe true }
    }
    "return false if clientChannelToken is not found in db" in new ChatController {
      val resultFut = isValidClient(TestRandom.string(6), requestClientId, authenticatedClientId)
      whenReady(resultFut) { result => result mustBe false }
    }
    "return false if requestClientId is not found in db" in new ChatController {
      val resultFut = isValidClient(clientChannelToken, TestRandom.string(6), authenticatedClientId)
      whenReady(resultFut) { result => result mustBe false }
    }
    "return false if authenticatedClientId is not found in db" in new ChatController {
      val resultFut = isValidClient(clientChannelToken, requestClientId, TestRandom.string(6))
      whenReady(resultFut) { result => result mustBe false }
    }
  }

  "areSameClientChannel" should {
    "return true if they have the same channel id" in new ChatController {

      val result = areSameClientChannel(channelTokenModel, clientLoginRequestModel, authenticatedClientModel)
      result mustBe true
    }
    "return false if they don't have the same channel id" in new ChatController {
      val differentChannelToken = new ChannelTokenFactory {}.create(1).head
      val differentRequest = new ClientLoginRequestFactory {}.create(1).head
      val differentAuthenticated = new AuthenticatedClientFactory {}.create(1).head

      val result1 = areSameClientChannel(differentChannelToken, clientLoginRequestModel, authenticatedClientModel)
      result1 mustBe false

      val result2 = areSameClientChannel(channelTokenModel, differentRequest, authenticatedClientModel)
      result2 mustBe false

      val result3 = areSameClientChannel(channelTokenModel, clientLoginRequestModel, differentAuthenticated)
      result3 mustBe false
    }

    "return false if they have the same channel ids but clientLoginRequest's requestClientId and authenticatedClient's requestClientId are different" in new ChatController {
      // this means that this request have a valid channel id but have someone else's id in the chat room.
      // The person who is trying to join the chatroom is NOT the same person who requested to join the chat room.
      val differentAuthenticatedClient = new AuthenticatedClientFactory {
        override def authenticatedClientIdHash: String = Hash.sha2(authenticatedClientId)
        override def authenticatedClientIdEnc = Crypter.encrypt(authenticatedClientId, channelId)
        // someone in the same chat room is faking someone else's id
        override def requestClientIdHash: String = TestRandom.string(5)
        override def channelId: String = dummyChannelId
      }.create(1).head


      val result1 = areSameClientChannel(channelTokenModel, clientLoginRequestModel, differentAuthenticatedClient)
      result1 mustBe false

    }
  }
}
