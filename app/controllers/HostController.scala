package controllers



import akka.actor.ActorSystem
import akka.stream.{Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.Flow
import com.typesafe.config.ConfigFactory
import database.tables._
import javax.inject._
import play.api.mvc._
import database.models.Channel
import database.models.ChannelToken
import forms.ClientStatus
import helpers.controllers.{EnvironmentAware, IpRateLimiter}
import scala.concurrent.{ExecutionContext, Future}
import play.api.cache.AsyncCacheApi
import helpers.crypto.Crypter
import helpers.streams.{ChatStream, HostEventSource}
import modules.InputSanitizer
import play.api.Environment
import play.api.http.ContentTypes
import play.api.libs.json.Json
import scala.concurrent.duration._



@Singleton
class HostController @Inject()
(val controllerComponents: ControllerComponents,
 val channels: Channels,
 val channelTokens: ChannelTokens,
 val clientLoginRequests: ClientLoginRequests,
 val authenticatedClients: AuthenticatedClients,
 val messagesActionBuilder: MessagesActionBuilder,
 val cache: AsyncCacheApi,
 val inputSanitizer: InputSanitizer)
(implicit val actorSystem: ActorSystem,
 val materializer: Materializer,
 val executionContext: ExecutionContext,
 val environment: Environment
)
  extends BaseController
  with Chat
  with EnvironmentAware
{

  case class ChannelInfo(channelName: String, loginChannelToken: String)

  lazy val cacheExpiration = ConfigFactory.load().getInt("chatExpiration.byHour")
  private val ipRateLimitFilter = IpRateLimiter.throttle(10, 1f / 20)
  private val manageRequestLimit = IpRateLimiter.throttle(30, 1f / 20)
  val hostEventSource = new HostEventSource(channels, clientLoginRequests)

  protected def chatFlow(hostChannelToken: String): Future[Flow[String, String, UniqueKillSwitch]] = {
    // cache api stores the reference of the object(chat stream flow)
    cache.getOrElseUpdate[Flow[String, String, UniqueKillSwitch]](hostChannelToken, cacheExpiration.hours) {
      val channelTokenFut = channelTokens.getByHostChannelToken(hostChannelToken)
      channelTokenFut.map {
        case Some(channelToken) => {
          val clientChannelToken = Crypter.decrypt(channelToken.clientChannelTokenEnc, channelToken.channelId)
          val chatStream = ChatStream.create(inputSanitizer)
          // This will store the reference of the chat stream,
          // but stored with different keys. Two cache keys points to the same chat stream(object) in memory.
          // This way, a host and clients can access the same chat stream by their different keys.
          cache.set(clientChannelToken, chatStream, cacheExpiration.hours) // cached by clientChannel
          chatStream // cached by hostChannel
        }
        case None => {
          throw new Exception("channel token does not exist.")
        }
      }
    }
  }


  def chat(hostChannelToken: String) = (messagesActionBuilder andThen ipRateLimitFilter).async { implicit request: MessagesRequest[AnyContent] =>
    request.session.get(hostIdSessionKey) match {
      case Some(hostId) => {
        // FIXME: it should abort immediately if no secret key.
        val secretKey = request.session.get("secretKeyHost").getOrElse("")
        val webSocketUrl = routes.HostController.ws(hostChannelToken).webSocketURL(isProd)
        getChannel(hostChannelToken, hostId).map {
          case Some(channelInfo) =>
            val clientRequestPageUrl = routes.ClientController.request(channelInfo.loginChannelToken).absoluteURL(isProd)
            Ok(views.html.pages.host.main(
              channelInfo.channelName,
              hostChannelToken,
              webSocketUrl,
              clientRequestPageUrl,
              secretKey
            )).withHeaders("Content-Security-Policy" -> s"connect-src 'self' ${webSocketUrl}")
          case None => Unauthorized("unauthorized")
        }
      }
      case None => {
        Future.successful(Unauthorized("unauthorized"))
      }
    }
  }


  def getChannel(hostChannelToken: String, hostId: String): Future[Option[ChannelInfo]] = {
    val channelFut = channels.getByHostId(hostId)
    channelFut.flatMap {
      case Some(channel) => getToken(hostChannelToken, channel)
      case None => Future.successful(None)
    }
  }


  def getToken(hostChannelToken: String, channel: Channel): Future[Option[ChannelInfo]] = {
    channelTokens.getByHostChannelToken(hostChannelToken).map {
      case Some(token) => {
        // !important Checking if host id and channel id are related to the same channel.
        if (channel.channelId == token.channelId) {
          val loginChannelToken = Crypter.decrypt(token.loginChannelTokenEnc, token.channelId)
          val channelName = Crypter.decrypt(token.channelNameEnc, token.channelId)
          Some(ChannelInfo(channelName, loginChannelToken))
        } else {
          None
        }
      }
      case None => None
    }
  }


  def manageRequest(hostChannelToken: String) = (messagesActionBuilder andThen manageRequestLimit).async {implicit request: MessagesRequest[AnyContent] =>
    // FIXME: validate host id. Only the channel's valid host can update its client statuses.
    ClientStatus.form.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      successfulData => {
        val channelFut = channelTokens.getByHostChannelToken(hostChannelToken)
        val requestClientFut = clientLoginRequests.updateStatus(successfulData)
        createAuthenticatedClient(channelFut, requestClientFut).map {
          case Some(value) => Ok(Json.toJson(value))
          case None => BadRequest(Json.toJson("channel does not exits"))
        }
      }
    )
  }

  def closeRequest(hostChannelToken: String) = (messagesActionBuilder andThen ipRateLimitFilter).async { implicit request: MessagesRequest[AnyContent] =>
    // FIXME: validate host id. Only the channel's valid host can delete secret key.
    channelTokens.trashSecretKeyByHostChannelToken(hostChannelToken).map(_ => {
      Ok(Json.toJson("message" -> "ok"))
    })
  }


  def createAuthenticatedClient(channelFut: Future[Option[ChannelToken]], requestClientFut: Future[String]): Future[Option[String]] = {
    channelFut.flatMap {
      case Some(channel) => {
        requestClientFut.map(hashedClientId => {
          authenticatedClients.create(channel.channelId, hashedClientId)
          Some("ok")
        })
      }
      case None => Future.successful(None)
    }
  }


  def sse(hostChannelToken: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.session.get(hostIdSessionKey) match {
      // FIXME: validate if hostChannel and hostId belongs to the same channel
      case Some(hostId) => {
        Ok.chunked(hostEventSource.streamRequestingUsers(hostId)).as(ContentTypes.EVENT_STREAM)
      }
      case None => {
        Unauthorized("unauthorized")
      }
    }
  }


}
