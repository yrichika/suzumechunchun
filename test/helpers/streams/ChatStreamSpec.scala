package helpers.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{Flow, Keep}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import testhelpers.{TestCase, TestDatabaseConfiguration}
import testhelpers.utils.TestRandom
import modules.InputSanitizer

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt




class ChatStreamSpec extends TestCase with TestDatabaseConfiguration {

  // Does not work with actor system created by `injectDependency` method
  // just create actor system from `ActorSystem()`
  // implicit val actorSystem: ActorSystem = injectDependency[ActorSystem]
  // implicit val materializer: Materializer = injectDependency[Materializer]
  implicit val system = ActorSystem("testing")
  implicit val materializer = Materializer

  val inputSanitizer = injectDependency[InputSanitizer]

  "chat stream" should {
    "merge many sources and broadcast to many sinks" in {
      val chatFlow = ChatStream.create(inputSanitizer)

      val test = createMultipleTestSourcesAndSink(3, chatFlow)

      val message1 = TestRandom.string(5)
      val message2 = TestRandom.string(5)
      val message3 = TestRandom.string(5)

      test(0).publisher.sendNext(message1)
      test(1).publisher.sendNext(message2)
      test(2).publisher.sendNext(message3)
      test(0).subscriber.request(3).expectNextUnordered(message1, message2, message3)
      test(1).subscriber.request(3).expectNextUnordered(message1, message2, message3)
      test(2).subscriber.request(3).expectNextUnordered(message1, message2, message3)
    }

    "ignore heartbeat message" in {
      val chatFlow = ChatStream.create(inputSanitizer)
      val test = createTestSourcesAndSink(chatFlow)

      test.publisher.sendNext(ChatStream.heartbeat) // should be ignored
      test.subscriber.request(1).expectNoMessage(500.millis)
    }

    "terminate sink if sources are terminated" in {
      val chatFlow = ChatStream.create(inputSanitizer)
      val test = createTestSourcesAndSink(chatFlow)

      test.publisher.sendComplete()
      test.subscriber.request(1).expectComplete()
    }

    // FIXME: Don't know how to test this
    "be shutdown by killswitch and stop stream" in {
//      val chatFlow = ChatStream.create(inputSanitizer)
//      val test = createTestSourcesAndSink(chatFlow)
//
//      val message = TestRandom.string(3)
//      test.publisher.sendNext(message)
//      test.subscriber.request(1).expectNext(message)
//
//      chatFlow.mapMaterializedValue(killSwitch => killSwitch.shutdown())
//
//      test.publisher.sendNext(message)
//      test.subscriber.request(1).expectNoMessage(500.millis)
    }
  }

  case class TestPublisherAndSubscriber(publisher: TestPublisher.Probe[String], subscriber: TestSubscriber.Probe[String])

  def createTestSourcesAndSink(chatFlow: Flow[String, String, UniqueKillSwitch]) = {
    val testSource = TestSource.probe[String]
    val testSink = TestSink.probe[String]
    val (publisher, subscriber) = testSource.via(chatFlow).toMat(testSink)(Keep.both).run()
    TestPublisherAndSubscriber(publisher, subscriber)
  }

  def createMultipleTestSourcesAndSink(howMany: Int, chatFlow: Flow[String, String, UniqueKillSwitch]) = {
    val publisherAndSubscriber = for (i <- 1 to howMany) yield {
      createTestSourcesAndSink(chatFlow)
    }
    publisherAndSubscriber.toList
  }



}
