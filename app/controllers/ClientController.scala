package controllers

import akka.actor.ActorSystem
import akka.stream.{Materializer, UniqueKillSwitch}
import database.tables._
import helpers.streams.ClientEventSource
import javax.inject._
import play.api.mvc._
import forms.LoginRequest
import play.api.http.ContentTypes
import play.api.libs.json.Json
import akka.stream.scaladsl.Flow
import database.models.{AuthenticatedClient, ChannelToken}
import helpers.controllers.{EnvironmentAware, IpRateLimiter}
import helpers.crypto.Crypter
import modules.InputSanitizer
import play.api.Environment
import play.api.cache.AsyncCacheApi
import play.api.data.Form
import scala.concurrent.{ExecutionContext, Future}



@Singleton
class ClientController @Inject()
(
  val controllerComponents: ControllerComponents,
  val channels: Channels,
  val channelTokens: ChannelTokens,
  val clientLoginRequests: ClientLoginRequests,
  val authenticatedClients: AuthenticatedClients,
  val messagesActionBuilder: MessagesActionBuilder,
  val inputSanitizer: InputSanitizer,
  val cache: AsyncCacheApi
)
(
  implicit val actorSystem: ActorSystem,
  val materializer: Materializer,
  val executionContext: ExecutionContext,
  val environment: Environment
)
  extends BaseController
  with Chat
  with EnvironmentAware
{

  val modelNotFoundErrorMessage = "Data not found"
  val loginRequestResponseJsonKey = "message"
  val closedStatusString = "__closed__"
  val loginRequestAcceptedJson = Json.obj(loginRequestResponseJsonKey -> "ok")
  val LoginRequestClosedStatusJson = Json.obj(loginRequestResponseJsonKey -> closedStatusString)

  val clientEventSource = new ClientEventSource(clientLoginRequests, channelTokens)


  private val requestRateLimit = IpRateLimiter.throttle(6, 1f / 15)
  private val accessRateLimit = IpRateLimiter.throttle(10, 1f / 20)

  override protected def chatFlow(clientChannelToken: String): Future[Flow[String, String, UniqueKillSwitch]] = {
    cache.get[Flow[String, String, UniqueKillSwitch]](clientChannelToken).map {
      case Some(flow) => flow
      // FIXME: make this not to throw exception. Any other way to fail safely.
      case None => throw new Exception("This chat does not exist")
    }
  }

  def request(loginChannelToken: String) = (messagesActionBuilder andThen requestRateLimit).async {implicit request: MessagesRequest[AnyContent] =>

    val tokenFut = channelTokens.getByLoginChannelToken(loginChannelToken)
    tokenFut.map(tokenOpt => {
      tokenOpt match {
        case Some(channel) if (request.session.get("hostId").nonEmpty) => {
          BadRequest(views.html.pages.error.e400(BAD_REQUEST,
            """If you are a host and want to access this client page,
              |you should access with PRIVATE MODE or use different browsers.
              |Otherwise, it's now allowed for a host to access to the client page.
              | もし同じブラウザから同時にhostとclientを使いたい場合は、
              |どちらかをプライベート(シークレット)モードにして下さい。もしくはhostとclient
              |で別々のブラウザを使って下さい。""".stripMargin))
        }
        case Some(channelToken) => getRequestPage(channelToken, loginChannelToken)
        case None => NotFound("Token Invalid")
      }
    })
  }

  def getRequestPage(channelToken: ChannelToken, loginChannelToken: String)(implicit request: MessagesRequest[AnyContent]) = {
    channelToken.secretKeyEnc match {
      case Some(key) => {
        // valid only when secret key exists
        val channelName = Crypter.decrypt(channelToken.channelNameEnc, channelToken.channelId.toString)
        Ok(views.html.pages.client.request(loginChannelToken, channelName, isProd, LoginRequest.form))
      }
      // No secret key: channel is closed
      case None => NotFound("Not Found")
    }
  }


  def validate(loginChannelToken: String) = (messagesActionBuilder andThen requestRateLimit).async {implicit request: MessagesRequest[AnyContent] =>
    val channelFut = channelTokens.getByLoginChannelToken(loginChannelToken)
    channelFut.flatMap(channelOpt => {
      processRequest(loginChannelToken, channelOpt)
    })
  }


  def processRequest(loginChannelToken: String, channelOpt: Option[ChannelToken])(implicit request: MessagesRequest[AnyContent]) = {
    channelOpt match {
      case Some(channel) => {
        LoginRequest.form.bindFromRequest().fold(
          formWithErrors => failedRequest(loginChannelToken, channel, formWithErrors),
          successfulData => successRequest(loginChannelToken, channel, successfulData)
        )
      }
      case None => Future.successful(Unauthorized("Unauthorized"))
    }
  }


  def failedRequest(loginChannelToken: String, channel: ChannelToken, formWithErrors: Form[LoginRequest])(implicit request: MessagesRequest[AnyContent]) = {
    Future.successful(BadRequest(formWithErrors.errorsAsJson))
  }


  def successRequest(loginChannelToken: String, channel: ChannelToken, successfulData: LoginRequest) = {
    channel.secretKeyEnc match {
      case Some(key) =>
        val requestFut = clientLoginRequests.create(successfulData, channel.channelId)
        requestFut.map(request => {
          Ok(loginRequestAcceptedJson)
            .withSession(requestClientIdSessionKey -> request.requestClientId)
        })
      case None => Future.successful(Ok(LoginRequestClosedStatusJson))
    }

  }




  def chat(clientChannelToken: String) = (messagesActionBuilder andThen accessRateLimit).async { implicit request: MessagesRequest[AnyContent] =>
    val requestClientId = request.session.get(requestClientIdSessionKey).getOrElse("")
    // REFACTOR:
    authenticatedClients.getByRequestClientId(requestClientId).flatMap {
      case Some(authenticatedClient) => {
        val codenameFut = getCodenameOf(requestClientId, authenticatedClient)
        val authenticatedClientId = Crypter.decrypt(authenticatedClient.authenticatedClientIdEnc, authenticatedClient.channelId)
        val channelFut = channelTokens.getByClientChannelToken(clientChannelToken)
        val secretKeyFut = getSecretKey(channelFut)
        val channelNameFut = getChannelName(channelFut, authenticatedClient)
        for {
          secretKey <- secretKeyFut
          channelName <- channelNameFut
          codename <- codenameFut
        } yield {
          val webSocketUrl = routes.ClientController.ws(clientChannelToken).webSocketURL(isProd)
          Ok(views.html.pages.client.main(channelName, clientChannelToken, webSocketUrl, codename, secretKey))
            .withSession(
              authenticatedClientIdSessionKey -> authenticatedClientId,
              requestClientIdSessionKey -> requestClientId
            ).withHeaders(
            "Content-Security-Policy" -> s"connect-src 'self' ${webSocketUrl}"
            )
        }
      }
      // client id session value is wrong in header
      case None => Future.successful(Unauthorized("Unauthorized!"))
    }
  }

  def getCodenameOf(requestClientId: String, authenticatedClient: AuthenticatedClient) = {
    clientLoginRequests.getByRequestClientId(requestClientId).map(
      modelOpt => modelOpt match {
        case Some(data) => Crypter.decrypt(data.codenameEnc.get, authenticatedClient.channelId)
        case None => throw new Exception(modelNotFoundErrorMessage)
      }
    )
  }

  def getChannelName(channelFut: Future[Option[ChannelToken]], authenticatedClient: AuthenticatedClient) = {
    channelFut.map(channelOpt =>
      channelOpt match {
        case Some(data) => Crypter.decrypt(data.channelNameEnc, authenticatedClient.channelId)
        case None => throw new Exception(modelNotFoundErrorMessage)
      }
    )
  }

  def getSecretKey(channelFut: Future[Option[ChannelToken]]) = {
    channelFut.map {
      case Some(channel) => {
        channel.secretKeyEnc match {
          case Some(secretKey) => {
            Crypter.decrypt(secretKey, channel.channelId.toString)
          }
          case None => throw new Exception("This chat channel is already closed. このチャットチャンネルはすでに終了しました。")
        }
      }
      case None => throw new Exception(modelNotFoundErrorMessage)
    }
  }


  def sse(loginChannelToken: String) = Action { implicit request: Request[AnyContent] =>
    val requestClientId = request.session.get(requestClientIdSessionKey).getOrElse("")
    Ok.chunked(clientEventSource.streamCurrentAuthenticatedStatus(requestClientId)).as(ContentTypes.EVENT_STREAM)
  }

}
