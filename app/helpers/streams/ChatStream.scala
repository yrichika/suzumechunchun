package helpers.streams

import akka.actor.ActorSystem
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import modules.InputSanitizer

import scala.concurrent.duration._


object ChatStream {
  val heartbeat = "__ping__"

  def create(inputSanitizer: InputSanitizer)(implicit mat: Materializer): Flow[String, String, UniqueKillSwitch] = {
    val source = MergeHub.source[String]
      .filter(!_.contains(heartbeat))
      .map(inputSanitizer.sanitize)
      .recoverWithRetries(-1, {case _: Exception => Source.empty})
    val sink = BroadcastHub.sink[String]
    val (chatSink, chatSource) = source.toMat(sink)(Keep.both).run()
    Flow.fromSinkAndSourceCoupled(chatSink, chatSource)
      // FIXME: Not sure if this kill switch works
      .joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
      .backpressureTimeout(5.seconds)
  }
}
