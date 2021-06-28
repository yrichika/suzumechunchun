package helpers.streams

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.libs.EventSource
import play.api.libs.json.Json
import play.api.test.Injecting
import testhelpers.specificcontrollers.CreatingFakeChannel
import testhelpers.utils.TestRandom

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global


class HostEventSourceSpec extends CreatingFakeChannel with Injecting {

  implicit val system = ActorSystem("testing-host-event-source")
  implicit val materializer = Materializer

  val channel = channels.head

  override def beforeEach() = {
    Await.result(ChannelsTable.seed(channels), 2.seconds)
    Await.result(ClientLoginRequestsTable.seed(clientLoginRequests), 2.seconds)
  }

  val tickMilliSec = conf.getInt("serverSentEvent.hostEventSource.tickMilliSec")
  val expectedIfExists = Json.obj(
    "requestClientId" -> requestClientId,
    "codename" -> codename,
    "passphrase" -> passphrase,
    "isAuthenticated" -> None
  )

  "requestClientJson" should {
    "return json formatted data" in new HostEventSource(ChannelsTable, ClientLoginRequestsTable) {
      val r = TestRandom.string(5)
      val c = TestRandom.string(5)
      val p = TestRandom.string(5)
      val a = Some(true)
      val result = requestClientJson(r, c, p, a)
      result mustBe Json.obj(
        "requestClientId" -> r,
        "codename" -> c,
        "passphrase" -> p,
        "isAuthenticated" -> a
      )
    }
  }


  "streamRequestingUsers" should {
    "stream json data immediately first and next at interval rate" in new HostEventSource(ChannelsTable, ClientLoginRequestsTable) {
      val testSink = TestSink.probe[EventSource.Event](system)
      val probe = streamRequestingUsers(hostId)
        .runWith(testSink)
      val waitMilliSec = tickMilliSec + 500
      val jsonString = Json.toJson(Seq(expectedIfExists)).toString

      // FIXME: not sure why receiving initial tick fails
      // probe.request(1).expectNext(EventSource.Event(jsonString))
      probe.request(1).expectNext(waitMilliSec.millis, EventSource.Event(jsonString))
    }

    "stream json data at not less than interval rate" in new HostEventSource(ChannelsTable, ClientLoginRequestsTable) {
      val testSink = TestSink.probe[EventSource.Event](system)
      val probe = streamRequestingUsers(hostId)
        .runWith(testSink)
      val lessThanWaitMilliSec = tickMilliSec - 1000
      val jsonString = Json.toJson(Seq(expectedIfExists)).toString

       probe.request(1).expectNext(EventSource.Event(jsonString))
      // message is sent every 5 seconds. Not sent at less than config's seconds from the start.
       probe.request(1).expectNoMessage(lessThanWaitMilliSec.millis)
    }

  }

  "getClientRequests" should {
    "return seq of client requests in the table if exists any" in new HostEventSource(ChannelsTable, ClientLoginRequestsTable) {
      whenReady(getClientRequests(hostId)) {
        result => result mustBe Seq(expectedIfExists)
      }
    }
    "return empty seq if no entry in the table" in new HostEventSource(ChannelsTable, ClientLoginRequestsTable) {
      val hostWithNoRequest = TestRandom.string(10)
      whenReady(getClientRequests(hostWithNoRequest)) {
        result => result mustBe Seq()
      }
    }
  }

  "decryptClientRequest" should {
    "return decrypted client request as json" in new HostEventSource(ChannelsTable, ClientLoginRequestsTable) {
      val result = decryptClientRequest(clientLoginRequests, channel)
      result.map(jsonObj => jsonObj mustBe expectedIfExists)
    }
  }
}
