package schedulers


import javax.inject.Inject
import javax.inject.Named
import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import database.tables._
import helpers.Log

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

// https://www.playframework.com/documentation/2.8.x/ScheduledTasks
class ChannelCleaner @Inject()(
  actorSystem: ActorSystem,
  channels: Channels,
  channelTokens: ChannelTokens,
  clientLoginRequests: ClientLoginRequests,
  authenticatedClients: AuthenticatedClients)
  (implicit executor: ScheduleDispatcher)
{
  val conf = ConfigFactory.load()
  lazy val deleteRecordsOlderThanThisHour = conf.getInt("chatExpiration.byHour")
  // InitialDelay should NOT be set to 0.milliseconds for testing.
  // If initialDelay is set to 0.milliseconds, ChannelCleanerSpec will fail
  // because clean() method will be executed before any test runs.
  val delay = conf.getInt("channelCleaner.initialDelayMinutes")
  val interval = conf.getInt("channelCleaner.intervalMinutes")
  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = delay.minutes, interval = interval.minutes) { () =>
    clean()
  }

  def clean() = {
    channels.getItemsOlderThan(deleteRecordsOlderThanThisHour).flatMap(data => {
      val channelIds = data.map(_.channelId)

      val deleteChannelsFut = channels.deleteByIds(channelIds)
      val deleteChannelTokensFut = channelTokens.deleteByChannelIds(channelIds)
      val deleteAuthenticatedClientsFut = authenticatedClients.deleteByChannelIds(channelIds)
      val deleteClientLoginRequestsFut = clientLoginRequests.deleteByChannelIds(channelIds)
      for {
        deleteChannels <- deleteChannelsFut
        deleteChannelTokens <- deleteChannelTokensFut
        deleteAuthenticatedClients <- deleteAuthenticatedClientsFut
        deleteClientLoginRequests <- deleteClientLoginRequestsFut
      } yield {
        Log.console.info("Deleted old channel data.")
        (deleteChannels, deleteChannelTokens, deleteAuthenticatedClients, deleteClientLoginRequests)
      }
    })
  }
}
