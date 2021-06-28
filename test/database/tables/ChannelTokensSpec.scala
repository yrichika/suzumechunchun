package database.tables

import com.mohiva.play.silhouette.api.crypto.Hash
import database.models.{ChannelToken, CreatedChannel}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import testhelpers.{TestCase, TestDatabaseConfiguration}

import scala.util.Random
import helpers.crypto.Crypter

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global
import database.factories.ChannelTokenFactory

import scala.concurrent.duration._

class ChannelTokensSpec extends TestCase
with ScalaFutures
with TestDatabaseConfiguration
{
  def ChannelTokensTable(implicit app: Application): ChannelTokens = injectDependency[ChannelTokens]
  val randomIdMax = 1000
  val channelName = "any name"
  val fakeChannelId: String = Random.alphanumeric.take(5).mkString
  val ad: String = fakeChannelId
  val fakeHostChannelToken: String = Random.alphanumeric.take(5).mkString
  val fakeClientChannelToken: String = Random.alphanumeric.take(5).mkString
  val fakeLoginChannelToken: String = Random.alphanumeric.take(5).mkString

  val fakeModels = new ChannelTokenFactory {
    override def channelId = fakeChannelId
    override def hostChannelTokenHash = Hash.sha2(fakeHostChannelToken)
    override def clientChannelTokenHash: String = Hash.sha2(fakeClientChannelToken)
    override def loginChannelTokenHash: String = Hash.sha2(fakeLoginChannelToken)
  }.create(1)


  override def beforeEach() = {
    Await.result(ChannelTokensTable.seed(fakeModels), 2.seconds)
  }

  override def afterEach() = {
    Await.result(ChannelTokensTable.truncate(), 2.seconds)
  }

  "create method" should {
    "create a new record" in {
      val idLocal = Random.alphanumeric.take(5).mkString
      Await.result(ChannelTokensTable.create(idLocal, channelName), 2.seconds)
      // just asserting the created record exists in db.
      // not much point in asserting the value, but just making sure it exists
      whenReady(ChannelTokensTable.getByChannelId(idLocal)) {
        result => result.get.channelId mustBe idLocal
      }

    }

    "return created values" in {
      val idLocal = Random.alphanumeric.take(5).mkString
      whenReady(ChannelTokensTable.create(idLocal, channelName)) {
        result => {
          result.hostChannelToken mustBe a [String]
          result.loginChannelToken mustBe a [String]
          result.secretKey mustBe a [String]
        }
      }
    }
  }

  "getByChannelId" should {
    "get record by channel id and values should be decryptable" in {
      whenReady(ChannelTokensTable.getByChannelId(fakeChannelId)) {
        result => haveAllElements(result)
      }
    }
  }

  "getByHostChannelToken" should {
    "get record by hostChannelToken and get value" in {
      whenReady(ChannelTokensTable.getByHostChannelToken(fakeHostChannelToken)) {
        result => haveAllElements(result)
      }
    }
  }


  "getByClientChannelToken" should {
    "get record by client channel token" in {
      whenReady(ChannelTokensTable.getByClientChannelToken(fakeClientChannelToken)) {
        result => haveAllElements(result)
      }
    }
  }

  "getByLoginChannelToken" should {
    "get record by request token" in {
      whenReady(ChannelTokensTable.getByLoginChannelToken(fakeLoginChannelToken)) {
        result => haveAllElements(result)
      }
    }
  }

  "trashSecretKeyByHostChannelToken" should {
    "update secretKey value to None" in {
      whenReady(ChannelTokensTable.trashSecretKeyByHostChannelToken(fakeHostChannelToken)) { _ =>
        whenReady(ChannelTokensTable.getByHostChannelToken(fakeHostChannelToken)) {
          result => result.get.secretKeyEnc mustBe None
        }
      }
    }
    "update updatedAt column" in {
      whenReady(ChannelTokensTable.trashSecretKeyByHostChannelToken(fakeHostChannelToken)) { _ =>
        whenReady(ChannelTokensTable.getByHostChannelToken(fakeHostChannelToken)) {
          result => result.get.updatedAt must not equal fakeModels.head.updatedAt
        }
      }
    }
  }

  "deleteByChannelIds" should {
    "delete records by given channel ids" in {
      val differentModels = new ChannelTokenFactory {}.create(1)
      Await.result(ChannelTokensTable.seed(differentModels), 2.seconds)
      val allRecords = Await.result(ChannelTokensTable.all(), 2.seconds)
      val channelIds = allRecords.map(_.channelId)
      whenReady(ChannelTokensTable.deleteByChannelIds(channelIds).flatMap(_ => ChannelTokensTable.all())) {
        result => result mustBe empty
      }
    }
  }



  def haveAllElements(result: Option[ChannelToken]) = {
    val fakeModel = fakeModels.head
    result.get.channelId mustBe fakeModel.channelId
    result.get.channelNameEnc mustBe fakeModel.channelNameEnc
    result.get.hostChannelTokenHash mustBe fakeModel.hostChannelTokenHash
    result.get.loginChannelTokenHash mustBe fakeModel.loginChannelTokenHash
    result.get.loginChannelTokenEnc mustBe fakeModel.loginChannelTokenEnc
    result.get.clientChannelTokenHash mustBe fakeModel.clientChannelTokenHash
    result.get.clientChannelTokenEnc mustBe fakeModel.clientChannelTokenEnc
    result.get.secretKeyEnc.get mustBe fakeModel.secretKeyEnc.get
  }
}
