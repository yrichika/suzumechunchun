package database.tables

import com.mohiva.play.silhouette.api.crypto.Hash
import database.factories.AuthenticatedClientFactory
import database.models.AuthenticatedClient
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import testhelpers.utils.TestRandom
import testhelpers.{TestCase, TestDatabaseConfiguration}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

class AuthenticatedClientsSpec extends TestCase
with ScalaFutures
with TestDatabaseConfiguration
{

  def AuthenticatedClientsTable(implicit app: Application): AuthenticatedClients = injectDependency[AuthenticatedClients]

  val fakeChannelId: String = TestRandom.string(5)
  val fakeRequestClientId: String = TestRandom.string(5)
  val authenticatedClientId: String = TestRandom.string(5)
  val fakeModels = new AuthenticatedClientFactory {
    val fakeRequestClientIdHash = Hash.sha2(fakeRequestClientId)
    val fakeAuthenticatedClientIdHash = Hash.sha2(authenticatedClientId)
    override def authenticatedClientIdHash: String = fakeAuthenticatedClientIdHash
    override def channelId: String = fakeChannelId
    override def requestClientIdHash: String = fakeRequestClientIdHash
  }.create(1)

  override def beforeEach() = {
    Await.result(AuthenticatedClientsTable.seed(fakeModels), 2.seconds)
  }


  override def afterEach() = {
    Await.result(AuthenticatedClientsTable.truncate(), 2.seconds)
  }


  "create method" should {
    "create a record" in {
      val fakeChannelIdLocal = TestRandom.string(5)
      val fakeRequestClientIdLocal = TestRandom.string(5)
      val fakeRequestClientIdHashLocal = Hash.sha2(fakeRequestClientIdLocal)
      whenReady(AuthenticatedClientsTable.create(fakeChannelIdLocal, fakeRequestClientIdHashLocal)) { _ =>
        whenReady(AuthenticatedClientsTable.getByRequestClientId(fakeRequestClientIdLocal)) {
          result => result.get.channelId mustBe fakeChannelIdLocal
        }
      }
    }
  }

  "getByAuthenticatedClientId" should {
    "get item by authenticated client id" in {
      whenReady(AuthenticatedClientsTable.getByAuthenticatedClientId(authenticatedClientId)) {
        result =>
          val fakeModel = fakeModels.head
          result.get.requestClientIdHash mustBe fakeModel.requestClientIdHash
          result.get.authenticatedClientIdHash mustBe fakeModel.authenticatedClientIdHash
          result.get.authenticatedClientIdEnc mustBe fakeModel.authenticatedClientIdEnc
          result.get.channelId mustBe fakeModel.channelId
      }
    }
  }

  "getByRequestClientId" should {
    "get item by RequestClientId" in {
      whenReady(AuthenticatedClientsTable.getByRequestClientId(fakeRequestClientId)) {
        result => {
          val fakeModel = fakeModels.head
          result.get.requestClientIdHash mustBe fakeModel.requestClientIdHash
          result.get.authenticatedClientIdHash mustBe fakeModel.authenticatedClientIdHash
          result.get.authenticatedClientIdEnc mustBe fakeModel.authenticatedClientIdEnc
          result.get.channelId mustBe fakeModel.channelId
        }
      }
    }
  }

  "deleteByChannelIds" should {
    "delete records by given channel ids" in {
      val differentModels = new AuthenticatedClientFactory {}.create(1)
      Await.result(AuthenticatedClientsTable.seed(differentModels), 2.seconds)
      val allRecords = Await.result(AuthenticatedClientsTable.all(), 2.seconds)
      val channelIds = allRecords.map(_.channelId)
      whenReady(AuthenticatedClientsTable.deleteByChannelIds(channelIds).flatMap(_ => AuthenticatedClientsTable.all())) {
        result => result mustBe empty
      }

    }
  }

}
