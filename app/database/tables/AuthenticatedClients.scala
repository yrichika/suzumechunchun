package database.tables

import java.sql.Timestamp
import helpers.slicktable.CommonQueries

import javax.inject.Inject
import database.models.AuthenticatedClient
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import helpers.crypto.Random
import com.mohiva.play.silhouette.api.crypto.Hash

import scala.concurrent.{ExecutionContext, Future}
import helpers.crypto.Crypter

import java.util.UUID
import scala.concurrent.ExecutionContext


class AuthenticatedClients @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]
  with CommonQueries
{
  import profile.api._

  type Model = AuthenticatedClient
  type ModelsTable = AuthenticatedClientsTable
  lazy val query = TableQuery[ModelsTable]
  protected val tableName = "authenticated_clients"

  def create(channelId: String, requestClientIdHash: String) = {
    val authenticatedClientId = UUID.randomUUID().toString()
    val authenticatedClientIdHash = Hash.sha2(authenticatedClientId)
    val authenticatedClientIdEnc = Crypter.encrypt(authenticatedClientId, channelId.toString)
    val current = new Timestamp(System.currentTimeMillis())
    val statement = query returning query.map(_.id) += AuthenticatedClient(0, authenticatedClientIdHash, authenticatedClientIdEnc, requestClientIdHash, channelId, current, current)
    db.run(statement)
  }

  def getByAuthenticatedClientId(authenticatedClientId: String): Future[Option[AuthenticatedClient]] = {
    val authenticatedClientIdHash = Hash.sha2(authenticatedClientId)
    val statement = query.filter(_.authenticatedClientIdHash === authenticatedClientIdHash).result.headOption
    db.run(statement)
  }

  def getByRequestClientId(requestClientId: String): Future[Option[AuthenticatedClient]] = {
    val requestClientIdHash = Hash.sha2(requestClientId)
    val statement = query.filter(_.requestClientIdHash === requestClientIdHash).result.headOption
    db.run(statement)
  }

  def deleteByChannelIds(channelIds: Seq[String]) = {
    val statement = query.filter(_.channelId.inSetBind(channelIds)).delete
    db.run(statement)
  }

  final protected class AuthenticatedClientsTable(tag: Tag) extends Table[AuthenticatedClient](tag, tableName) {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def authenticatedClientIdHash = column[String]("authenticated_client_id_hash")
    def authenticatedClientIdEnc = column[Array[Byte]]("authenticated_client_id_enc")
    def requestClientIdHash = column[String]("request_client_id_hash")
    def channelId = column[String]("channel_id")
    def createdAt = column[Timestamp]("created_at")
    def updatedAt = column[Timestamp]("updated_at")

    def * = (id, authenticatedClientIdHash, authenticatedClientIdEnc, requestClientIdHash, channelId, createdAt, updatedAt) <> (AuthenticatedClient.tupled, AuthenticatedClient.unapply)

  }
}
