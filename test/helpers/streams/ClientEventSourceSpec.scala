package helpers.streams

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import database.factories.ClientLoginRequestFactory
import database.models.ClientLoginRequest
import forms.ClientStatus
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.libs.EventSource
import play.api.test.Injecting
import testhelpers.specificcontrollers.CreatingFakeChannel
import play.api.libs.json.{JsObject, JsValue, Json}
import testhelpers.utils.TestRandom

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt


class ClientEventSourceSpec extends CreatingFakeChannel with Injecting {

  implicit val system = ActorSystem("testing")
  implicit val materializer = Materializer

  override def beforeEach() = {
    Await.result(ChannelsTable.seed(channels), 2.seconds)
    Await.result(ChannelTokensTable.seed(channelTokens), 2.seconds)
    Await.result(ClientLoginRequestsTable.seed(clientLoginRequests), 2.seconds)
    Await.result(AuthenticatedClientsTable.seed(authenticatedClients), 2.seconds)
  }

  val clientLoginRequest = clientLoginRequests.head

  val tickMilliSec = conf.getInt("serverSentEvent.clientEventSource.tickMilliSec")

  "streamCurrentAuthenticatedStatus" should {
    "stream json data immediately first and next at interval rate" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      val testSink = TestSink.probe[EventSource.Event](system)
      val probe = streamCurrentAuthenticatedStatus(requestClientId)
        .runWith(testSink)
      val waitMilliSec = tickMilliSec + 500

      probe.request(1).expectNext(EventSource.Event(emptyChannelJson.toString()))
      probe.request(1).expectNext(waitMilliSec.millis, EventSource.Event(emptyChannelJson.toString()))
    }

    "stream json data at not less than interval rate" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      val testSink = TestSink.probe[EventSource.Event](system)
      val probe = streamCurrentAuthenticatedStatus(requestClientId)
        .runWith(testSink)
      val lessThanWaitMilliSec = tickMilliSec - 1000

      probe.request(1).expectNext(EventSource.Event(emptyChannelJson.toString()))
      // message is sent every 5 seconds. Not sent at 2 seconds from the start.
       probe.request(1).expectNoMessage(lessThanWaitMilliSec.millis)
    }
  }

  "getAuthenticatedClient" should {
    "get empty json data if authenticated is None" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      whenReady(getAuthenticatedClient(requestClientId)) {
        result => result mustBe emptyChannelJson
      }
    }
    "get if authenticated is false" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      val clientStatusRejected = ClientStatus(requestClientId, false)
      Await.result(ClientLoginRequestsTable.updateStatus(clientStatusRejected), 2.seconds)
      whenReady(getAuthenticatedClient(requestClientId)) {
        result => result mustBe rejectedRequestJson
      }
    }

    "get if authenticated is true" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      val clientStatusAccepted = ClientStatus(requestClientId, true)
      Await.result(ClientLoginRequestsTable.updateStatus(clientStatusAccepted), 2.seconds)
      whenReady(getAuthenticatedClient(requestClientId)) {
        result => result mustBe Json.obj(channelResponseJsonKey -> clientChannelToken)
      }
    }

    "get closed if requestClientId does not match any in the table" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      val nonExistingRequestClientId = TestRandom.string(10)
      whenReady(getAuthenticatedClient(nonExistingRequestClientId)) {
        result => result mustBe clientRequestClosedJson
      }
    }
  }

  "getClientChannel" should {
    "get client channel token json from clientLoginRequest object" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      whenReady(getClientChannel(clientLoginRequest)) {
        result => result mustBe Json.obj(channelResponseJsonKey -> clientChannelToken)
      }
    }

    "return empty json data if channel does not exists" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      val nonExistingClientRequest = new ClientLoginRequestFactory {}.create(1).head
      whenReady(getClientChannel(nonExistingClientRequest)) {
        result => result mustBe emptyChannelJson
      }
    }

    "return closed json data if secret key does not exists" in new ClientEventSource(ClientLoginRequestsTable, ChannelTokensTable) {
      Await.result(ChannelTokensTable.trashSecretKeyByHostChannelToken(hostChannelToken), 2.seconds)
      whenReady(getClientChannel(clientLoginRequest)) {
        result => result mustBe clientRequestClosedJson
      }
    }
  }


}
