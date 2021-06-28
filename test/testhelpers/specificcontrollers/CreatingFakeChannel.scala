package testhelpers.specificcontrollers

import com.mohiva.play.silhouette.api.crypto.Hash
import database.factories.{AuthenticatedClientFactory, ChannelFactory, ChannelTokenFactory, ClientLoginRequestFactory}
import database.tables.{AuthenticatedClients, ChannelTokens, Channels, ClientLoginRequests}
import helpers.crypto.Crypter
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.test._
import play.api.test.Helpers._
import testhelpers.utils.TestRandom
import testhelpers.{TestCase, TestDatabaseConfiguration}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


trait CreatingFakeChannel extends TestCase with TestDatabaseConfiguration {

  def ChannelsTable(implicit app: Application): Channels = injectDependency[Channels]
  def ChannelTokensTable(implicit app: Application): ChannelTokens = injectDependency[ChannelTokens]
  def ClientLoginRequestsTable(implicit app: Application): ClientLoginRequests = injectDependency[ClientLoginRequests]
  def AuthenticatedClientsTable(implicit app: Application): AuthenticatedClients = injectDependency[AuthenticatedClients]


  //  creating fake data for testing
  val dummyChannelId = TestRandom.string(5)
  val hostId = TestRandom.string(5)

  val channels = new ChannelFactory {
    override def channelId: String = dummyChannelId
    override def hostIdHash = Hash.sha2(hostId)
  }.create(1)

  val hostChannelToken = TestRandom.string(5)
  val secretKey = TestRandom.string(5)
  val channelName = TestRandom.string(5)
  val loginChannelToken = TestRandom.string(5)
  val clientChannelToken = TestRandom.string(5)

  val channelTokens = new ChannelTokenFactory {
    override def channelId = dummyChannelId
    override def channelNameEnc: Array[Byte] = Crypter.encrypt(channelName, channelId)
    override def hostChannelTokenHash: String = Hash.sha2(hostChannelToken)
    override def loginChannelTokenHash: String = Hash.sha2(loginChannelToken)
    override def loginChannelTokenEnc: Array[Byte] = Crypter.encrypt(loginChannelToken, channelId)
    override def clientChannelTokenHash = Hash.sha2(clientChannelToken)
    override def clientChannelTokenEnc: Array[Byte] = Crypter.encrypt(clientChannelToken, channelId)
    override def secretKeyEnc: Option[Array[Byte]] = Some(Crypter.encrypt(secretKey, channelId))
  }.create(1)


  // --- another channel for testing credential ---
  val anotherChannelId = TestRandom.string(5)
  val anotherHostId = TestRandom.string(5)

  val anotherChannels = new ChannelFactory {
    override def channelId: String = anotherChannelId
    override def hostIdHash = Hash.sha2(hostId)
  }.create(1)

  val anotherHostChannelToken = TestRandom.string(5)
  val anotherSecretKey = TestRandom.string(5)
  val anotherChannelName = TestRandom.string(5)
  val anotherLoginChannelToken = TestRandom.string(5)
  val anotherClientChannelToken = TestRandom.string(5)

  val anotherChannelTokens = new ChannelTokenFactory {
    override def channelId = anotherChannelId
    override def channelNameEnc: Array[Byte] = Crypter.encrypt(anotherChannelName, channelId)
    override def hostChannelTokenHash: String = Hash.sha2(anotherHostChannelToken)
    override def loginChannelTokenHash: String = Hash.sha2(anotherLoginChannelToken)
    override def loginChannelTokenEnc: Array[Byte] = Crypter.encrypt(anotherLoginChannelToken, channelId)
    override def clientChannelTokenHash = Hash.sha2(anotherClientChannelToken)
    override def clientChannelTokenEnc: Array[Byte] = Crypter.encrypt(anotherClientChannelToken, channelId)
    override def secretKeyEnc: Option[Array[Byte]] = Some(Crypter.encrypt(anotherSecretKey, channelId))
  }.create(1)


  val requestClientId = TestRandom.string(5)
  val codename = TestRandom.string(5)
  val passphrase = TestRandom.string(5)
  val clientLoginRequests = new ClientLoginRequestFactory {
    override def channelId = dummyChannelId
    override def requestClientIdHash = Hash.sha2(requestClientId)
    override def requestClientIdEnc = Crypter.encrypt(requestClientId, channelId)
    override def codenameEnc = Some(Crypter.encrypt(codename, channelId))
    override def passphraseEnc = Some(Crypter.encrypt(passphrase, channelId))
    override def isAuthenticated = None
  }.create(1)

  val authenticatedClientId = TestRandom.string(5)
  val authenticatedClients = new AuthenticatedClientFactory {
    override def authenticatedClientIdHash: String = Hash.sha2(authenticatedClientId)
    override def authenticatedClientIdEnc = Crypter.encrypt(authenticatedClientId, channelId)
    override def requestClientIdHash: String = Hash.sha2(requestClientId)
    override def channelId: String = dummyChannelId
  }.create(1)

  override def afterEach() = {
    Await.result(ChannelsTable.truncate(), 2.seconds)
    Await.result(ChannelTokensTable.truncate(), 2.seconds)
    Await.result(ClientLoginRequestsTable.truncate(), 2.seconds)
    Await.result(AuthenticatedClientsTable.truncate(), 2.seconds)
  }

}
