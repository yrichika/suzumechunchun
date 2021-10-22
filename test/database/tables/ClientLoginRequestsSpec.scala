package database.tables

import com.mohiva.play.silhouette.api.crypto.Hash
import database.factories.ClientLoginRequestFactory
import database.models.ClientLoginRequest
import org.scalatest.concurrent.ScalaFutures
import testhelpers.{TestCase, TestDatabaseConfiguration}
import play.api.Application

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import forms.{ClientStatus, LoginRequest}
import testhelpers.utils.TestRandom

import java.sql.Timestamp
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


class ClientLoginRequestsSpec extends TestCase
with ScalaFutures
with TestDatabaseConfiguration
{
  def ClientLoginRequestsTable(implicit app: Application): ClientLoginRequests = injectDependency[ClientLoginRequests]

  val fakeChannelId = TestRandom.string(5)
  val fakeRequestClientId = TestRandom.string(5)
  // REFACTOR: Is there any way to wrap fakeModel* creations to one method?
  val fakeModel1 = new ClientLoginRequestFactory {
    override def channelId: String = fakeChannelId
    override def requestClientIdHash: String = Hash.sha2(fakeRequestClientId)
    override def isAuthenticated: Option[Boolean] = None // Just for clarity
  }.create()
  val fakeModel2 = new ClientLoginRequestFactory {
    override def channelId: String = fakeChannelId
    // For testing `getByChannelId`. It sorts items by createdAt in descendant
    override def createdAt: Timestamp = new Timestamp(System.currentTimeMillis() + 1000)
  }.create()
  val fakeModel3 = new ClientLoginRequestFactory {
    override def channelId: String = fakeChannelId
    // For testing `getByChannelId`. It sorts items by createdAt in descendant
    override def createdAt: Timestamp = new Timestamp(System.currentTimeMillis() + 5000)
  }.create()
  val fakeModels = fakeModel1 ++ fakeModel2 ++ fakeModel3

  val firstModel = fakeModels.head
  val loginRequest = LoginRequest(
    TestRandom.string(5),
    TestRandom.string(5)
  )

  override def beforeEach() = {
    Await.result(ClientLoginRequestsTable.seed(fakeModels), 2.seconds)
  }

  override def afterEach() = {
    Await.result(ClientLoginRequestsTable.truncate(), 2.seconds)
  }

  "getByChannelId" should {
    "get collection by channel id" in {
      whenReady(ClientLoginRequestsTable.getByChannelId(fakeChannelId)) {
        results =>
          val sortedExpecteds = fakeModels.sortBy(_.createdAt).reverse
          results.foreach(a => println(a.createdAt))
          for ((r, e) <- (results zip sortedExpecteds)) {
            r.clientLoginRequestId mustBe a [String]
            r.requestClientIdHash mustBe e.requestClientIdHash
            r.channelId mustBe e.channelId
            r.isAuthenticated mustBe e.isAuthenticated
          }
      }
    }
  }

  "getByRequestClientId" should {
    "get item by request client id" in {
      whenReady(ClientLoginRequestsTable.getByRequestClientId(fakeRequestClientId)) {
        result =>
          val fakeModel = fakeModels.head
          result.get.clientLoginRequestId mustBe a [String]
          result.get.requestClientIdHash mustBe fakeModel.requestClientIdHash
          result.get.channelId mustBe fakeModel.channelId
          result.get.isAuthenticated mustBe fakeModel.isAuthenticated
      }
    }
  }

  "create" should {
    val channelIdLocal = TestRandom.string(5)
    "create a record based on give values" in {
      whenReady(ClientLoginRequestsTable.create(loginRequest, channelIdLocal)) { _ =>
        whenReady(ClientLoginRequestsTable.all()) {
          results => {
            val channelIds = results.map(_.channelId)
            channelIds must contain(channelIdLocal)
          }
        }
      }
    }

    "return created CreatedRequest case instance" in {
      whenReady(ClientLoginRequestsTable.create(loginRequest, channelIdLocal)) {
        result => {
          result.requestClientId mustBe a[String]
          result.codename mustBe loginRequest.codename
          result.passphrase mustBe loginRequest.passphrase
          result.isAuthenticated mustBe None
        }
      }
    }
  }

  "updateStatus" should {
    val status = Random.nextBoolean()
    val clientStatus = ClientStatus(fakeRequestClientId, status)
    "update isAuthenticated to true" in {
      whenReady(ClientLoginRequestsTable.updateStatus(clientStatus)) { _ =>
        whenReady(ClientLoginRequestsTable.getByRequestClientId(fakeRequestClientId)) {
          result => result.get.isAuthenticated mustBe Some(status)
        }
      }
    }

    "update updatedAt column" in {
      whenReady(ClientLoginRequestsTable.updateStatus(clientStatus)) { _ =>
        whenReady(ClientLoginRequestsTable.getByRequestClientId(fakeRequestClientId)) {
          result => result.get.updatedAt must not equal firstModel.updatedAt
        }
      }
    }

    "return hashed request client id" in {
      whenReady(ClientLoginRequestsTable.updateStatus(clientStatus)) {
        result => result mustBe Hash.sha2(fakeRequestClientId)
      }
    }

  }

  "deleteByChannelIds" should {
    "delete records by given channel ids" in {
      val differentModels = new ClientLoginRequestFactory {}.create(1)
      Await.result(ClientLoginRequestsTable.seed(differentModels), 2.seconds)
      val allRecords = Await.result(ClientLoginRequestsTable.all(), 2.seconds)
      val channelIds = allRecords.map(_.channelId)
      whenReady(ClientLoginRequestsTable.deleteByChannelIds(channelIds).flatMap(_ => ClientLoginRequestsTable.all())) {
        result => result mustBe empty
      }
    }
  }


}
