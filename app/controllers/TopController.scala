package controllers

import com.typesafe.config.ConfigFactory
import database.tables.{ChannelTokens, Channels}

import javax.inject._
import play.api.mvc._
import forms.CreateChannel
import helpers.Log
import helpers.controllers.IpRateLimiter
import helpers.crypto.Random

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport.ResultWithMessagesApi
import play.api.i18n.Lang

import java.util.UUID

@Singleton
class TopController @Inject()
(val messagesActionBuilder: MessagesActionBuilder,
 val controllerComponents: ControllerComponents,
 val channels: Channels,
 val channelTokens: ChannelTokens,
 val cache: AsyncCacheApi
) extends BaseController
{
  private val ipRateLimitFilter = IpRateLimiter.throttle(10, 1f / 20)

  def index = (messagesActionBuilder andThen ipRateLimitFilter) { implicit request: MessagesRequest[AnyContent] =>
    
    request.session.get("hostId") match {
      case Some(hostId) => {
        val secretKey = request.session.get("secretKeyHost").getOrElse("")
        Redirect(routes.HostController.chat(hostId))
          .withSession("hostId" -> hostId, "secretKeyHost" -> secretKey)
      }
      case None => Ok(views.html.pages.top.main(CreateChannel.form))
    }
  }

  def validate = (messagesActionBuilder andThen ipRateLimitFilter) { implicit request: MessagesRequest[AnyContent] =>
    CreateChannel.form.bindFromRequest().fold(
      formWithErrors => BadRequest(views.html.pages.top.main(formWithErrors)),
      successfulData => {
        val channelName = successfulData.channelName
        Redirect(routes.TopController.create()).withSession("channelName" -> channelName)
      }
    )
  }

  def create = Action.async { implicit request =>
    request.session.get("channelName") match {
      case Some(channelName) => {
        val hostId = UUID.randomUUID().toString()
        val createdChannel = channels.create(hostId)
        val createdToken = createdChannel.flatMap(channelId => {
          channelTokens.create(channelId, channelName).map(createdChannel => {
            (channelId, createdChannel)
          })
        })
        createdToken.map(token => {
          val channelId = token._1
          val createdChannel = token._2

          Redirect(routes.HostController.chat(createdChannel.hostChannelToken))
            .withSession("hostId" -> hostId, "secretKeyHost" -> createdChannel.secretKey)
        })
      }
      case None => Future.successful(Unauthorized("Unauthorized"))
    }
  }
}
