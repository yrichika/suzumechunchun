package database.tables

import com.mohiva.play.silhouette.api.crypto.Hash
import database.models.Channel
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import database.factories.ChannelFactory
import testhelpers.utils.TestRandom
import testhelpers.{TestCase, TestDatabaseConfiguration}

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

class ChannelsSpec extends TestCase
with ScalaFutures
with TestDatabaseConfiguration
{
  def ChannelsTable(implicit app: Application): Channels = injectDependency[Channels]

  val fakeHostId: String = TestRandom.string(5)
  val fakeChannels: Seq[Channel] = new ChannelFactory {
    override def hostIdHash: String = Hash.sha2(fakeHostId)
  }.create(1)

  override def beforeEach() = {
    Await.result(ChannelsTable.seed(fakeChannels), 2.seconds)
  }

  override def afterEach() = {
    Await.result(ChannelsTable.truncate(), 2.seconds)
  }

  "create method" should {
    "create record with hashed host id" in {
      val fakeHostId2 = Random.alphanumeric.take(5).mkString
      val fakeHostIdHashed2 = Hash.sha2(fakeHostId2)

      whenReady(ChannelsTable.create(fakeHostId2)) { _ =>
        whenReady(ChannelsTable.getByHostId(fakeHostId2)) {
          result => result.get.hostIdHash mustBe fakeHostIdHashed2
        }
      }
    }
  }

  "getByHostId" should {
    "get item by host id" in {
      whenReady(ChannelsTable.getByHostId(fakeHostId)) {
        result => {
          val fakeChannel = fakeChannels.head
          result.get.hostIdHash mustBe fakeChannel.hostIdHash
          // might be pointless to assert id?
          result.get.channelId mustBe a [String]
        }
      }
    }
  }


  "getItemsOlderThan" should {
    val hour = Random.nextInt(6)
    val hoursAgo = LocalDateTime.now().minusHours(hour)

    "get items older than given relative hour" in {
      val oldData = new ChannelFactory {
        override def createdAt: Timestamp = Timestamp.valueOf(hoursAgo)
      }.create(1)
      Await.result(ChannelsTable.seed(oldData), 2.seconds)
      whenReady(ChannelsTable.getItemsOlderThan(hour)) {
        results => {
          val hostIdHashes = results.map(_.hostIdHash)
          hostIdHashes must contain(oldData.head.hostIdHash)
        }
      }

    }
    "not get items younger than given relative hour" in {

      val notOldData = new ChannelFactory {
        override def createdAt: Timestamp = Timestamp.valueOf(hoursAgo)
      }.create(1)
      // one hour before the given target hour. This record should not be targeted. Should not find anything.
      val hourBefore = hour + 1
      Await.result(ChannelsTable.seed(notOldData), 2.seconds)
      whenReady(ChannelsTable.getItemsOlderThan(hourBefore)) {
        result => result mustBe empty
      }

    }
  }

  "deleteByIds" should {
    "delete records of given ids" in {
      val differentModels = new ChannelFactory {}.create(1)
      Await.result(ChannelsTable.seed(differentModels), 2.seconds)
      val allRecords = Await.result(ChannelsTable.all(), 2.seconds)
      val channelIds = allRecords.map(_.channelId)
      whenReady(ChannelsTable.deleteByIds(channelIds).flatMap(_ => ChannelsTable.all())) {
        result => result mustBe empty
      }
    }
  }
}
