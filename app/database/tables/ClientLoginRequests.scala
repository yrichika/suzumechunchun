package database.tables

import java.sql.{Blob, Timestamp}
import helpers.slicktable.CommonQueries

import javax.inject.Inject
import database.models.{ClientLoginRequest, CreatedRequest}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import forms.{ClientStatus, LoginRequest}
import com.mohiva.play.silhouette.api.crypto.Hash
import helpers.crypto.Random
import helpers.crypto.Crypter

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


class ClientLoginRequests @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]
  with CommonQueries
{
  import profile.api._

  type Model = ClientLoginRequest
  type ModelsTable = ClientLoginRequestsTable
  lazy val query = TableQuery[ModelsTable]
  protected val tableName = "client_login_requests"

  def getByChannelId(channelId: String): Future[Seq[ClientLoginRequest]] = {
    val statement = query.filter(_.channelId === channelId).result
    db.run(statement)
  }

  def getByRequestClientId(requestClientId: String): Future[Option[ClientLoginRequest]] = {
    val hashedRequestClientId = Hash.sha2(requestClientId)
    val statement = query.filter(_.requestClientIdHash === hashedRequestClientId).result.headOption
    db.run(statement)
  }

  def create(loginRequest: LoginRequest, channelId: String): Future[CreatedRequest] = {
    val clientLoginRequestId = UUID.randomUUID().toString()
    val requestClientId = UUID.randomUUID().toString()
    val requestClientIdHash = Hash.sha2(requestClientId)
    val requestClientIdEnc = Crypter.encrypt(requestClientId, channelId.toString)
    val codenameEnc = Crypter.encrypt(loginRequest.codename, channelId.toString)
    val passphraseEnc = Crypter.encrypt(loginRequest.passphrase, channelId.toString)
    val isAuthenticated = None
    val current = new Timestamp(System.currentTimeMillis())
    val statement = query returning query.map(_.id) += ClientLoginRequest(0, clientLoginRequestId, requestClientIdHash, requestClientIdEnc, channelId, Some(codenameEnc), Some(passphraseEnc), isAuthenticated, current, current)
    val resultFut = db.run(statement)
    resultFut.map(_ => CreatedRequest(requestClientId, loginRequest.codename, loginRequest.passphrase, isAuthenticated))
  }


  // FIXME: Auto update `updatedAt` column will be implemented
  def updateStatus(clientStatus: ClientStatus): Future[String] = {
    val requestClientId = clientStatus.requestClientId
    val status = clientStatus.status
    val hashedRequestClientId = Hash.sha2(requestClientId)
    val statement = query.filter(_.requestClientIdHash === hashedRequestClientId)
      .map(request => (request.isAuthenticated, request.updatedAt))
      .update((Some(status), new Timestamp(System.currentTimeMillis())))
    val result = db.run(statement)
    result.map(_ => hashedRequestClientId)
  }

  def deleteByChannelIds(channelIds: Seq[String]) = {
    val statement = query.filter(_.channelId.inSetBind(channelIds)).delete
    db.run(statement)
  }


  final protected class ClientLoginRequestsTable(tag: Tag) extends Table[ClientLoginRequest](tag, tableName) {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def clientLoginRequestId = column[String]("client_login_request_id")
    def requestClientIdHash = column[String]("request_client_id_hash")
    def requestClientIdEnc = column[Array[Byte]]("request_client_id_enc")
    def channelId = column[String]("channel_id")
    def codenameEnc = column[Option[Array[Byte]]]("codename_enc")
    def passphraseEnc = column[Option[Array[Byte]]]("passphrase_enc")
    def isAuthenticated = column[Option[Boolean]]("is_authenticated")
    def createdAt = column[Timestamp]("created_at")
    def updatedAt =column[Timestamp]("updated_at")

    def * = (id, clientLoginRequestId, requestClientIdHash, requestClientIdEnc, channelId, codenameEnc, passphraseEnc, isAuthenticated, createdAt, updatedAt) <> (ClientLoginRequest.tupled, ClientLoginRequest.unapply)

  }
}
