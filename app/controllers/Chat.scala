package controllers

import java.net.URI
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import com.typesafe.config.ConfigFactory
import database.models.{AuthenticatedClient, Channel, ChannelToken, ClientLoginRequest}
import database.tables.{AuthenticatedClients, ChannelTokens, Channels, ClientLoginRequests}
import helpers.Log
import modules.InputSanitizer
import play.api.{Environment, Logger}
import play.api.mvc.BaseController
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait Chat extends RequestMarkerContext {
  this: BaseController =>

  // Inject these classes and assign injected instances to these
  protected val inputSanitizer: InputSanitizer
  implicit protected val materializer: Materializer
  implicit protected val actorSystem: ActorSystem
  implicit protected val executionContext: ExecutionContext
  implicit protected val environment: Environment
  val channels: Channels
  val clientLoginRequests: ClientLoginRequests
  val authenticatedClients: AuthenticatedClients
  val channelTokens: ChannelTokens
  val validHosts = ConfigFactory.load().getStringList("play.filters.hosts.allowed")
  protected def chatFlow(channelToken: String): Future[Flow[String, String, UniqueKillSwitch]]


  val requestClientIdSessionKey = "requestClientId"
  val authenticatedClientIdSessionKey = "authenticatedClientId"
  val hostIdSessionKey = "hostId"


  /**
   * This `ws` method's test should be done in HostControllerSpec and ClientControllerSpec separately
   * because this method is called directly from http request.
   * Do not test in ChatSpec
   */
  def ws(HostOrClientChannelToken: String): WebSocket = WebSocket.acceptOrResult[String, String] { request =>
    request match {
      case requestHeader if isSameOrigin(requestHeader) => {
        hasValidSessionValues(requestHeader, HostOrClientChannelToken).flatMap {
          hasValidSessionValue => hasValidSessionValue match {
            case true => createChatStream(HostOrClientChannelToken)
            case false => Future.successful(Left(Forbidden("Not valid credential")))
          }
        }
      }
      case rejected => {
        Log.console.error(s"Request ${rejected} failed same origin check.")
        Future.successful(Left(Forbidden("forbidden")))
      }
    }
  }


  def createChatStream(channelToken: String) = {
    chatFlow(channelToken)
      .map(flow => {
        Log.devOnly("WebSocket connected")
        Right(flow)
      })
      // FIXME: Not sure this is a right way to implement flow when failed
      .recover {
        case exception: Exception => {
          val message = "cannot create websocket"
          Log.console.error(message, exception)
          val result = InternalServerError(message)
          Left(result)
        }
      }
  }


  protected def isSameOrigin(requestHeader: RequestHeader): Boolean = {
    Log.devOnly("Checking the ORIGIN ")
    requestHeader.headers.get("Origin") match {
      case Some(originValue) if isAcceptableOrigin(originValue) =>
        Log.devOnly(s"originCheck: originValue = ${originValue}")
        true
      case Some(badOrigin) =>
        Log.console.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false
      case None =>
        Log.console.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }


  protected def isAcceptableOrigin(origin: String): Boolean = {
    try {
      val url = new URI(origin)
      validHosts.contains(url.getHost)
    }
    // when origin is empty string
    catch {
      case exception: Exception => false
    }
  }


  protected def hasValidSessionValues(requestHeader: RequestHeader, channelToken: String): Future[Boolean] = {

    val requestClientId = requestHeader.session.get(requestClientIdSessionKey)
    val authenticatedClientId = requestHeader.session.get(authenticatedClientIdSessionKey)
    val hostId = requestHeader.session.get(hostIdSessionKey)

    if (requestClientId.nonEmpty && authenticatedClientId.nonEmpty) {
      isValidClient(channelToken, requestClientId.get, authenticatedClientId.get)
    }
    else if (hostId.nonEmpty) {
      isValidHost(channelToken, hostId.get)
    }
    else {
      Log.console.error("No session! Not Logged in!")
      Future.successful(false)
    }
  }

  def isValidHost(hostChannelToken: String, hostId: String): Future[Boolean] = {
    val channelFut = channels.getByHostId(hostId)
    val channelTokenFut = channelTokens.getByHostChannelToken(hostChannelToken)
    for {
      channelOpt <- channelFut
      channelTokenOpt <- channelTokenFut
    } yield {
      if (channelOpt.isEmpty || channelTokenOpt.isEmpty) false
      else areSameHostChannel(channelTokenOpt.get, channelOpt.get)
    }
  }


  def areSameHostChannel(channelToken: ChannelToken, channel: Channel): Boolean = {
    if (channelToken.channelId == channel.channelId) true
    else false
  }


  def isValidClient(clientChannelToken: String, requestClientId: String, authenticatedClientId: String): Future[Boolean] = {

    val channelTokenFut = channelTokens.getByClientChannelToken(clientChannelToken)
    val clientLoginRequestFut = clientLoginRequests.getByRequestClientId(requestClientId)
    val authenticatedClientFut = authenticatedClients.getByAuthenticatedClientId(authenticatedClientId)
    for {
      channelTokenOpt <- channelTokenFut
      clientLoginRequestOpt <- clientLoginRequestFut
      authenticatedClientOpt <- authenticatedClientFut
    } yield {
      if (clientLoginRequestOpt.isEmpty || authenticatedClientOpt.isEmpty || channelTokenOpt.isEmpty) false
      else areSameClientChannel(channelTokenOpt.get, clientLoginRequestOpt.get, authenticatedClientOpt.get)
    }
  }


  def areSameClientChannel(channelToken: ChannelToken, clientLoginRequest: ClientLoginRequest, authenticatedClient: AuthenticatedClient): Boolean = {
    if (
      (channelToken.channelId == clientLoginRequest.channelId)
      && (channelToken.channelId == authenticatedClient.channelId)
      && (clientLoginRequest.requestClientIdHash == authenticatedClient.requestClientIdHash)
      ) true
    else false
  }



}
