package schedulers

import akka.actor.ActorSystem
import database.factories.{AuthenticatedClientFactory, ChannelFactory, ChannelTokenFactory, ClientLoginRequestFactory}
import database.models.{AuthenticatedClient, Channel, ChannelToken, ClientLoginRequest}
import database.tables.{AuthenticatedClients, ChannelTokens, Channels, ClientLoginRequests}
import helpers.Log
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.whenReady
import testhelpers.{TestCase, TestDatabaseConfiguration}
import play.api.{Application, Logger}
import testhelpers.utils.TestRandom

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class ChannelCleanerSpec extends TestCase
  with ScalaFutures
  with TestDatabaseConfiguration {

  implicit val actorSystem = ActorSystem("TestingScheduler")
  implicit val dispatcher = new ScheduleDispatcher(actorSystem)
  def ChannelsTable(implicit app: Application): Channels = injectDependency[Channels]
  def ChannelTokensTable(implicit app: Application): ChannelTokens = injectDependency[ChannelTokens]
  def ClientLoginRequestsTable(implicit app: Application): ClientLoginRequests = injectDependency[ClientLoginRequests]
  def AuthenticatedClientsTable(implicit app: Application): AuthenticatedClients = injectDependency[AuthenticatedClients]

  val scheduler = new ChannelCleaner(
    actorSystem,
    ChannelsTable,
    ChannelTokensTable,
    ClientLoginRequestsTable,
    AuthenticatedClientsTable)

  val hour = scheduler.deleteRecordsOlderThanThisHour
  val hoursAgo = LocalDateTime.now().minusHours(hour)
  val lessThanHoursAgo = LocalDateTime.now().minusHours(scheduler.deleteRecordsOlderThanThisHour - 1)

  override def afterEach(): Unit = {
    val truncating = for {
      channels <- ChannelsTable.truncate()
      tokens <- ChannelTokensTable.truncate()
      requests <- ClientLoginRequestsTable.truncate()
      clients <- AuthenticatedClientsTable.truncate()
    } yield (channels, tokens, requests, clients)
    Await.result(truncating, 2.seconds)
  }


  "clean" should {
    "delete channels older than configured hour" in {
      val channelId = TestRandom.string(5)
      val data = createSeedingData(hoursAgo, channelId)
      val seeding = for {
        channels <- ChannelsTable.seed(data.channels)
        tokens <- ChannelTokensTable.seed(data.tokens)
        requests <- ClientLoginRequestsTable.seed(data.requests)
        clients <- AuthenticatedClientsTable.seed(data.clients)
      } yield (channels, tokens, requests, clients)

      Await.result(seeding, 2.seconds)

      whenReady(scheduler.clean()) { result =>
        result mustBe (1, 1, 1, 1)
      }
      whenReady(ChannelsTable.all()) (_ mustBe empty)
      whenReady(ChannelTokensTable.all()) (_ mustBe empty)
      whenReady(ClientLoginRequestsTable.all()) (_ mustBe empty)
      whenReady(AuthenticatedClientsTable.all()) (_ mustBe empty)

    }

    "not delete data that's younger than configured hour" in {
      val channelId = TestRandom.string(5)
      val data = createSeedingData(lessThanHoursAgo, channelId)
      val seeding = for {
        channels <- ChannelsTable.seed(data.channels)
        tokens <- ChannelTokensTable.seed(data.tokens)
        requests <- ClientLoginRequestsTable.seed(data.requests)
        clients <- AuthenticatedClientsTable.seed(data.clients)
      } yield (channels, tokens, requests, clients)

      Await.result(seeding, 2.seconds)

      whenReady(scheduler.clean()) { result =>
        result mustBe (0, 0, 0, 0)
      }
      whenReady(ChannelsTable.all()) (_ must not be empty)
      whenReady(ChannelTokensTable.all()) (_ must not be empty)
      whenReady(ClientLoginRequestsTable.all()) (_ must not be empty)
      whenReady(AuthenticatedClientsTable.all()) (_ must not be empty)
    }
  }

  case class TableSeeder(channels: Seq[Channel], tokens: Seq[ChannelToken], requests: Seq[ClientLoginRequest], clients: Seq[AuthenticatedClient])

  def createSeedingData(hour: LocalDateTime, defaultChannelId: String, howMany: Int = 1) = {

    val oldData = new ChannelFactory {
      override def channelId: String = defaultChannelId
      override def createdAt: Timestamp = Timestamp.valueOf(hour)
    }.create(howMany)

    val channelTokens = new ChannelTokenFactory {
      override def channelId: String = defaultChannelId
    }.create(howMany)
    val clientLoginRequests = new ClientLoginRequestFactory {
      override def channelId: String = defaultChannelId
    }.create(howMany)
    val authenticatedClients = new AuthenticatedClientFactory {
      override def channelId: String = defaultChannelId
    }.create(howMany)

    TableSeeder(oldData, channelTokens, clientLoginRequests, authenticatedClients)
  }

}
