package helpers.streams

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import database.models.ClientLoginRequest
import database.tables.{ChannelTokens, ClientLoginRequests}
import helpers.crypto.Crypter
import play.api.libs.EventSource
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * This is intended to be a single object per controller.
 * Always instantiate as a class member.
 * Do NOT instantiate as a variable in methods.
 */
class ClientEventSource(
 val clientLoginRequests: ClientLoginRequests,
 val channelTokens: ChannelTokens)
(
   implicit val actorSystem: ActorSystem,
   val materializer: Materializer
 ) {
  implicit val executionContext = actorSystem.dispatchers.lookup("serverSentEvent.clientEventSource.tickDispatcher")
  val channelResponseJsonKey = "clientChannel"
  val rejectedStatusString = "__rejected__"
  val rejectedRequestJson = Json.obj(channelResponseJsonKey -> rejectedStatusString)
  val emptyChannelJson = Json.obj(channelResponseJsonKey -> "")
  val closedStatusString = "__closed__"
  val clientRequestClosedJson = Json.obj(channelResponseJsonKey -> closedStatusString)
  def acceptedRequestJson(clientChannelToken: String) = Json.obj(channelResponseJsonKey -> clientChannelToken)

  val tickMilliSeconds = ConfigFactory.load.getInt("serverSentEvent.clientEventSource.tickMilliSec")

  def streamCurrentAuthenticatedStatus(requestClientId: String) = {
    val tickSource = Source.tick(0.millis, tickMilliSeconds.millis, "TICK")
    tickSource.mapAsync(1)(_ => getAuthenticatedClient(requestClientId))
      .map(data => Json.toJson(data))
      .via(EventSource.flow)
  }


  def getAuthenticatedClient(requestClientId: String): Future[JsObject] = {
    clientLoginRequests.getByRequestClientId(requestClientId).flatMap {
      case Some(request) => {
        request.isAuthenticated match {
          case Some(true) => getClientChannel(request)
          case Some(false) => Future.successful(rejectedRequestJson)
          case None => Future.successful(emptyChannelJson)
        }
      }
      // this case below: requestClientId value in session is wrong
      // which means that user must've put wrong session value (maybe intentionally) in header
      // Maybe it's a sign of an attack.
      // it can be `rejectedRequestJson` or `clientRequestClosedJson`
      case None => Future.successful(clientRequestClosedJson)
    }
  }

  def getClientChannel(request: ClientLoginRequest): Future[JsObject] = {
    channelTokens.getByChannelId(request.channelId).map {
      case Some(channelToken) => {
        // if there is the secret key, then chat participation is not closed yet
        channelToken.secretKeyEnc match {
          case Some(key) => {
            val clientChannel = Crypter.decrypt(channelToken.clientChannelTokenEnc, channelToken.channelId.toString)
            acceptedRequestJson(clientChannel)
          }
          // if there is the secret key, then chat participation is closed
          case None => clientRequestClosedJson
        }
      }
      case None => emptyChannelJson
    }
  }
}
