package helpers.streams

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.config.ConfigFactory
import database.models.{Channel, ClientLoginRequest}
import database.tables.{Channels, ClientLoginRequests}
import helpers.crypto.Crypter
import play.api.libs.EventSource
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class HostEventSource
(val channels: Channels, clientLoginRequests: ClientLoginRequests)
(
  implicit val actorSystem: ActorSystem,
  val materializer: Materializer
)
{
  implicit val executionContext = actorSystem.dispatchers.lookup("serverSentEvent.hostEventSource.tickDispatcher")
  val tickMilliSeconds = ConfigFactory.load().getInt("serverSentEvent.hostEventSource.tickMilliSec")

  def requestClientJson(requestClientId: String, codename: String, passphrase: String, isAuthenticated: Option[Boolean]) = {
    Json.obj(
      "requestClientId" -> requestClientId,
      "codename" -> codename,
      "passphrase" -> passphrase,
      "isAuthenticated" -> isAuthenticated
    )
  }

  def streamRequestingUsers(hostId: String) = {
    val tickSource = Source.tick(0.millis, tickMilliSeconds.millis, "TICK")
    tickSource.mapAsync(1)(_ => getClientRequests(hostId))
      .map(data => Json.toJson(data))
      .via(EventSource.flow)
  }


  def getClientRequests(hostId: String) = {
    channels.getByHostId(hostId).flatMap {
      case Some(channel) => {
        clientLoginRequests.getByChannelId(channel.channelId).map(clientRequests => {
          decryptClientRequest(clientRequests, channel)
        })
      }
      case None => Future.successful(Seq())

    }
  }


  def decryptClientRequest(clientRequests: Seq[ClientLoginRequest], channel: Channel) = {
    clientRequests.map(request => {
      val requestClientId = Crypter.decrypt(request.requestClientIdEnc,channel.channelId.toString)
      val codename = Crypter.decrypt(request.codenameEnc.getOrElse(Array[Byte]()), channel.channelId.toString)
      val passphrase = Crypter.decrypt(request.passphraseEnc.getOrElse(Array[Byte]()), channel.channelId.toString)
      val isAuthenticated = request.isAuthenticated
      requestClientJson(requestClientId, codename, passphrase, isAuthenticated)
    })

  }

}
