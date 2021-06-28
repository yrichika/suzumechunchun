package controllers

import akka.stream.UniqueKillSwitch
import akka.stream.scaladsl.Flow
import database.tables.{ChannelTokens, Channels}
import forms.CreateChannel
import helpers.controllers.IpRateLimiter
import helpers.crypto.Crypter

import javax.inject.{Inject, Singleton}
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, MessagesActionBuilder, MessagesRequest}

import scala.concurrent.ExecutionContext


@Singleton
class LogoutController @Inject()
(val messagesActionBuilder: MessagesActionBuilder,
 val controllerComponents: ControllerComponents,
 val channelTokens: ChannelTokens,
 cache: AsyncCacheApi
) (implicit val executionContext: ExecutionContext) extends BaseController with I18nSupport
{
  private val rateLimiter = IpRateLimiter.throttle(5, 1f / 20)

  def index(hostChannelToken: String) = (messagesActionBuilder andThen rateLimiter) { implicit request: MessagesRequest[AnyContent] =>
    val chatStream = cache.get[Flow[String, String, UniqueKillSwitch]](hostChannelToken)
    chatStream.map {
      // FIXME: Not sure if killSwtich is doing anything.
      //  It does not stop client side WebSocket connection.
      //  But it will stop WebSocket at reload or closing browser
      //  (because `cache.remove()` deletes WebSocket flow from the cache anyway).
      //  It's not a problem for now, but I want to know how to stop
      //  WebSocket flow on client side from server.
      case Some(chat) => chat.mapMaterializedValue(killSwitch => killSwitch.shutdown())
      case None => 0
    }
    cache.remove(hostChannelToken)

    channelTokens.getByHostChannelToken(hostChannelToken).map(tokensOpt => {
      tokensOpt match {
        case Some(tokens) => {
          val clientChannelToken = Crypter.decrypt(tokens.clientChannelTokenEnc, tokens.channelId)
          cache.remove(clientChannelToken)
        }
        case None => 0
      }
    })

    channelTokens.trashSecretKeyByHostChannelToken(hostChannelToken)

    Redirect(routes.TopController.index()).withNewSession
  }
}
